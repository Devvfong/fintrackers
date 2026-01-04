package com.example.fintracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("ALL")
public class TransactionFragment extends Fragment {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private final List<Transaction> transactionList = new ArrayList<>();
    private final List<Transaction> allTransactions = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Filter settings
    private String filterType = "All";
    private String filterCategory = "All";
    private String sortBy = "Newest First";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
            public void onEditClick(@NonNull Transaction t) {
                openEdit(t);
            }

            @Override
            public void onRowClick(@NonNull Transaction t) {
                Intent i = new Intent(requireContext(), TransactionDetailActivity.class);
                i.putExtra("transactionId", t.getId());
                startActivity(i);
            }
        });
        recyclerView.setAdapter(adapter);

        // FAB Add button
        FloatingActionButton fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
        fabAddTransaction.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddTransactionActivity.class)));

        // Filter button
        View btnFilter = view.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        attachSwipeDelete();
        loadTransactions();
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        @SuppressLint("InflateParams") View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        // Type chips
        ChipGroup chipGroupType = dialogView.findViewById(R.id.chipGroupType);
        chipGroupType.setSingleSelection(true);

        // Category chips
        ChipGroup chipGroupCategory = dialogView.findViewById(R.id.chipGroupCategory);
        chipGroupCategory.setSingleSelection(true);

        // Sort chips
        ChipGroup chipGroupSort = dialogView.findViewById(R.id.chipGroupSort);
        chipGroupSort.setSingleSelection(true);

        // Set current selections
        setChipChecked(chipGroupType, filterType);
        setChipChecked(chipGroupCategory, filterCategory);
        setChipChecked(chipGroupSort, sortBy);

        // Apply button
        Button btnApply = dialogView.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(v -> {
            // Get selected type
            filterType = getSelectedChipText(chipGroupType, "All");
            filterCategory = getSelectedChipText(chipGroupCategory, "All");
            sortBy = getSelectedChipText(chipGroupSort, "Newest First");

            Log.d("Filter", "Type: " + filterType + ", Category: " + filterCategory + ", Sort: " + sortBy);

            applyFilter();
            Toast.makeText(requireContext(), "Filter applied", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Reset button
        Button btnReset = dialogView.findViewById(R.id.btnReset);
        btnReset.setOnClickListener(v -> {
            filterType = "All";
            filterCategory = "All";
            sortBy = "Newest First";
            applyFilter();
            Toast.makeText(requireContext(), "Filter reset", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setContentView(dialogView);
        dialog.show();
    }

    private void setChipChecked(ChipGroup chipGroup, String value) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            String chipText = chip.getText().toString();
            if (chipText.equals(value)) {
                chip.setChecked(true);
                return;
            }
        }
    }

    private String getSelectedChipText(ChipGroup chipGroup, String defaultValue) {
        int selectedId = chipGroup.getCheckedChipId();
        if (selectedId == -1) return defaultValue;

        Chip selectedChip = chipGroup.findViewById(selectedId);
        return selectedChip != null ? selectedChip.getText().toString() : defaultValue;
    }

    private void applyFilter() {
        transactionList.clear();

        Log.d("Filter", "All transactions: " + allTransactions.size());

        // Filter by type and category
        for (Transaction t : allTransactions) {
            boolean typeMatch = filterType.equals("All") || filterType.equals(t.getType());
            boolean categoryMatch = filterCategory.equals("All") || filterCategory.equals(t.getCategory());

            if (typeMatch && categoryMatch) {
                transactionList.add(t);
            }
        }

        Log.d("Filter", "After filter: " + transactionList.size());

        // Sort
        switch (sortBy) {
            case "Newest First":
                transactionList.sort((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
                break;
            case "Oldest First":
                transactionList.sort((t1, t2) -> Long.compare(t1.getTimestamp(), t2.getTimestamp()));
                break;
            case "Highest Amount":
                Collections.sort(transactionList, (t1, t2) -> Double.compare(t2.getAmount(), t1.getAmount()));
                break;
            case "Lowest Amount":
                transactionList.sort(Comparator.comparingDouble(Transaction::getAmount));
                break;
        }

        adapter.updateTransactions(transactionList);
        Log.d("Filter", "Final list: " + transactionList.size() + " transactions");
    }

    private void loadTransactions() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "NO_USER";
        Log.d("TransactionFragment", "Loading transactions for user: " + userId);

        if (mAuth.getCurrentUser() == null) {
            Log.w("TransactionFragment", "No user logged in");
            return;
        }

        db.collection("transactions")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("TransactionFragment", "Firestore error: " + error.getMessage(), error);
                        return;
                    }

                    List<Transaction> newList = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Transaction t = doc.toObject(Transaction.class);
                                t.setId(doc.getId());
                                newList.add(t);
                                Log.d("TransactionFragment", "Loaded: " + t.getType() + " - " + t.getCategory());
                            } catch (Exception e) {
                                Log.e("TransactionFragment", "Parse error: " + doc.getId(), e);
                            }
                        }
                    }

                    allTransactions.clear();
                    allTransactions.addAll(newList);
                    applyFilter();
                });
    }

    private void attachSwipeDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder v, int d) {
                int pos = v.getAdapterPosition();
                if (pos < 0 || pos >= transactionList.size()) return;

                Transaction t = transactionList.get(pos);
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Transaction?")
                        .setMessage("Delete " + t.getCategory() + " - $" + t.getAmount() + "?")
                        .setPositiveButton("Delete", (dd, ww) -> {
                            if (t.getId() != null) {
                                db.collection("transactions").document(t.getId()).delete();
                            }
                        })
                        .setNegativeButton("Cancel", (dd, ww) -> adapter.notifyItemChanged(pos))
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