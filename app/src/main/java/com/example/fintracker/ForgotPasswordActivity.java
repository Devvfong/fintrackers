package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnSendReset;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());

        btnSendReset.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        btnSendReset.setEnabled(false);
        btnSendReset.setText("Sending...");

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.reset_link_sent, Toast.LENGTH_LONG).show();
                    finish(); // return to login
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            getString(R.string.reset_link_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    btnSendReset.setEnabled(true);
                    btnSendReset.setText(R.string.send_reset_link);
                });
    }
}