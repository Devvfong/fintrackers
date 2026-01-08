package com.example.fintracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {

    private ImageView ivProfilePicture, btnEditName, btnBack;
    private CardView cvCameraIcon;
    private TextView tvInitials, tvDisplayName, tvName, tvEmail, tvUserId;
    private View menuChangePassword, menuDeleteAccount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private Uri cameraImageUri;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cropLauncher; // ✅ ADD THIS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        btnBack = findViewById(R.id.btnBack);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        cvCameraIcon = findViewById(R.id.ivCameraIcon);
        tvInitials = findViewById(R.id.tvInitials);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvUserId = findViewById(R.id.tvUserId);
        btnEditName = findViewById(R.id.btnEditName);
        menuChangePassword = findViewById(R.id.menuChangePassword);
        menuDeleteAccount = findViewById(R.id.menuDeleteAccount);

        initializeLaunchers();

        btnBack.setOnClickListener(v -> finish());
        ivProfilePicture.setOnClickListener(v -> showPhotoOptionsDialog());
        cvCameraIcon.setOnClickListener(v -> showPhotoOptionsDialog());
        loadUserData();
        btnEditName.setOnClickListener(v -> showEditNameDialog());
        menuChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        menuDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void initializeLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                        startCrop(cameraImageUri);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            startCrop(selectedImageUri);
                        }
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, R.string.camera_access_denied, Toast.LENGTH_SHORT).show();
                    }
                });

        // ✅ ADD CROP LAUNCHER
        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        final Uri croppedUri = UCrop.getOutput(result.getData());
                        if (croppedUri != null) {
                            uploadProfilePhoto(croppedUri);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        Toast.makeText(this, getString(R.string.crop_error,
                                        cropError != null ? cropError.getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserData() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String email = currentUser.getEmail();
            tvEmail.setText(email != null ? email : getString(R.string.no_email));
            tvUserId.setText(userId);

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String profilePhotoUrl = documentSnapshot.getString("profilePhotoUrl");

                            if (name != null && !name.isEmpty()) {
                                tvName.setText(name);
                                tvDisplayName.setText(name);
                                String initials = name.substring(0, Math.min(2, name.length())).toUpperCase();
                                tvInitials.setText(initials);
                            } else {
                                setDefaultName();
                            }

                            if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty()) {
                                loadProfilePhoto(profilePhotoUrl);
                            } else {
                                showInitials();
                            }

                        } else {
                            setDefaultName();
                            showInitials();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, R.string.failed_to_load_user_data, Toast.LENGTH_SHORT).show();
                        setDefaultName();
                        showInitials();
                    });
        }
    }

    private void loadProfilePhoto(String photoUrl) {
        tvInitials.setVisibility(View.GONE);
        ivProfilePicture.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(ivProfilePicture);
    }

    private void showInitials() {
        ivProfilePicture.setVisibility(View.GONE);
        tvInitials.setVisibility(View.VISIBLE);
    }

    private void setDefaultName() {
        String defaultName = getString(R.string.default_user_name);
        tvName.setText(defaultName);
        tvDisplayName.setText(defaultName);
        tvInitials.setText(R.string.default_user_initials);
    }

    private void showPhotoOptionsDialog() {
        String[] options = {
                getString(R.string.take_photo),
                getString(R.string.choose_from_gallery),
                getString(R.string.remove_photo)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_photo)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            checkCameraPermissionAndOpen();
                            break;
                        case 1:
                            openGallery();
                            break;
                        case 2:
                            removeProfilePhoto();
                            break;
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getCacheDir(), "profile_photo_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                photoFile
        );
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void startCrop(Uri sourceUri) {
        String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setCompressionQuality(80);
        options.setToolbarColor(ContextCompat.getColor(this, R.color.purple));           // Toolbar background
//        options.setStatusBarColor(ContextCompat.getColor(this, R.color.purple_dark));   // ← Correct place
        options.setToolbarTitle(getString(R.string.profile_photo));

        // Best fix for hard-to-tap confirm button
        options.setHideBottomControls(true);  // Removes bottom bar → more space + easier access

        // Make icons white on dark toolbar
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.white));

        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800)
                .withOptions(options)
                .getIntent(this);

        cropLauncher.launch(cropIntent);
    }


    // ✅ REMOVE THIS METHOD COMPLETELY - NOT NEEDED
    // @Override
    // protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    //     DELETE THIS
    // }

    private void uploadProfilePhoto(Uri imageUri) {
        if (currentUser == null) return;

        // ✅ Show the image immediately from local URI

        tvInitials.setVisibility(View.GONE);
        ivProfilePicture.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(imageUri)
                .circleCrop()
                .into(ivProfilePicture);

        String userId = currentUser.getUid();
        StorageReference profilePhotoRef = storage.getReference()
                .child("profile_photos/" + userId + ".jpg");

        Toast.makeText(this, R.string.uploading_photo, Toast.LENGTH_SHORT).show();

        profilePhotoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    profilePhotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profilePhotoUrl", photoUrl);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();

                                    // ✅ COMPLETE FIX - Notifies ProfileFragment & MainActivity
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("profile_updated", true);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();  // Close activity
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, R.string.failed_to_save_photo_url, Toast.LENGTH_SHORT).show();
                                });

                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.failed_to_upload_photo, Toast.LENGTH_SHORT).show();
                });

    }

    private void removeProfilePhoto() {
        if (currentUser == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_photo)
                .setMessage(R.string.remove_photo_confirmation)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    String userId = currentUser.getUid();
                    StorageReference profilePhotoRef = storage.getReference()
                            .child("profile_photos/" + userId + ".jpg");

                    profilePhotoRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("profilePhotoUrl", null);

                                db.collection("users").document(userId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid1 -> {
                                            showInitials();
                                            Toast.makeText(this, R.string.profile_photo_removed, Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, R.string.failed_to_remove_photo, Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(tvName.getText().toString());
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_name)
                .setView(input)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateName(newName);
                    } else {
                        Toast.makeText(this, R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateName(String newName) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);

            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        tvName.setText(newName);
                        tvDisplayName.setText(newName);
                        String initials = newName.substring(0, Math.min(2, newName.length())).toUpperCase();
                        tvInitials.setText(initials);
                        Toast.makeText(this, R.string.name_updated_successfully, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, R.string.failed_to_update_name, Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setView(dialogView)
                .setPositiveButton(R.string.change_password, (dialog, which) -> {
                    String currentPassword = etCurrentPassword.getText().toString();
                    String newPassword = etNewPassword.getText().toString();
                    String confirmPassword = etConfirmPassword.getText().toString();

                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(this, R.string.please_fill_all_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, R.string.new_passwords_dont_match, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        Toast.makeText(this, R.string.password_must_be_6_chars, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    changePassword(currentPassword, newPassword);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        if (currentUser != null && currentUser.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(
                    currentUser.getEmail(),
                    currentPassword
            );

            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        currentUser.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(this, R.string.password_changed_successfully, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, getString(R.string.failed_to_change_password, e.getMessage()), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, R.string.current_password_incorrect, Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_warning)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    showPasswordConfirmationDialog();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showPasswordConfirmationDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.password);
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_deletion)
                .setMessage(R.string.enter_password_to_confirm)
                .setView(input)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    String password = input.getText().toString();
                    if (!password.isEmpty()) {
                        deleteAccount(password);
                    } else {
                        Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAccount(String password) {
        if (currentUser != null && currentUser.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(
                    currentUser.getEmail(),
                    password
            );

            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        String userId = currentUser.getUid();
                        deleteUserData(userId, () -> {
                            currentUser.delete()
                                    .addOnSuccessListener(aVoid1 -> {
                                        Toast.makeText(this, R.string.account_deleted_successfully, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, getString(R.string.failed_to_delete_account, e.getMessage()), Toast.LENGTH_SHORT).show();
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, R.string.password_incorrect, Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteUserData(String userId, Runnable onComplete) {
        StorageReference profilePhotoRef = storage.getReference()
                .child("profile_photos/" + userId + ".jpg");

        profilePhotoRef.delete();
        db.collection("users").document(userId).delete();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });

        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    onComplete.run();
                });
    }
}
