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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvBalance, tvIncome, tvExpense;
    private RecyclerView rvRecentTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        tvBalance = view.findViewById(R.id.tvBalance);
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
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    transactionList.clear();
                    double totalIncome = 0;
                    double totalExpense = 0;

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Transaction transaction = doc.toObject(Transaction.class);
                            transaction.setId(doc.getId());
                            transactionList.add(transaction);

                            if ("Income".equals(transaction.getType())) {
                                totalIncome += transaction.getAmount();
                            } else if ("Expense".equals(transaction.getType())) {
                                totalExpense += transaction.getAmount();
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();

                    // Update balance
                    double balance = totalIncome - totalExpense;
                    tvBalance.setText(String.format("$%.2f", balance));
                    tvIncome.setText(String.format("$%.2f", totalIncome));
                    tvExpense.setText(String.format("$%.2f", totalExpense));
                });
    }
}
