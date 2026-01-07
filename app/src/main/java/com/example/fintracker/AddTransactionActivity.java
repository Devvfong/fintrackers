package com.example.fintracker;
import android.view.View;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AddTransactionActivity extends AppCompatActivity {
    private ImageView imgAttachmentPreview;
    private View attachmentPreviewContainer;

    private boolean isEditMode = false;
    private String editingTransactionId = null;

    private EditText etAmount;
    private AutoCompleteTextView actvCategory, actvWallet;
    private TextInputEditText etDescription, etDate;
    private MaterialButton btnContinue;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private String attachmentUrl = null;
    private long selectedTimestamp;
    private Uri cameraImageUri;

    private boolean isUploading = false;

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestReadStoragePermissionLauncher;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;
    private ActivityResultLauncher<Intent> legacyGalleryLauncher;

    @SuppressLint({"SetTextI18n", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        etAmount = findViewById(R.id.etAmount);
        actvCategory = findViewById(R.id.actvCategory);
        actvWallet = findViewById(R.id.actvWallet);

// Setup category dropdown to show on click
        actvCategory.setThreshold(0);
        actvCategory.setOnClickListener(v -> actvCategory.showDropDown());

// Setup wallet dropdown
        actvWallet.setThreshold(0);
        actvWallet.setOnClickListener(v -> actvWallet.showDropDown());
        actvWallet = findViewById(R.id.actvWallet);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);

        btnContinue = findViewById(R.id.btnContinue);
        ImageButton btnBack = findViewById(R.id.btnBack);
        LinearLayout layoutAttachmentRow = findViewById(R.id.layoutAttachmentRow);
        attachmentPreviewContainer = findViewById(R.id.attachmentPreviewContainer);
        imgAttachmentPreview = findViewById(R.id.imgAttachmentPreview);
        ImageButton btnRemoveAttachment = findViewById(R.id.btnRemoveAttachment);

        attachmentPreviewContainer.setVisibility(View.GONE);
        btnRemoveAttachment.setOnClickListener(v -> {
            attachmentUrl = null;
            attachmentPreviewContainer.setVisibility(View.GONE);
            imgAttachmentPreview.setImageDrawable(null);
            Toast.makeText(this, "Attachment removed", Toast.LENGTH_SHORT).show();
        });

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("editMode", false)) {
            isEditMode = true;
            editingTransactionId = intent.getStringExtra("transactionId");

            String amount = intent.getStringExtra("amount");
            String category = intent.getStringExtra("category");
            String wallet = intent.getStringExtra("wallet");
            String description = intent.getStringExtra("description");
            long ts = intent.getLongExtra("timestamp", System.currentTimeMillis());
            String existingAttachmentUrl = intent.getStringExtra("attachmentUrl");

            if (amount != null) etAmount.setText(amount);
            if (category != null) actvCategory.setText(category, false);
            if (wallet != null) actvWallet.setText(wallet, false);
            if (description != null) etDescription.setText(description);

            selectedTimestamp = ts;
            updateDateUi(selectedTimestamp);

            // show existing attachment preview if you want
            if (existingAttachmentUrl != null && !existingAttachmentUrl.trim().isEmpty()) {
                attachmentUrl = existingAttachmentUrl; // keep it unless user changes/removes
                if (attachmentPreviewContainer != null) attachmentPreviewContainer.setVisibility(View.VISIBLE);
                if (imgAttachmentPreview != null) {
                    com.bumptech.glide.Glide.with(this).load(existingAttachmentUrl).into(imgAttachmentPreview);
                }
            }

            btnContinue.setText("Update");
        }


        setupActivityResultLaunchers();
        setupDropdowns();
        setupAmountInput();

        selectedTimestamp = System.currentTimeMillis();
        updateDateUi(selectedTimestamp);

        btnBack.setOnClickListener(v -> finish());
        layoutAttachmentRow.setOnClickListener(v -> showAttachmentOptions());
        etDate.setOnClickListener(v -> showDatePicker());

        btnContinue.setOnClickListener(v -> saveTransaction());
        setContinueStateIdle();
    }

    private void setupDropdowns() {
        // ===== FIX FOR CATEGORY DROPDOWN =====
        // Make category dropdown show when tapped
        actvCategory.setThreshold(0);  // Show dropdown with 0 characters typed
        actvCategory.setOnClickListener(v -> actvCategory.showDropDown());
        actvCategory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvCategory.showDropDown();
            }
        });

        // Load categories from Firestore
        loadCategoriesFromFirestore();

        // ===== FIX FOR WALLET DROPDOWN =====
        // Make wallet dropdown show when tapped
        actvWallet.setThreshold(0);
        actvWallet.setOnClickListener(v -> actvWallet.showDropDown());
        actvWallet.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvWallet.showDropDown();
            }
        });

        // Set wallet options
        String[] wallets = {"Cash", "Bank Account", "Credit Card", "E-Wallet"};
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, wallets);
        actvWallet.setAdapter(walletAdapter);
    }

    /**
     * Load categories from Firestore (synced with Budget)
     * INCLUDES "+ Create New Category" option
     */
    private void loadCategoriesFromFirestore() {
        db.collection("categories")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> categories = new ArrayList<>();

                    // Add categories from Firestore
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String categoryName = doc.getString("name");
                        if (categoryName != null && !categoryName.trim().isEmpty()) {
                            categories.add(categoryName.trim());
                        }
                    }

                    // If no categories in Firestore, add defaults
                    if (categories.isEmpty()) {
                        categories.add("Shopping");
                        categories.add("Food");
                        categories.add("Transport");
                        categories.add("Subscription");
                        categories.add("Bills");
                        categories.add("Entertainment");
                        categories.add("Healthcare");
                        categories.add("Salary");
                        categories.add("Other");
                    }

                    // Add "+ Create New Category" option at the end
                    categories.add("+ Create New Category");

                    // Setup adapter
                    String[] categoryArray = categories.toArray(new String[0]);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            categoryArray
                    );
                    actvCategory.setAdapter(adapter);

                    // Handle category selection
                    actvCategory.setOnItemClickListener((parent, view, position, id) -> {
                        String selected = (String) parent.getItemAtPosition(position);

                        if (selected.equals("+ Create New Category")) {
                            // Clear the selection
                            actvCategory.setText("", false);
                            // Show dialog to create new category
                            showCreateCategoryDialog();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Fallback to default categories if Firestore fails
                    List<String> categories = new ArrayList<>();
                    categories.add("Shopping");
                    categories.add("Food");
                    categories.add("Transport");
                    categories.add("Subscription");
                    categories.add("Bills");
                    categories.add("Entertainment");
                    categories.add("Healthcare");
                    categories.add("Salary");
                    categories.add("Other");
                    categories.add("+ Create New Category");

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            categories.toArray(new String[0])
                    );
                    actvCategory.setAdapter(adapter);

                    Toast.makeText(this, "Using default categories", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Show dialog to create new category
     * NEW category is automatically synced to Budget!
     */
    private void showCreateCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Category");

        // Create EditText for category name
        final EditText input = new EditText(this);
        input.setHint("Category name");
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();

            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if category already exists
            db.collection("categories")
                    .whereEqualTo("name", categoryName)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                            // Set the existing category
                            actvCategory.setText(categoryName, false);
                        } else {
                            // Create new category in Firestore
                            Map<String, Object> category = new HashMap<>();
                            category.put("name", categoryName);
                            category.put("createdAt", System.currentTimeMillis());

                            db.collection("categories")
                                    .add(category)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "✅ Category created: " + categoryName,
                                                Toast.LENGTH_SHORT).show();

                                        // Reload categories to show new one
                                        loadCategoriesFromFirestore();

                                        // Select the new category
                                        actvCategory.setText(categoryName, false);
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Error creating category: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show()
                                    );
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error checking category: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private void setupAmountInput() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @SuppressLint("SetTextI18n")
            @Override public void afterTextChanged(Editable s) {
                if (s == null) return;
                String text = s.toString();
                if (!text.isEmpty() && !text.startsWith("$")) {
                    etAmount.removeTextChangedListener(this);
                    etAmount.setText("$" + text.replace("$", ""));
                    etAmount.setSelection(etAmount.getText().length());
                    etAmount.addTextChangedListener(this);
                }
            }
        });
    }

    private void setupActivityResultLaunchers() {

        requestCameraPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) openCamera();
                    else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                });

        requestReadStoragePermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) openGallery();
                    else Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
                });

        takePictureLauncher =
                registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                    if (success && cameraImageUri != null) {
                        uploadImageUriToFirebase(cameraImageUri);
                    }
                });

        photoPickerLauncher =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) uploadImageUriToFirebase(uri);
                });

        legacyGalleryLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) uploadImageUriToFirebase(uri);
                    }
                });
    }

    private void showAttachmentOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Remove Attachment", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Add Attachment")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen();
                    else if (which == 1) checkGalleryPermissionAndOpen();
                    else if (which == 2) {
                        attachmentUrl = null;
                        Toast.makeText(this, "Attachment removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkGalleryPermissionAndOpen() {
        // Android 13+ Photo Picker needs no permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openGallery();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestReadStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openCamera() {
        try {
            File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (picturesDir == null) {
                Toast.makeText(this, "Storage not available", Toast.LENGTH_SHORT).show();
                return;
            }

            File imageFile = File.createTempFile(
                    "receipt_" + System.currentTimeMillis(),
                    ".jpg",
                    picturesDir
            );

            // Your manifest uses ${applicationId}.fileprovider
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );

            takePictureLauncher.launch(cameraImageUri);

        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()
            );
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            legacyGalleryLauncher.launch(intent);
        }
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedTimestamp);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, y);
                    picked.set(Calendar.MONTH, m);
                    picked.set(Calendar.DAY_OF_MONTH, d);

                    // Keep time-of-day from "now"
                    Calendar now = Calendar.getInstance();
                    picked.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
                    picked.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
                    picked.set(Calendar.SECOND, 0);
                    picked.set(Calendar.MILLISECOND, 0);

                    selectedTimestamp = picked.getTimeInMillis();
                    updateDateUi(selectedTimestamp);
                },
                year, month, day
        );
        dialog.show();
    }

    private void updateDateUi(long ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etDate.setText(sdf.format(new java.util.Date(ts)));
    }

    @SuppressLint("SetTextI18n")
    private void setContinueStateUploading() {
        isUploading = true;
        btnContinue.setEnabled(false);
        btnContinue.setText("Uploading...");
    }

    @SuppressLint("SetTextI18n")
    private void setContinueStateIdle() {
        isUploading = false;
        btnContinue.setEnabled(true);
        btnContinue.setText("Continue");
    }

    @SuppressLint("SetTextI18n")
    private void uploadImageUriToFirebase(Uri uri) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        setContinueStateUploading();

        String userId = mAuth.getCurrentUser().getUid();
        String fileName = "receipts/" + userId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot ->
                        taskSnapshot.getStorage().getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    attachmentUrl = downloadUri.toString();

                                    Glide.with(this)
                                            .load(uri)  // show immediately from local uri
                                            .into(imgAttachmentPreview);
                                    attachmentPreviewContainer.setVisibility(View.VISIBLE);

                                    Toast.makeText(this, "✅ Attachment uploaded!", Toast.LENGTH_SHORT).show();
                                    setContinueStateIdle();
                                })

                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "❌ URL failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    setContinueStateIdle();
                                })
                )
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setContinueStateIdle();
                });
    }

    private void performSave(Map<String, Object> transaction) {
        if (isEditMode && editingTransactionId != null && !editingTransactionId.trim().isEmpty()) {
            db.collection("transactions")
                    .document(editingTransactionId)
                    .update(transaction)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.msg_update_success, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.msg_update_error) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        setContinueStateIdle();
                    });
        } else {
            db.collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, R.string.msg_add_success, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.msg_add_error) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        setContinueStateIdle();
                    });
        }
    }

    @SuppressLint("SetTextI18n")
    private void saveTransaction() {
        if (isUploading) {
            Toast.makeText(this, R.string.msg_wait_upload, Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim()
                .replace("$", "").replace(",", "");
        String category = actvCategory.getText().toString().trim();
        String wallet = actvWallet.getText().toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();

        if (TextUtils.isEmpty(amountStr) || amountStr.equals("0")) {
            Toast.makeText(this, R.string.msg_enter_amount, Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, R.string.msg_select_category, Toast.LENGTH_SHORT).show();
            actvCategory.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(wallet)) {
            Toast.makeText(this, R.string.msg_select_wallet, Toast.LENGTH_SHORT).show();
            actvWallet.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.msg_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.msg_not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String type = category.equals("Salary") ? "Income" : "Expense";

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", userId);
        transaction.put("amount", amount);
        transaction.put("category", category);
        transaction.put("wallet", wallet);
        transaction.put("description", description);
        transaction.put("type", type);
        transaction.put("timestamp", selectedTimestamp);
        transaction.put("isRepeated", false);
        if (attachmentUrl != null && !attachmentUrl.trim().isEmpty()) {
            transaction.put("attachmentUrl", attachmentUrl);
        }

        btnContinue.setEnabled(false);
        btnContinue.setText(R.string.saving);

        if (isEditMode) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.save_changes_title)
                    .setMessage(R.string.save_changes_message)
                    .setPositiveButton(R.string.save, (dialog, which) -> performSave(transaction))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> setContinueStateIdle())
                    .show();
        } else {
            performSave(transaction);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}