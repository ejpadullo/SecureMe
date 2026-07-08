package com.example.secureme;

import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

public class VaultEntryActivity extends AppCompatActivity {
    private EditText editTitle, editUsername, editPassword, editNotes;
    private Button btnSave, btnDelete, btnGenerate, btnCheckBreach;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String documentId = null;
    private boolean isPasswordVisible = false;

    private SecretKey encryptionKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault_entry);

        editTitle = findViewById(R.id.editTitle);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editNotes = findViewById(R.id.editNotes);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnGenerate = findViewById(R.id.btnGeneratePassword);
        btnCheckBreach = findViewById(R.id.btnCheckBreach);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Retrieve AES key from memory cache
        encryptionKey = AESKeyCache.getKey();
        if (encryptionKey == null) {
            Toast.makeText(this, "Security error: no key available.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editPassword.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0);
        editPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    event.getRawX() >= (editPassword.getRight() - editPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                isPasswordVisible = !isPasswordVisible;
                editPassword.setInputType(isPasswordVisible ?
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                editPassword.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_lock, 0,
                        isPasswordVisible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility, 0
                );
                editPassword.setSelection(editPassword.getText().length());
                return true;
            }
            return false;
        });

        if (getIntent().hasExtra("documentId")) {
            documentId = getIntent().getStringExtra("documentId");
            try {
                editTitle.setText(AESHelper.decrypt(getIntent().getStringExtra("title"), encryptionKey));
                editUsername.setText(AESHelper.decrypt(getIntent().getStringExtra("username"), encryptionKey));
                editPassword.setText(AESHelper.decrypt(getIntent().getStringExtra("password"), encryptionKey));
                editNotes.setText(AESHelper.decrypt(getIntent().getStringExtra("notes"), encryptionKey));
            } catch (Exception e) {
                Toast.makeText(this, "Failed to decrypt data.", Toast.LENGTH_LONG).show();
                e.printStackTrace();
                finish();
                return;
            }
            btnDelete.setVisibility(Button.VISIBLE);
        }

        btnSave.setOnClickListener(v -> saveOrUpdateEntry());

        btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Entry")
                    .setMessage("Are you sure you want to permanently delete this data?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteEntry())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnGenerate.setOnClickListener(v -> {
            String gen = BreachCheckUtil.generateSecurePassword(16);
            editPassword.setText(gen);
            Toast.makeText(this, "Password generated!", Toast.LENGTH_SHORT).show();

            BreachCheckUtil.checkPasswordBreach(gen, isBreached -> runOnUiThread(() -> {
                if (isBreached) {
                    Toast.makeText(this, "⚠️ Found in a breach — try another.", Toast.LENGTH_LONG).show();
                }
            }));
        });

        btnCheckBreach.setOnClickListener(v -> {
            String pass = editPassword.getText().toString().trim();
            if (pass.isEmpty()) {
                Toast.makeText(this, "Enter a password first.", Toast.LENGTH_SHORT).show();
                return;
            }

            BreachCheckUtil.checkPasswordBreach(pass, isBreached -> runOnUiThread(() -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(isBreached ? "Breached Password" : "Password Safe")
                        .setMessage(isBreached
                                ? "This password appears in a breach! Avoid using it."
                                : "Your password appears safe.")
                        .setPositiveButton("OK", null)
                        .setIcon(isBreached ? R.drawable.ic_warning_red : R.drawable.ic_check_green)
                        .show();
            }));
        });
    }

    private void saveOrUpdateEntry() {
        String title = editTitle.getText().toString().trim();
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        if (title.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Map<String, Object> encryptedData = new HashMap<>();
            encryptedData.put("title", AESHelper.encrypt(title, encryptionKey));
            encryptedData.put("username", AESHelper.encrypt(username, encryptionKey));
            encryptedData.put("password", AESHelper.encrypt(password, encryptionKey));
            encryptedData.put("notes", AESHelper.encrypt(notes, encryptionKey));
            encryptedData.put("timestamp", System.currentTimeMillis());

            if (documentId == null) {
                db.collection("users").document(uid).collection("vault")
                        .add(encryptedData)
                        .addOnSuccessListener(doc -> {
                            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                db.collection("users").document(uid).collection("vault").document(documentId)
                        .set(encryptedData)
                        .addOnSuccessListener(doc -> {
                            Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Update error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Encryption failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void deleteEntry() {
        if (documentId == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("vault").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}