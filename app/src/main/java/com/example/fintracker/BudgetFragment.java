package com.example.fintracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import java.util.List;

public class BudgetFragment extends Fragment {

    private BudgetAdapter adapter;
    private List<Budget> budgetList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        RecyclerView rvBudgets = view.findViewById(R.id.rvBudgets);
        FloatingActionButton fabCreateBudget = view.findViewById(R.id.fabCreateBudget);

        // Setup RecyclerView
        budgetList = new ArrayList<>();
        adapter = new BudgetAdapter(getContext(), budgetList);
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(adapter);

        // FAB click
        fabCreateBudget.setOnClickListener(v -> {
            // TODO: Open create budget dialog
        });

        // Load budgets
        loadBudgets();

        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadBudgets() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    budgetList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Budget budget = doc.toObject(Budget.class);
                            budget.setId(doc.getId());
                            budgetList.add(budget);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
