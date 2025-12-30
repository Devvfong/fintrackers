package com.example.fintracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText etAmount;
    private AutoCompleteTextView actvCategory, actvWallet;
    private TextInputEditText etDescription;
    private MaterialButton btnContinue;
    private ImageButton btnBack;
    private LinearLayout layoutAttachment, rootLayout;
    private TextView tvTitle;
    private SwitchMaterial switchRepeat;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri imageUri;
    private String attachmentUrl = null;
    private boolean isExpense = true;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Initialize views
        etAmount = findViewById(R.id.etAmount);
        actvCategory = findViewById(R.id.actvCategory);
        actvWallet = findViewById(R.id.actvWallet);
        etDescription = findViewById(R.id.etDescription);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        layoutAttachment = findViewById(R.id.layoutAttachment);
        tvTitle = findViewById(R.id.tvTitle);
        rootLayout = findViewById(R.id.rootLayout);
        switchRepeat = findViewById(R.id.switchRepeat);

        // Setup activity result launchers
        setupActivityResultLaunchers();

        // Setup category dropdown
        String[] categories = {"Shopping", "Subscription", "Food", "Transport", "Salary", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(categoryAdapter);

        // Setup wallet dropdown
        String[] wallets = {"Cash", "Bank Account", "Credit Card", "E-Wallet"};
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, wallets);
        actvWallet.setAdapter(walletAdapter);

        // Amount input formatting
        setupAmountInput();

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Attachment click
        if (layoutAttachment != null) {
            layoutAttachment.setOnClickListener(v -> showAttachmentOptions());
        }

        // Continue button
        btnContinue.setOnClickListener(v -> saveTransaction());
    }

    private void setupAmountInput() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && !s.toString().startsWith("$")) {
                    etAmount.setText("$" + s.toString().replace("$", ""));
                    etAmount.setSelection(etAmount.getText().length());
                }
            }
        });
    }

    private void setupActivityResultLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            uploadImageToFirebase(imageBitmap);
                        }
                    }
                }
        );

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageUriToFirebase(imageUri);
                        }
                    }
                }
        );
    }

    private void showAttachmentOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Attachment")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else if (which == 1) {
                        checkStoragePermissionAndOpen();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermissionAndOpen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                openGallery();
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void uploadImageToFirebase(Bitmap bitmap) {
        btnContinue.setEnabled(false);
        btnContinue.setText("Uploading...");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        String userId = mAuth.getCurrentUser().getUid();
        String fileName = "receipts/" + userId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    attachmentUrl = uri.toString();
                    Toast.makeText(this, "✅ Attachment uploaded!", Toast.LENGTH_SHORT).show();
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                });
    }

    private void uploadImageUriToFirebase(Uri uri) {
        btnContinue.setEnabled(false);
        btnContinue.setText("Uploading...");

        String userId = mAuth.getCurrentUser().getUid();
        String fileName = "receipts/" + userId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    attachmentUrl = downloadUri.toString();
                    Toast.makeText(this, "✅ Attachment uploaded!", Toast.LENGTH_SHORT).show();
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim().replace("$", "").replace(",", "");
        String category = actvCategory.getText().toString().trim();
        String wallet = actvWallet.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(amountStr) || amountStr.equals("0")) {
            Toast.makeText(this, "⚠️ Please enter amount", Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "⚠️ Please select category", Toast.LENGTH_SHORT).show();
            actvCategory.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(wallet)) {
            Toast.makeText(this, "⚠️ Please select wallet", Toast.LENGTH_SHORT).show();
            actvWallet.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "⚠️ Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
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
        transaction.put("isRepeated", switchRepeat.isChecked());

        // Add attachment URL if exists
        if (attachmentUrl != null) {
            transaction.put("attachmentUrl", attachmentUrl);
        }

        // Save to Firestore
        btnContinue.setEnabled(false);
        btnContinue.setText("Saving...");

        db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "✅ Transaction added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                });
    }
}
