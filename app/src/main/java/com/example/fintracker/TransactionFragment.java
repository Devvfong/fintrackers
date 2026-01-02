package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public class TransactionFragment extends Fragment {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private final List<Transaction> transactionList = new ArrayList<>();
    private final List<Transaction> allTransactions = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d("TransactionFragment", "🔥 onViewCreated START");
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setNestedScrollingEnabled(false);

        adapter = new TransactionAdapter(requireContext(), transactionList, new TransactionAdapter.Listener() {
            @Override public void onEditClick(@NonNull Transaction t) { openEdit(t); }
            @Override public void onRowClick(@NonNull Transaction t) {
                Intent i = new Intent(requireContext(), TransactionDetailActivity.class);
                i.putExtra("transactionId", t.getId());
                startActivity(i);
            }
        });
        recyclerView.setAdapter(adapter);

        // FAB Add button
        FloatingActionButton fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
        fabAddTransaction.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddTransactionActivity.class)));

        attachSwipeDelete();
        loadTransactions();
        Log.d("TransactionFragment", "🎉 onViewCreated COMPLETE");
    }

    private void loadTransactions() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "NO_USER";
        Log.d("TransactionFragment", "🔥 loadTransactions START - user: " + userId);

        if (mAuth.getCurrentUser() == null) {
            Log.w("TransactionFragment", "❌ NO USER LOGGED IN - skipping");
            return;
        }

        db.collection("transactions")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("TransactionFragment", "❌ FIRESTORE ERROR: " + error.getMessage(), error);
                        return;
                    }

                    int docCount = snapshots != null ? snapshots.size() : 0;
                    Log.d("TransactionFragment", "📊 QUERY RESULT: " + docCount + " documents");

                    List<Transaction> newList = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Transaction t = doc.toObject(Transaction.class);
                                t.setId(doc.getId());
                                newList.add(t);
                                Log.d("TransactionFragment", "✅ LOADED: " + t.getType() + " $" + t.getAmount());
                            } catch (Exception e) {
                                Log.e("TransactionFragment", "❌ PARSE ERROR doc: " + doc.getId(), e);
                            }
                        }
                    }

                    allTransactions.clear();
                    allTransactions.addAll(newList);
                    transactionList.clear();
                    transactionList.addAll(newList);
                    adapter.updateTransactions(newList);
                    Log.d("TransactionFragment", "🎉 ADAPTER UPDATED: " + newList.size() + " transactions");
                });
    }

    private void attachSwipeDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder v, int d) {
                int pos = v.getAdapterPosition();
                if (pos < 0 || pos >= transactionList.size()) return;

                Transaction t = transactionList.get(pos);
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete?")
                        .setMessage("Delete " + t.getCategory() + "?")
                        .setPositiveButton("Yes", (dd, ww) -> {
                            if (t.getId() != null) {
                                db.collection("transactions").document(t.getId()).delete();
                            }
                            transactionList.remove(pos);
                            adapter.notifyItemRemoved(pos);
                        })
                        .setNegativeButton("No", (dd, ww) -> adapter.notifyItemChanged(pos))
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void openEdit(Transaction t) {
        Intent i = new Intent(requireContext(), AddTransactionActivity.class);
        i.putExtra("editMode", true);
        i.putExtra("transactionId", t.getId());
        i.putExtra("amount", String.valueOf(t.getAmount()));
        i.putExtra("category", t.getCategory());
        i.putExtra("wallet", t.getWallet());
        i.putExtra("description", t.getDescription() != null ? t.getDescription() : "");
        i.putExtra("timestamp", t.getTimestamp());
        i.putExtra("attachmentUrl", t.getAttachmentUrl());
        startActivity(i);
    }
}
