package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionFragment extends Fragment {

    private RecyclerView rvTransactions;
    private FloatingActionButton fabAddTransaction;

    private TransactionAdapter adapter;
    private final List<Transaction> list = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTransactions = view.findViewById(R.id.rvTransactions);
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(requireContext(), list);
        rvTransactions.setAdapter(adapter);

        fabAddTransaction.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddTransactionActivity.class));
        });

        loadTransactions();
    }

    private void loadTransactions() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // No orderBy here => no composite index needed
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    list.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t != null) list.add(t);
                    }

                    // Sort in Java by timestamp DESC (matches your saved field)
                    Collections.sort(list, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when coming back from AddTransactionActivity
        loadTransactions();
    }
}
