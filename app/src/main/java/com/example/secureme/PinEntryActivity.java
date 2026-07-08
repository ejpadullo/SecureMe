package com.example.secureme;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.util.concurrent.Executor;

import javax.crypto.SecretKey;

public class PinEntryActivity extends AppCompatActivity {

    private EditText pinEditText;
    private Button btnVerifyPin;
    private TextView forgotPin;

    private BiometricPrompt biometricPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_entry);

        pinEditText = findViewById(R.id.pinEditText);
        btnVerifyPin = findViewById(R.id.btnVerifyPin);
        forgotPin = findViewById(R.id.forgotPin);

        btnVerifyPin.setOnClickListener(v -> verifyPin());
        forgotPin.setOnClickListener(v -> launchBiometricOrDeviceCredential());
    }

    private void verifyPin() {
        String enteredPin = pinEditText.getText().toString().trim();

        if (TextUtils.isEmpty(enteredPin) || enteredPin.length() != 6) {
            Toast.makeText(this, "PIN must be exactly 6 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        LoadingDialogHelper.show(this, "Verifying PIN...");

        String hashedInput = hashPin(enteredPin);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    LoadingDialogHelper.hide();
                    String storedHashedPin = document.getString("pin");

                    if (storedHashedPin != null && storedHashedPin.equals(hashedInput)) {
                        try {
                            SecretKey key = AESHelper.getAESKeyFromPin(enteredPin);
                            AESKeyCache.setKey(key);  // cache the AES key

                            Toast.makeText(this, "PIN correct!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(PinEntryActivity.this, VaultActivity.class));
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(this, "Error generating key: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    LoadingDialogHelper.hide();
                    Toast.makeText(this, "Failed to verify PIN.", Toast.LENGTH_SHORT).show();
                });
    }

    private void launchBiometricOrDeviceCredential() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authStatus = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (authStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            authenticateWithBiometricOrDevice();
        } else if (authStatus == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                authStatus == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ||
                authStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            Toast.makeText(this, "Please set up a device PIN, pattern, or password.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Authentication not available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void authenticateWithBiometricOrDevice() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(PinEntryActivity.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(PinEntryActivity.this, "Authentication Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PinEntryActivity.this, PinResetActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(PinEntryActivity.this, "Authentication Failed. Try again.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(PinEntryActivity.this, "Error: " + errString, Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock SecureMe")
                .setDescription("Use your fingerprint, PIN, pattern, or password")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes());
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
