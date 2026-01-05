package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetFragment extends Fragment {

    private static final String TAG = "BudgetFragment";

    private RecyclerView rvBudgets;
    private LinearLayout layoutEmpty;
    private Button btnCreate;
    private TextView tvMonth;
    private ImageView btnPrevMonth, btnNextMonth;

    private BudgetAdapter adapter;
    private final List<Budget> budgetList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Calendar currentMonth;
    private boolean isCalculating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        currentMonth = Calendar.getInstance();

        initViews(view);
        setupRecyclerView();
        setupListeners();

        updateMonthDisplay();
        loadBudgetsForMonth();

        return view;
    }

    private void initViews(View view) {
        rvBudgets = view.findViewById(R.id.rvBudgets);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        btnCreate = view.findViewById(R.id.btnCreate);
        tvMonth = view.findViewById(R.id.tvMonth);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter(requireContext(), budgetList, new BudgetAdapter.Listener() {

            @Override
            public void onRowClick(Budget budget) {
                Intent i = new Intent(requireContext(), DetailBudgetActivity.class);
                i.putExtra("budgetId", budget.getId());
                startActivity(i);
            }

            @Override
            public void onEditClick(Budget budget) {
                Intent intent = new Intent(requireContext(), CreateBudgetActivity.class);
                intent.putExtra("budgetId", budget.getId());
                startActivity(intent);
            }

            @Override
            public void onCheckboxChanged() {
                // Not used in this version
            }
        });

        rvBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBudgets.setAdapter(adapter);
    }

    private void setupListeners() {
        btnCreate.setOnClickListener(v -> createNewBudget());

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadBudgetsForMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadBudgetsForMonth();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        tvMonth.setText(monthFormat.format(currentMonth.getTime()));
    }

    private void loadBudgetsForMonth() {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "No user logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        Calendar monthStart = (Calendar) currentMonth.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);

        long monthTimestamp = monthStart.getTimeInMillis();
        Log.d(TAG, "Loading budgets for month: " + monthTimestamp);

        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("monthTimestamp", monthTimestamp)
                .addSnapshotListener((budgetSnapshots, budgetError) -> {
                    if (budgetError != null) {
                        Log.e(TAG, "Error loading budgets", budgetError);
                        Toast.makeText(requireContext(), "Error loading budgets", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    budgetList.clear();

                    if (budgetSnapshots != null && !budgetSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : budgetSnapshots) {
                            Budget budget = doc.toObject(Budget.class);
                            budget.setId(doc.getId());
                            budgetList.add(budget);
                        }

                        if (!isCalculating) {
                            calculateSpendingForBudgets(userId);
                        }
                    } else {
                        adapter.notifyDataSetChanged();
                        toggleEmptyState();
                    }
                });
    }

    private void calculateSpendingForBudgets(String userId) {
        if (budgetList.isEmpty()) {
            return;
        }

        isCalculating = true;

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "Expense")
                .get()
                .addOnSuccessListener(transactionSnapshots -> {

                    Map<String, Double> spendingMap = new HashMap<>();
                    for (Budget budget : budgetList) {
                        spendingMap.put(budget.getId(), 0.0);
                    }

                    if (transactionSnapshots != null && !transactionSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot transDoc : transactionSnapshots) {
                            String category = transDoc.getString("category");
                            Double amount = transDoc.getDouble("amount");
                            Long timestamp = transDoc.getLong("timestamp");

                            if (category == null || amount == null || timestamp == null) continue;

                            for (Budget budget : budgetList) {
                                if (doesTransactionMatchBudget(budget, category, timestamp)) {
                                    String budgetId = budget.getId();
                                    double currentSpending = spendingMap.getOrDefault(budgetId, 0.0);
                                    spendingMap.put(budgetId, currentSpending + amount);
                                }
                            }
                        }
                    }

                    for (Budget budget : budgetList) {
                        double spent = spendingMap.getOrDefault(budget.getId(), 0.0);
                        budget.setSpent(spent);
                    }

                    adapter.notifyDataSetChanged();
                    toggleEmptyState();
                    isCalculating = false;

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error calculating spending", e);
                    Toast.makeText(requireContext(), "Error calculating spending: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    toggleEmptyState();
                    isCalculating = false;
                });
    }

    private boolean doesTransactionMatchBudget(Budget budget, String transactionCategory, long transactionTimestamp) {
        boolean categoryMatches =
                transactionCategory.equalsIgnoreCase(budget.getCategoryName()) ||
                        transactionCategory.equalsIgnoreCase(budget.getCategoryId());

        if (!categoryMatches) return false;

        Calendar budgetCal = Calendar.getInstance();
        budgetCal.setTimeInMillis(budget.getMonthTimestamp());

        Calendar transCal = Calendar.getInstance();
        transCal.setTimeInMillis(transactionTimestamp);

        int budgetMonth = budgetCal.get(Calendar.MONTH);
        int budgetYear = budgetCal.get(Calendar.YEAR);

        int transMonth = transCal.get(Calendar.MONTH);
        int transYear = transCal.get(Calendar.YEAR);

        return budgetMonth == transMonth && budgetYear == transYear;
    }

    private void toggleEmptyState() {
        if (budgetList.isEmpty()) {
            rvBudgets.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvBudgets.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void createNewBudget() {
        Intent intent = new Intent(requireContext(), CreateBudgetActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isCalculating) {
            loadBudgetsForMonth();
        }
    }
}
