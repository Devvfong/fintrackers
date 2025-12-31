package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TransactionFragment extends Fragment {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private final List<Transaction> transactionList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setNestedScrollingEnabled(false);

        adapter = new TransactionAdapter(requireContext(), transactionList, new TransactionAdapter.Listener() {
            @Override
            public void onEditClick(@NonNull Transaction transaction) {
                openEditTransaction(transaction);
            }

            @Override
            public void onRowClick(@NonNull Transaction transaction) {
                // Optional: open details
            }
        });

        recyclerView.setAdapter(adapter);

        attachSwipeToDelete();
        loadTransactions();
    }

    private void openEditTransaction(@NonNull Transaction transaction) {
        Intent i = new Intent(requireContext(), AddTransactionActivity.class);
        i.putExtra("editMode", true);
        i.putExtra("transactionId", transaction.getId());
        i.putExtra("amount", String.valueOf(transaction.getAmount()));
        i.putExtra("category", transaction.getCategory());
        i.putExtra("wallet", transaction.getWallet());
        i.putExtra("description", transaction.getDescription() != null ? transaction.getDescription() : "");
        i.putExtra("timestamp", transaction.getTimestamp());
        i.putExtra("attachmentUrl", transaction.getAttachmentUrl());
        startActivity(i);
    }

    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
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
        if (position < 0 || position >= transactionList.size()) return;

        Transaction transaction = transactionList.get(position);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.remove) + " " + transaction.getCategory() + "?")
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTransaction(transaction, position))
                .setNegativeButton(R.string.cancel, (dialog, which) -> adapter.notifyItemChanged(position))
                .setCancelable(false)
                .show();
    }

    private void deleteTransaction(@NonNull Transaction transaction, int position) {
        String id = transaction.getId();
        if (id == null || id.trim().isEmpty()) {
            adapter.notifyItemChanged(position);
            return;
        }

        db.collection("transactions")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    adapter.removeItem(position);
                    Toast.makeText(requireContext(), R.string.remove, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    adapter.notifyItemChanged(position);
                    Toast.makeText(requireContext(), R.string.todo, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTransactions() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("transactions")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    List<Transaction> newList = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction t = document.toObject(Transaction.class);
                            t.setId(document.getId());
                            newList.add(t);
                        }
                    }

                    adapter.updateTransactions(newList);
                });
    }
}
