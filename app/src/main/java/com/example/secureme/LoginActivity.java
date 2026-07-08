package com.example.secureme;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText loginEmail, loginPassword;
    private Button btnLogin;
    private TextView goToSignup, forgotPassword;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        goToSignup = findViewById(R.id.goToSignup);
        forgotPassword = findViewById(R.id.forgotPassword);

        // Default password is hidden + visibility icon shown
        loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        loginPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock, 0, R.drawable.ic_visibility_off, 0
        );

        // Password visibility toggle
        loginPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (loginPassword.getRight()
                        - loginPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {

                    if (isPasswordVisible) {
                        // Hide password
                        loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        loginPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_visibility_off, 0
                        );
                    } else {
                        // Show password
                        loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        loginPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0
                        );
                    }

                    isPasswordVisible = !isPasswordVisible;
                    loginPassword.setSelection(loginPassword.getText().length());
                    return true;
                }
            }
            return false;
        });

        // Login button
        btnLogin.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading dialog while logging in
            LoadingDialogHelper.show(LoginActivity.this, "Logging in...");

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            // Hide loading after login success
                            LoadingDialogHelper.hide();

                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            // Check if PIN exists in Firestore
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(document -> {
                                        if (document.contains("pin")) {
                                            startActivity(new Intent(LoginActivity.this, PinEntryActivity.class));
                                        } else {
                                            startActivity(new Intent(LoginActivity.this, PinSetupActivity.class));
                                        }
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        LoadingDialogHelper.hide();
                                        Toast.makeText(LoginActivity.this, "Failed to fetch PIN info.", Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            LoadingDialogHelper.hide();
                            Toast.makeText(LoginActivity.this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                            auth.signOut();
                        }
                    })
                    .addOnFailureListener(e -> {
                        LoadingDialogHelper.hide();
                        Toast.makeText(LoginActivity.this, "Login failed: Email or password is incorrect.", Toast.LENGTH_SHORT).show();
                    });
        });

        // Go to signup
        goToSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        // Forgot password
        forgotPassword.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email first.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to send reset email: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        LoadingDialogHelper.hide();
    }
}