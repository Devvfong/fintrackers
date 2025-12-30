package com.example.fintracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvAccountBalance, tvIncome, tvExpense;
    private RecyclerView rvRecentTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private double totalIncome = 0;
    private double totalExpense = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        tvAccountBalance = view.findViewById(R.id.tvAccountBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);

        // Setup RecyclerView
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(getContext(), transactionList);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentTransactions.setAdapter(adapter);

        // Load data
        loadTransactions();

        return view;
    }

    private void loadTransactions() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {   // CHANGED: from .get() to .addSnapshotListener()
                    if (error != null) {
                        return;
                    }

                    transactionList.clear();
                    totalIncome = 0;
                    totalExpense = 0;

                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactionList.add(transaction);

                            // Calculate totals
                            if ("Income".equals(transaction.getType())) {
                                totalIncome += transaction.getAmount();
                            } else {
                                totalExpense += transaction.getAmount();
                            }
                        }
                    }

                    // Update UI
                    updateBalanceUI();
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateBalanceUI() {
        double balance = totalIncome - totalExpense;

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

        tvAccountBalance.setText(formatter.format(balance));
        tvIncome.setText(formatter.format(totalIncome));
        tvExpense.setText(formatter.format(totalExpense));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment becomes visible
        loadTransactions();
    }
}
