package com.example.fintracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fintracker.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CreateBudgetActivity - Create or Edit Budget
 * <p>
 * FEATURES:
 * 1. Amount input with validation
 * 2. Currency selector: USD or KHR ONLY
 * 3. Month selector: This Month or Last Month ONLY (no future)
 * 4. Category selector with "Add New Category" button
 * 5. Edit existing budget
 */
public class CreateBudgetActivity extends AppCompatActivity {

    // Views
    private EditText etAmount;
    private Spinner spinnerCurrency, spinnerMonth, spinnerCategory;
    private Button btnSave, btnAddCategory;
    private ImageButton btnBack;
    private TextView tvTitle;
    private LinearLayout layoutAddCategory;
    private EditText etNewCategoryName;
    private Button btnSaveNewCategory, btnCancelNewCategory;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Data
    private final List<String> categoryList = new ArrayList<>();
    private final List<String> categoryIdList = new ArrayList<>();
    private boolean isEditMode = false;
    private String budgetId = null;

    // Month options (This Month and Last Month ONLY)
    private final long[] monthTimestamps = new long[2];
    private final String[] monthNames = new String[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_budget);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

        // Check if edit mode
        checkEditMode();

        // Setup spinners
        setupCurrencySpinner();
        setupMonthSpinner();
        loadCategories();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        etAmount = findViewById(R.id.etAmount);
        spinnerCurrency = findViewById(R.id.spinnerCurrency);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        // Add new category section (initially hidden)
        layoutAddCategory = findViewById(R.id.layoutAddCategory);
        etNewCategoryName = findViewById(R.id.etNewCategoryName);
        btnSaveNewCategory = findViewById(R.id.btnSaveNewCategory);
        btnCancelNewCategory = findViewById(R.id.btnCancelNewCategory);

        layoutAddCategory.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void checkEditMode() {
        budgetId = getIntent().getStringExtra("budgetId");
        isEditMode = budgetId != null;

        if (isEditMode) {
            tvTitle.setText("Edit Budget");
            btnSave.setText("Update");
            loadBudgetData();
        } else {
            tvTitle.setText("Create Budget");
            btnSave.setText("Create");
        }
    }

    /**
     * Setup Currency Spinner - USD and KHR ONLY
     */
    private void setupCurrencySpinner() {
        String[] currencies = {"USD", "KHR"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                currencies
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(adapter);
    }

    /**
     * Setup Month Spinner - This Month and Last Month ONLY
     * Future months are NOT included
     */
    private void setupMonthSpinner() {
        // Get This Month timestamp
        monthTimestamps[0] = DateUtils.getThisMonthStartTimestamp();
        monthNames[0] = DateUtils.getMonthName(monthTimestamps[0]);

        // Get Last Month timestamp
        monthTimestamps[1] = DateUtils.getLastMonthStartTimestamp();
        monthNames[1] = DateUtils.getMonthName(monthTimestamps[1]);

        // Create adapter with ONLY 2 options
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                monthNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);
    }

    /**
     * Load categories from Firestore (global category list)
     */
    private void loadCategories() {
        db.collection("categories")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    categoryIdList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryId = doc.getId();
                        String categoryName = doc.getString("name");

                        categoryList.add(categoryName);
                        categoryIdList.add(categoryId);
                    }

                    // Update spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            categoryList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);

