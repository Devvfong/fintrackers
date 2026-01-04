package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

/**
 * BudgetFragment with Month Navigation
 *
 * Features:
 * 1. Month selector (< May >)
 * 2. Automatic spending calculation from transactions
 * 3. Status-based color coding (Safe/Warning/Critical/Over)
 * 4. Real-time updates via Firestore listeners
 */
public class BudgetFragment extends Fragment {

    private static final String TAG = "BudgetFragment";

    private RecyclerView rvBudgets;
    private LinearLayout layoutEmpty;
    private Button btnCreate;
    private TextView tvMonth;
    private TextView btnPrevMonth, btnNextMonth;

    private BudgetAdapter adapter;
    private List<Budget> budgetList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Calendar currentMonth;
    private boolean isCalculating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize current month
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

    /**
     * Update month display text
     */
    private void updateMonthDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        tvMonth.setText(monthFormat.format(currentMonth.getTime()));
    }

    /**
     * Load budgets for selected month
     */
    private void loadBudgetsForMonth() {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "No user logged in");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Get month timestamp (first day of month at 00:00:00)
        Calendar monthStart = (Calendar) currentMonth.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);
        long monthTimestamp = monthStart.getTimeInMillis();

        Log.d(TAG, "Loading budgets for month: " + monthTimestamp);

        // Load budgets for this month
        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("monthTimestamp", monthTimestamp)
                .addSnapshotListener((budgetSnapshots, budgetError) -> {
                    if (budgetError != null) {
                        Log.e(TAG, "Error loading budgets", budgetError);
                        Toast.makeText(requireContext(), "Error loading budgets",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    budgetList.clear();
                    if (budgetSnapshots != null && !budgetSnapshots.isEmpty()) {
                        Log.d(TAG, "Found " + budgetSnapshots.size() + " budgets");
                        for (QueryDocumentSnapshot doc : budgetSnapshots) {
                            Budget budget = doc.toObject(Budget.class);
                            budget.setId(doc.getId());
                            budgetList.add(budget);
                        }

                        // Calculate spending
                        if (!isCalculating) {
                            calculateSpendingForBudgets(userId);
                        }
                    } else {
                        Log.d(TAG, "No budgets found for this month");
                        adapter.notifyDataSetChanged();
                        toggleEmptyState();
                    }
                });
    }

    /**
     * Calculate spending from transactions
     */
    private void calculateSpendingForBudgets(String userId) {
        if (budgetList.isEmpty()) {
            Log.d(TAG, "No budgets to calculate spending for");
            return;
        }

        isCalculating = true;
        Log.d(TAG, "Calculating spending for " + budgetList.size() + " budgets");

        // Load all expense transactions
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "Expense")  // ← Capital E!
                .get()
                .addOnSuccessListener(transactionSnapshots -> {
                    Log.d(TAG, "Loaded " + transactionSnapshots.size() + " expense transactions");

                    // Initialize spending map
                    Map<String, Double> spendingMap = new HashMap<>();
                    for (Budget budget : budgetList) {
                        spendingMap.put(budget.getId(), 0.0);
                    }
                    Log.d(TAG, "Querying transactions for userId: " + userId);
                    // Calculate spending
                    int matchedTransactions = 0;
                    if (transactionSnapshots != null && !transactionSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot transDoc : transactionSnapshots) {
                            String category = transDoc.getString("category");
                            Double amount = transDoc.getDouble("amount");
                            Long timestamp = transDoc.getLong("timestamp");

                            if (category != null && amount != null && timestamp != null) {
                                for (Budget budget : budgetList) {
                                    if (doesTransactionMatchBudget(budget, category, timestamp)) {
                                        String budgetId = budget.getId();
                                        double currentSpending = spendingMap.getOrDefault(budgetId, 0.0);
                                        spendingMap.put(budgetId, currentSpending + amount);
                                        matchedTransactions++;
                                    }
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Matched " + matchedTransactions + " transactions to budgets");

                    // Update budgets with calculated spending
                    for (Budget budget : budgetList) {
                        double spent = spendingMap.getOrDefault(budget.getId(), 0.0);
                        budget.setSpent(spent);
                        Log.d(TAG, budget.getCategoryName() + ": $" + spent + " / $" + budget.getAmount());
                    }

                    // Refresh UI
                    adapter.notifyDataSetChanged();
                    toggleEmptyState();
                    isCalculating = false;

                    Log.d(TAG, "Spending calculation completed");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error calculating spending", e);
                    Toast.makeText(requireContext(),
                            "Error calculating spending: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    adapter.notifyDataSetChanged();
                    toggleEmptyState();
                    isCalculating = false;
                });
    }

    /**
     * Check if transaction matches budget
     */
    private boolean doesTransactionMatchBudget(Budget budget, String transactionCategory, long transactionTimestamp) {
        // Match 1: Category
        boolean categoryMatches = transactionCategory.equalsIgnoreCase(budget.getCategoryName()) ||
                transactionCategory.equalsIgnoreCase(budget.getCategoryId());

        // DEBUG LOGS
        Log.d(TAG, "Checking match:");
        Log.d(TAG, "  Transaction category: '" + transactionCategory + "'");
        Log.d(TAG, "  Budget category: '" + budget.getCategoryName() + "'");
        Log.d(TAG, "  Category matches: " + categoryMatches);

        if (!categoryMatches) {
            return false;
        }

        // Match 2: Month and year
        Calendar budgetCal = Calendar.getInstance();
        budgetCal.setTimeInMillis(budget.getMonthTimestamp());
        int budgetMonth = budgetCal.get(Calendar.MONTH);
        int budgetYear = budgetCal.get(Calendar.YEAR);

        Calendar transCal = Calendar.getInstance();
        transCal.setTimeInMillis(transactionTimestamp);
        int transMonth = transCal.get(Calendar.MONTH);
        int transYear = transCal.get(Calendar.YEAR);

        boolean monthMatches = budgetMonth == transMonth && budgetYear == transYear;

        // DEBUG LOGS
        Log.d(TAG, "  Budget: " + budgetMonth + "/" + budgetYear);
        Log.d(TAG, "  Transaction: " + transMonth + "/" + transYear);
        Log.d(TAG, "  Month matches: " + monthMatches);
        Log.d(TAG, "  FINAL MATCH: " + monthMatches);

        return monthMatches;
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
        Log.d(TAG, "onResume");

        if (!isCalculating) {
            loadBudgetsForMonth();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }
}