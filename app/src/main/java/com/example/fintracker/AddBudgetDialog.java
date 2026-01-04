package com.example.fintracker;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddBudgetDialog extends DialogFragment {

    private EditText etLimit;
    private Spinner spinnerCategory;
    private Spinner spinnerCurrency;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_budget, null);

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Views
        etLimit = view.findViewById(R.id.etBudgetLimit);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerCurrency = view.findViewById(R.id.spinnerCurrency);

        Button btnSave = view.findViewById(R.id.btnSaveBudget);
        Button btnCancel = view.findViewById(R.id.btnCancelBudget);

        // Category spinner
        String[] categories = {"Shopping", "Food", "Transport", "Subscription", "Bills", "Entertainment", "Healthcare", "Salary", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Currency spinner
        String[] currencies = {"USD", "KHR"};
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                currencies
        );
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(currencyAdapter);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> saveBudget(dialog));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private void saveBudget(AlertDialog dialog) {
        String limitStr = etLimit.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String currency = spinnerCurrency.getSelectedItem().toString();

        if (limitStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter budget limit", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(limitStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(getContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        Map<String, Object> budget = new HashMap<>();
        budget.put("userId", userId);
        budget.put("category", category);
        budget.put("amount", amount);
        budget.put("currency", currency);
        budget.put("spent", 0.0);
        budget.put("timestamp", System.currentTimeMillis());

        db.collection("budgets")
                .add(budget)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Budget created!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to create budget: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
