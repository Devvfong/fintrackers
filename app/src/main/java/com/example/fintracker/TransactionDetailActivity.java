package com.example.fintracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        // Find all views
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvAmount = findViewById(R.id.tvAmount);
        TextView tvCategory = findViewById(R.id.tvCategory);
        TextView tvDateTime = findViewById(R.id.tvDateTime);
        TextView tvType = findViewById(R.id.tvType);
        TextView tvDetailCategory = findViewById(R.id.tvDetailCategory);
        TextView tvWallet = findViewById(R.id.tvWallet);
        TextInputEditText tvDescription = findViewById(R.id.tvDescription);
        ImageView imgAttachment = findViewById(R.id.imgAttachment);
        LinearLayout layoutAttachment = findViewById(R.id.layoutAttachment);

        tvTitle.setText("Detail Transaction");

        // 🔥 GET TRANSACTION ID
        String transactionId = getIntent().getStringExtra("transactionId");
        Log.d("TransactionDetail", "Transaction ID: " + transactionId);
        Toast.makeText(this, "ID: " + transactionId, Toast.LENGTH_SHORT).show();

        if (transactionId == null || transactionId.trim().isEmpty()) {
            Toast.makeText(this, "No transaction ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 🔥 LOAD FROM FIRESTORE
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("transactions").document(transactionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("TransactionDetail", "Document exists: " + documentSnapshot.exists());

                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Transaction not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    Transaction transaction = documentSnapshot.toObject(Transaction.class);
                    if (transaction == null) {
                        Toast.makeText(this, "Failed to parse transaction", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Log.d("TransactionDetail", "Type: " + transaction.getType() +
                            " | Category: " + transaction.getCategory() +
                            " | Wallet: " + transaction.getWallet());

                    // 🔥 SET ALL TEXT VIEWS - THIS WAS MISSING!
                    tvAmount.setText(String.format(Locale.US, "$%.2f", transaction.getAmount()));

                    String categoryText = transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty()
                            ? transaction.getDescription() : transaction.getCategory();
                    tvCategory.setText(categoryText);

                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault());
                    tvDateTime.setText(sdf.format(new Date(transaction.getTimestamp())));

                    // 🔥 THESE WERE MISSING - NOW FIXED!
                    tvType.setText(transaction.getType() != null ? transaction.getType() : "N/A");
                    tvDetailCategory.setText(transaction.getCategory() != null ? transaction.getCategory() : "N/A");
                    tvWallet.setText(transaction.getWallet() != null ? transaction.getWallet() : "N/A");
                    tvDescription.setText(transaction.getDescription() != null ? transaction.getDescription() : "");

                    // Attachment
                    String attachmentUrl = transaction.getAttachmentUrl();
                    if (attachmentUrl != null && !attachmentUrl.trim().isEmpty()) {
                        Glide.with(this).load(attachmentUrl).into(imgAttachment);
                        layoutAttachment.setVisibility(View.VISIBLE);
                    } else {
                        layoutAttachment.setVisibility(View.GONE);
                    }

                    Toast.makeText(this, "Loaded: " + transaction.getCategory(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("TransactionDetail", "Firestore error", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        btnBack.setOnClickListener(v -> finish());
    }
}
