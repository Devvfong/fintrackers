package com.example.fintracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText etAmount;
    private AutoCompleteTextView actvCategory, actvWallet;
    private TextInputEditText etDescription;
    private MaterialButton btnContinue;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etAmount = findViewById(R.id.etAmount);
        actvCategory = findViewById(R.id.actvCategory);
        actvWallet = findViewById(R.id.actvWallet);
        etDescription = findViewById(R.id.etDescription);
        btnContinue = findViewById(R.id.btnContinue);

        // Setup category dropdown
        String[] categories = {"Shopping", "Subscription", "Food", "Transport", "Salary"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(categoryAdapter);

        // Setup wallet dropdown
        String[] wallets = {"Cash", "Bank", "Card"};
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, wallets);
        actvWallet.setAdapter(walletAdapter);

        // Continue button
        btnContinue.setOnClickListener(v -> saveTransaction());
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String wallet = actvWallet.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(wallet)) {
            Toast.makeText(this, "Please select wallet", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String userId = mAuth.getCurrentUser().getUid();

        // Determine transaction type based on category
        String type = category.equals("Salary") ? "Income" : "Expense";

        // Create transaction map
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", userId);
        transaction.put("amount", amount);
        transaction.put("category", category);
        transaction.put("wallet", wallet);
        transaction.put("description", description);
        transaction.put("type", type);
        transaction.put("timestamp", System.currentTimeMillis());

        // Save to Firestore
        btnContinue.setEnabled(false);
        btnContinue.setText("Saving...");

        db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Transaction added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                });
    }
}