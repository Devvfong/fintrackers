package com.example.fintracker;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private LinearLayout btnMonthSelector;
    private MaterialCardView btnFinancialReport;
    private TextView tvMonthYear;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar selectedMonth;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        selectedMonth = Calendar.getInstance();

        // Initialize views
        rvTransactions = view.findViewById(R.id.rvTransactions);
        btnMonthSelector = view.findViewById(R.id.btnMonthSelector);
        btnFinancialReport = view.findViewById(R.id.btnFinancialReport);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);

        // Setup RecyclerView
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(getContext(), transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(adapter);

        setupListeners();
        updateMonthDisplay();
        loadTransactions();

        return view;
    }

    private void setupListeners() {
        // Month selector
        btnMonthSelector.setOnClickListener(v -> showMonthPicker());

        // Financial report
        btnFinancialReport.setOnClickListener(v -> {
            // TODO: Navigate to financial report
        });
    }

    private void showMonthPicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedMonth.set(Calendar.YEAR, year);
                    selectedMonth.set(Calendar.MONTH, month);
                    updateMonthDisplay();
                    loadTransactions();
                },
                selectedMonth.get(Calendar.YEAR),
                selectedMonth.get(Calendar.MONTH),
                1
        );
        datePickerDialog.show();
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM", Locale.getDefault());
        tvMonthYear.setText(sdf.format(selectedMonth.getTime()));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadTransactions() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        Calendar startCal = (Calendar) selectedMonth.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        long startTime = startCal.getTimeInMillis();

        Calendar endCal = (Calendar) selectedMonth.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        long endTime = endCal.getTimeInMillis();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThanOrEqualTo("timestamp", endTime)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    transactionList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactionList.add(transaction);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadTransactions();
        }
    }
}
