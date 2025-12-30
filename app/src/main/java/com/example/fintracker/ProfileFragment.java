package com.example.fintracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvInitials;
    private LinearLayout menuAccount, menuSettings, menuExportData, menuLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvUsername = view.findViewById(R.id.tvUsername);
        tvInitials = view.findViewById(R.id.tvInitials);
        menuAccount = view.findViewById(R.id.menuAccount);
        menuSettings = view.findViewById(R.id.menuSettings);
        menuExportData = view.findViewById(R.id.menuExportData);
        menuLogout = view.findViewById(R.id.menuLogout);

        // Load user data
        loadUserData();

        // Menu clicks
        menuAccount.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Account settings coming soon", Toast.LENGTH_SHORT).show();
        });

        menuSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Settings coming soon", Toast.LENGTH_SHORT).show();
        });

        menuExportData.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Export data coming soon", Toast.LENGTH_SHORT).show();
        });

        menuLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });

        return view;
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            tvUsername.setText(name != null ? name : "User");

                            // Set initials
                            if (name != null && !name.isEmpty()) {
                                String initials = name.substring(0, Math.min(2, name.length())).toUpperCase();
                                tvInitials.setText(initials);
                            }
                        }
                    });
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Logout?")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
