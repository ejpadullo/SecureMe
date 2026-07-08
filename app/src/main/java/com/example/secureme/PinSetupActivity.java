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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

public class PinSetupActivity extends AppCompatActivity {
    private EditText newPin, confirmPin;
    private Button btnSetPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        newPin = findViewById(R.id.newPin);
        confirmPin = findViewById(R.id.confirmPin);
        btnSetPin = findViewById(R.id.btnSetPin);

        btnSetPin.setOnClickListener(v -> {
            String pin1 = newPin.getText().toString().trim();
            String pin2 = confirmPin.getText().toString().trim();

            if (TextUtils.isEmpty(pin1) || TextUtils.isEmpty(pin2)) {
                Toast.makeText(this, "Please enter and confirm your PIN.", Toast.LENGTH_SHORT).show();
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

            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Important: Remember Your PIN")
                    .setMessage("Your PIN is used to encrypt your data. If you forget it, you won’t be able to access your vault again. There is NO recovery method.\n\nAre you sure you want to continue?")
                    .setCancelable(false)
                    .setPositiveButton("Yes, I Understand", (dialog, which) -> storePin(pin1))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void storePin(String pin) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPin;
        try {
            hashedPin = hashPin(pin);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error hashing PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        LoadingDialogHelper.show(this, "Saving PIN...");

        Map<String, Object> pinData = new HashMap<>();
        pinData.put("pin", hashedPin);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(pinData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    try {
                        SecretKey key = AESHelper.getAESKeyFromPin(pin);
                        AESKeyCache.setKey(key);
                    } catch (Exception e) {
                        LoadingDialogHelper.hide();
                        Toast.makeText(this, "Failed to derive encryption key.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                        return;
                    }

                    LoadingDialogHelper.hide();
                    Toast.makeText(this, "PIN set successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(PinSetupActivity.this, VaultActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    LoadingDialogHelper.hide();
                    Toast.makeText(this, "Failed to store PIN: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String hashPin(String pin) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(pin.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LoadingDialogHelper.hide();
    }
}