                    // If edit mode, set selected category
                    if (isEditMode) {
                        setEditModeCategory();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading categories: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    /**
     * Setup button listeners
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveBudget());
        btnAddCategory.setOnClickListener(v -> showAddCategorySection());
        btnSaveNewCategory.setOnClickListener(v -> saveNewCategory());
        btnCancelNewCategory.setOnClickListener(v -> hideAddCategorySection());
    }

    /**
     * Show "Add New Category" section
     */
    private void showAddCategorySection() {
        layoutAddCategory.setVisibility(View.VISIBLE);
        btnAddCategory.setVisibility(View.GONE);
        etNewCategoryName.requestFocus();
    }

    /**
     * Hide "Add New Category" section
     */
    private void hideAddCategorySection() {
        layoutAddCategory.setVisibility(View.GONE);
        btnAddCategory.setVisibility(View.VISIBLE);
        etNewCategoryName.setText("");
    }

    /**
     * Save new category to global database
     * This makes it immediately available across the app
     */
    private void saveNewCategory() {
        String categoryName = etNewCategoryName.getText().toString().trim();

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please enter category name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if category already exists
        if (categoryList.contains(categoryName)) {
            Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to Firestore (global categories collection)
        Map<String, Object> category = new HashMap<>();
        category.put("name", categoryName);
        category.put("type", "both"); // Can be used for both income and expense
        category.put("isCustom", true);
        category.put("createdAt", System.currentTimeMillis());

        db.collection("categories")
                .add(category)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Category added!", Toast.LENGTH_SHORT).show();
                    hideAddCategorySection();

                    // Reload categories to show new one
                    loadCategories();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error adding category: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    /**
     * Validate and save budget
     */
    private void saveBudget() {
        // Get values
        String amountStr = etAmount.getText().toString().trim();
        String currency = spinnerCurrency.getSelectedItem().toString();
        int monthIndex = spinnerMonth.getSelectedItemPosition();
        long selectedMonthTimestamp = monthTimestamps[monthIndex];
        int categoryIndex = spinnerCategory.getSelectedItemPosition();

        // Validation
        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than 0");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            etAmount.requestFocus();
            return;
        }

        // Currency validation (USD or KHR only)
        if (!currency.equals("USD") && !currency.equals("KHR")) {
            Toast.makeText(this, "Invalid currency. Only USD or KHR allowed.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Month validation (This Month or Last Month only)
        if (!DateUtils.isValidBudgetMonth(selectedMonthTimestamp)) {
            Toast.makeText(this, "Invalid month. Only This Month or Last Month allowed.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Category validation
        if (categoryIndex < 0 || categoryIndex >= categoryList.size()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        String categoryId = categoryIdList.get(categoryIndex);
        String categoryName = categoryList.get(categoryIndex);

        // Save or update budget
        if (isEditMode) {
            updateBudget(amount, currency, selectedMonthTimestamp, categoryId, categoryName);
        } else {
            createBudget(amount, currency, selectedMonthTimestamp, categoryId, categoryName);
        }
    }

    /**
     * Create new budget
     */
    private void createBudget(double amount, String currency, long monthTimestamp,
                              String categoryId, String categoryName) {
        String userId = mAuth.getCurrentUser().getUid();

        // Check if budget already exists for this category and month
        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("monthTimestamp", monthTimestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Budget already exists for this category and month",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Create budget
                    Map<String, Object> budget = new HashMap<>();
                    budget.put("userId", userId);
                    budget.put("categoryId", categoryId);
                    budget.put("categoryName", categoryName);
                    budget.put("amount", amount);
                    budget.put("currency", currency);
                    budget.put("monthTimestamp", monthTimestamp);
                    budget.put("spent", 0.0);
                    budget.put("createdAt", System.currentTimeMillis());
                    budget.put("updatedAt", System.currentTimeMillis());

                    db.collection("budgets")
                            .add(budget)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Budget created successfully!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error creating budget: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                });
    }

    /**
     * Update existing budget
     */
    private void updateBudget(double amount, String currency, long monthTimestamp,
                              String categoryId, String categoryName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("categoryId", categoryId);
        updates.put("categoryName", categoryName);
        updates.put("amount", amount);
        updates.put("currency", currency);
        updates.put("monthTimestamp", monthTimestamp);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("budgets")
                .document(budgetId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Budget updated successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error updating budget: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    /**
     * Load budget data for edit mode
     */
    private void loadBudgetData() {
        db.collection("budgets")
                .document(budgetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set amount
                        Double amount = documentSnapshot.getDouble("amount");
                        if (amount != null) {
                            etAmount.setText(String.valueOf(amount));
                        }

                        // Set currency
                        String currency = documentSnapshot.getString("currency");
                        if (currency != null) {
                            if (currency.equals("USD")) {
                                spinnerCurrency.setSelection(0);
                            } else if (currency.equals("KHR")) {
                                spinnerCurrency.setSelection(1);
                            }
                        }

                        // Set month
                        Long monthTimestamp = documentSnapshot.getLong("monthTimestamp");
                        if (monthTimestamp != null) {
                            if (monthTimestamp == monthTimestamps[0]) {
                                spinnerMonth.setSelection(0); // This Month
                            } else if (monthTimestamp == monthTimestamps[1]) {
                                spinnerMonth.setSelection(1); // Last Month
                            }
                        }

                        // Category will be set after categories are loaded
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading budget: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Set category in edit mode (called after categories are loaded)
     */
    private void setEditModeCategory() {
        db.collection("budgets")
                .document(budgetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String categoryId = documentSnapshot.getString("categoryId");
                        if (categoryId != null) {
                            int index = categoryIdList.indexOf(categoryId);
                            if (index >= 0) {
                                spinnerCategory.setSelection(index);
                            }
                        }
                    }
                });
    }
}