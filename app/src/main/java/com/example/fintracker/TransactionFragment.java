package com.example.fintracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TransactionFragment extends Fragment {

    private TransactionAdapter adapter;
    private final List<Transaction> transactionList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);

        adapter = new TransactionAdapter(getContext(), transactionList);
        recyclerView.setAdapter(adapter);
        adapter = new TransactionAdapter(getContext(), transactionList, transaction -> {
            android.content.Intent i = new android.content.Intent(requireContext(), AddTransactionActivity.class);
            i.putExtra("editMode", true);
            i.putExtra("transactionId", transaction.getId());
            i.putExtra("amount", String.valueOf(transaction.getAmount()));
            i.putExtra("category", transaction.getCategory());
            i.putExtra("wallet", transaction.getWallet());
            i.putExtra("description", transaction.getDescription());
            i.putExtra("timestamp", transaction.getTimestamp());
            i.putExtra("attachmentUrl", transaction.getAttachmentUrl());
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        loadTransactions();

        // SWIPE TO DELETE
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                showDeleteConfirmation(position);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmation(int position) {
        Transaction transaction = transactionList.get(position);

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete \"" + transaction.getCategory() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTransaction(transaction, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTransaction(Transaction transaction, int position) {
        db.collection("transactions")
                .document(transaction.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    transactionList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadTransactions() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("transactions")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    transactionList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction t = document.toObject(Transaction.class);
                            t.setId(document.getId());
                            transactionList.add(t);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
