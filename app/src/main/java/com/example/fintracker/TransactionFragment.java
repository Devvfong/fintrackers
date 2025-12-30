package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransactionFragment extends Fragment {

    private static final String TAG = "TransactionFragment";

    private RecyclerView rvTransactions;
    private FloatingActionButton fabAddTransaction;
    private TextView tvNoTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        rvTransactions = view.findViewById(R.id.rvTransactions);
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
        tvNoTransactions = view.findViewById(R.id.tvNoTransactions);

        // Setup RecyclerView
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(getContext(), transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(adapter);

        // FAB click
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
            startActivity(intent);
        });

        // Load transactions
        loadTransactions();

        return view;
    }

    private void loadTransactions() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in");
            showNoTransactions();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Load without orderBy to avoid index requirement
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading transactions", error);
                        showNoTransactions();
                        return;
                    }

                    transactionList.clear();

                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Transaction transaction = doc.toObject(Transaction.class);
                                transaction.setId(doc.getId());
                                transactionList.add(transaction);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing transaction", e);
                            }
                        }

                        // Sort by timestamp in descending order (newest first)
                        Collections.sort(transactionList, new Comparator<Transaction>() {
                            @Override
                            public int compare(Transaction t1, Transaction t2) {
                                return Long.compare(t2.getTimestamp(), t1.getTimestamp());
                            }
                        });

                        // Show RecyclerView, hide empty message
                        rvTransactions.setVisibility(View.VISIBLE);
                        tvNoTransactions.setVisibility(View.GONE);
                    } else {
                        // Show empty message
                        showNoTransactions();
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void showNoTransactions() {
        if (tvNoTransactions != null) {
            tvNoTransactions.setVisibility(View.VISIBLE);
        }
        if (rvTransactions != null) {
            rvTransactions.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadTransactions();
        }
    }
}