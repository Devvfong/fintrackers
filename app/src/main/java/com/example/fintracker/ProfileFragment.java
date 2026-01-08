package com.example.fintracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private LinearLayout menuExport;        // ← NEW: Export menu item
    private TextView tvUsername, tvInitials;
    private ImageView ivProfilePicture;
    private LinearLayout menuAccount, menuLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Modern launcher for AccountActivity result
    private final ActivityResultLauncher<Intent> accountLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            loadUserData();  // Refresh profile photo immediately
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views
        tvUsername = view.findViewById(R.id.tvUsername);
        tvInitials = view.findViewById(R.id.tvInitials);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        menuAccount = view.findViewById(R.id.menuAccount);
        menuLogout = view.findViewById(R.id.menuLogout);
        menuExport = view.findViewById(R.id.menuExport);  // ← NEW

        // Click listeners
        menuAccount.setOnClickListener(v ->
                accountLauncher.launch(new Intent(requireContext(), AccountActivity.class)));

        menuLogout.setOnClickListener(v -> showLogoutDialog());

        // ← NEW: Export click listener
        menuExport.setOnClickListener(v -> exportTransactionsToCSV());

        loadUserData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();  // Refresh when returning from AccountActivity
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String photoUrl = documentSnapshot.getString("profilePhotoUrl");

                        // Update name
                        tvUsername.setText(name != null && !name.isEmpty() ? name : "User");

                        // Load photo or show initials
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(ivProfilePicture);
                            ivProfilePicture.setVisibility(View.VISIBLE);
                            tvInitials.setVisibility(View.GONE);
                        } else {
                            String initials = "U";
                            if (name != null && !name.isEmpty()) {
                                initials = name.substring(0, Math.min(2, name.length())).toUpperCase();
                            }
                            tvInitials.setText(initials);
                            ivProfilePicture.setVisibility(View.GONE);
                            tvInitials.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Silent fail - show defaults
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout?")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    // ← NEW: Export all user transactions to CSV
    private void exportTransactionsToCSV() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Exporting transactions...", Toast.LENGTH_SHORT).show();

        db.collection("transactions")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        // Filename with timestamp
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(new Date());
                        String fileName = "FinTracker_Transactions_" + timeStamp + ".csv";

                        // Save to Downloads folder
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs();
                        }
                        File csvFile = new File(downloadsDir, fileName);

                        FileWriter writer = new FileWriter(csvFile);

                        // CSV Header
                        writer.append("Date,Time,Type,Category,Amount,Wallet,Description\n");

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        // Write each transaction
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Transaction t = doc.toObject(Transaction.class);

                            String date = dateFormat.format(new Date(t.getTimestamp()));
                            String time = timeFormat.format(new Date(t.getTimestamp()));
                            String type = t.getType() != null ? t.getType() : "expense";
                            String category = t.getCategory() != null ? t.getCategory() : "";
                            String amount = String.valueOf(t.getAmount());
                            String wallet = t.getWallet() != null ? t.getWallet() : "";
                            String description = t.getDescription() != null ?
                                    t.getDescription().replace("\"", "\"\"") : ""; // Escape quotes

                            writer.append(date).append(',')
                                    .append(time).append(',')
                                    .append(type).append(',')
                                    .append('"').append(category).append('"').append(',')
                                    .append(amount).append(',')
                                    .append('"').append(wallet).append('"').append(',')
                                    .append('"').append(description).append('"')
                                    .append('\n');
                        }

                        writer.flush();
                        writer.close();

                        Toast.makeText(requireContext(),
                                "Export successful!\nSaved as:\n" + fileName + "\nLocation: Downloads",
                                Toast.LENGTH_LONG).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(),
                                "Export failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to load transactions: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}