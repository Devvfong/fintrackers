package com.example.fintracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvInitials;
    private ImageView ivProfilePicture;
    private LinearLayout menuAccount, menuLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ✅ Modern launcher - No deprecated startActivityForResult
    private final ActivityResultLauncher<Intent> accountLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            loadUserData();  // Refresh profile photo immediately ✅
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find all views
        tvUsername = view.findViewById(R.id.tvUsername);
        tvInitials = view.findViewById(R.id.tvInitials);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        menuAccount = view.findViewById(R.id.menuAccount);
        menuLogout = view.findViewById(R.id.menuLogout);

        // ✅ Modern launcher - Clean & simple
        menuAccount.setOnClickListener(v ->
                accountLauncher.launch(new Intent(requireContext(), AccountActivity.class)));

        menuLogout.setOnClickListener(v -> showLogoutDialog());

        loadUserData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();  // Refresh when returning from AccountActivity ✅
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
                                    .placeholder(R.drawable.ic_person)  // Add this drawable if missing
                                    .error(R.drawable.ic_person)
                                    .into(ivProfilePicture);
                            ivProfilePicture.setVisibility(View.VISIBLE);
                            tvInitials.setVisibility(View.GONE);
                        } else {
                            // Show initials
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
}
