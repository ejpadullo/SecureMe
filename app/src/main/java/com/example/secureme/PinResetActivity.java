package com.example.secureme;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;

import javax.crypto.SecretKey;

public class PinResetActivity extends AppCompatActivity {
    private EditText newPin, confirmPin;
    private Button btnReset;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_reset);

        newPin = findViewById(R.id.newPin);
        confirmPin = findViewById(R.id.confirmPin);
        btnReset = findViewById(R.id.btnSavePin);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        btnReset.setOnClickListener(v -> attemptResetPin());
    }

    private void attemptResetPin() {
        String pin1 = newPin.getText().toString().trim();
        String pin2 = confirmPin.getText().toString().trim();

        if (TextUtils.isEmpty(pin1) || TextUtils.isEmpty(pin2)) {
            Toast.makeText(this, "Please enter and confirm your new PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pin1.length() != 6) {
            Toast.makeText(this, "PIN must be exactly 6 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pin1.equals(pin2)) {
            Toast.makeText(this, "PINs do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uid == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Reset PIN & Wipe Vault")
                .setMessage("Resetting your PIN will permanently delete all saved vault data.\n\nAre you absolutely sure you want to continue?")
                .setPositiveButton("Yes, Reset & Wipe", (dialog, which) -> resetPinAndWipeVault(pin1))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetPinAndWipeVault(String newPin) {
        LoadingDialogHelper.show(this, "Resetting PIN and wiping vault...");

        // Step 1: Delete user's vault collection
        CollectionReference vaultRef = db.collection("users").document(uid).collection("vault");

        vaultRef.get().addOnSuccessListener(querySnapshot -> {
            for (var doc : querySnapshot.getDocuments()) {
                doc.getReference().delete();
            }

            // Step 2: Save new PIN hash
            String hashedPin = hashPin(newPin);
            db.collection("users").document(uid)
                    .update("pin", hashedPin)
                    .addOnSuccessListener(aVoid -> {
                        try {
                            SecretKey newKey = AESHelper.getAESKeyFromPin(newPin);
                            AESKeyCache.setKey(newKey);
                        } catch (Exception e) {
                            LoadingDialogHelper.hide();
                            Toast.makeText(this, "Error deriving key", Toast.LENGTH_LONG).show();
                            return;
                        }

                        LoadingDialogHelper.hide();
                        Toast.makeText(this, "PIN reset & vault wiped.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PinResetActivity.this, VaultActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        LoadingDialogHelper.hide();
                        Toast.makeText(this, "Failed to update PIN: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            LoadingDialogHelper.hide();
            Toast.makeText(this, "Failed to access vault data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LoadingDialogHelper.hide();
    }
}