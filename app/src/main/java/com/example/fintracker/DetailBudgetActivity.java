package com.example.fintracker;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DetailBudgetActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvCategory;
    private TextView tvRemainingAmount;
    private TextView tvOver;
    private ProgressBar progressRemaining;

    private RecyclerView rvBudgetTransactions;
    private TransactionAdapter adapter;
    private final List<Transaction> txList = new ArrayList<>();

    private Budget budget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_budget);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        String budgetId = getIntent().getStringExtra("budgetId");
        if (budgetId == null) {
            finish();
            return;
        }

        tvCategory = findViewById(R.id.tvCategory);
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount);
        tvOver = findViewById(R.id.tvOver);
        progressRemaining = findViewById(R.id.progressRemaining);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> confirmDelete(budgetId));

        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent i = new Intent(this, CreateBudgetActivity.class);
            i.putExtra("budgetId", budgetId);
            startActivity(i);
        });

        rvBudgetTransactions = findViewById(R.id.rvBudgetTransactions);
        rvBudgetTransactions.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TransactionAdapter(this, txList, new TransactionAdapter.Listener() {

            @Override
            public void onEditClick(Transaction t) {
                Intent i = new Intent(DetailBudgetActivity.this, AddTransactionActivity.class);
                i.putExtra("editMode", true);
                i.putExtra("transactionId", t.getId());
                startActivity(i);
            }

            @Override
            public void onRowClick(Transaction t) {
                Intent i = new Intent(DetailBudgetActivity.this, TransactionDetailActivity.class);
                i.putExtra("transactionId", t.getId());
                startActivity(i);
            }
        });


        rvBudgetTransactions.setAdapter(adapter);

        loadBudget(budgetId);
    }

    private void loadBudget(String budgetId) {
        db.collection("budgets").document(budgetId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        finish();
                        return;
                    }

                    budget = doc.toObject(Budget.class);
                    if (budget == null) {
                        finish();
                        return;
                    }
                    budget.setId(doc.getId());

                    tvCategory.setText(budget.getCategoryName());
                    loadExpenseTransactionsAndRender();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading budget: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadExpenseTransactionsAndRender() {
        if (auth.getCurrentUser() == null || budget == null) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "Expense")
                .get()
                .addOnSuccessListener(snap -> {
                    txList.clear();

                    double spent = 0.0;

                    for (QueryDocumentSnapshot doc : snap) {
                        Transaction t = doc.toObject(Transaction.class);
                        t.setId(doc.getId());

                        if (t.getCategory() == null) continue;

                        if (doesMatchBudget(budget, t)) {
                            txList.add(t);
                            spent += t.getAmount();
                        }
                    }

                    budget.setSpent(spent);
                    renderHeader();

                    adapter.updateTransactions(txList);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private boolean doesMatchBudget(Budget b, Transaction t) {
        boolean categoryMatches =
                t.getCategory().equalsIgnoreCase(b.getCategoryName()) ||
                        t.getCategory().equalsIgnoreCase(b.getCategoryId());

        if (!categoryMatches) return false;

        Calendar budgetCal = Calendar.getInstance();
        budgetCal.setTimeInMillis(b.getMonthTimestamp());

        Calendar transCal = Calendar.getInstance();
        transCal.setTimeInMillis(t.getTimestamp());

        return budgetCal.get(Calendar.MONTH) == transCal.get(Calendar.MONTH)
                && budgetCal.get(Calendar.YEAR) == transCal.get(Calendar.YEAR);
    }

    private void renderHeader() {
        int percentage = budget.getPercentageSpent();
        progressRemaining.setProgress(Math.min(100, percentage));

        if (budget.isExceeded()) {
            tvRemainingAmount.setText("$0");
            tvOver.setVisibility(View.VISIBLE);
        } else {
            tvRemainingAmount.setText(formatAmount(budget.getRemaining(), budget.getCurrency()));
            tvOver.setVisibility(View.GONE);
        }
    }

    private String formatAmount(double amount, String currency) {
        if ("KHR".equals(currency)) {
            DecimalFormat khrFormat = new DecimalFormat("#,###");
            return "៛" + khrFormat.format(amount);
        } else {
            NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);
            return usdFormat.format(amount);
        }
    }

    private void confirmDelete(String budgetId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Budget?")
                .setMessage("Are you sure you want to delete this budget?")
                .setPositiveButton("Delete", (d, w) ->
                        db.collection("budgets").document(budgetId).delete()
                                .addOnSuccessListener(x -> finish())
                                .addOnFailureListener(e -> Toast.makeText(this, "Delete error: " + e.getMessage(), Toast.LENGTH_SHORT).show())
                )
                .setNegativeButton("Cancel", null)
                .show();
    }
}
