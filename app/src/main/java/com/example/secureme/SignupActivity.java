package com.example.secureme;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText signupEmail, confirmEmail, signupPassword, confirmPassword;
    private Button btnSignup;
    private TextView goToLogin;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        signupEmail = findViewById(R.id.signupEmail);
        confirmEmail = findViewById(R.id.confirmEmail);
        signupPassword = findViewById(R.id.signupPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        goToLogin = findViewById(R.id.goToLogin);

        signupPassword.setOnTouchListener(getVisibilityToggleListener(signupPassword));
        confirmPassword.setOnTouchListener(getVisibilityToggleListener(confirmPassword));

        btnSignup.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String confirmEmailText = confirmEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String confirmPasswordText = confirmPassword.getText().toString().trim();

            if (email.isEmpty() || confirmEmailText.isEmpty() || password.isEmpty() || confirmPasswordText.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!email.equals(confirmEmailText)) {
                Toast.makeText(SignupActivity.this, "Emails do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPasswordText)) {
                Toast.makeText(SignupActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading dialog while creating the account
            LoadingDialogHelper.show(SignupActivity.this, "Creating account...");

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Create user document in Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("created_at", System.currentTimeMillis());

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.getUid())
                                    .set(userData, SetOptions.merge())        // ← creates or merges here
                                    .addOnSuccessListener(aVoid -> {
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(task -> {
                                                    // Hide loading after email verification request
                                                    LoadingDialogHelper.hide();

                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(SignupActivity.this,
                                                                "Account created! Verification email sent.",
                                                                Toast.LENGTH_LONG).show();
                                                    } else {
                                                        Toast.makeText(SignupActivity.this,
                                                                "Failed to send verification email.",
                                                                Toast.LENGTH_SHORT).show();
                                                    }

                                                    auth.signOut();
                                                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                                    finish();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Hide loading on failure
                                        LoadingDialogHelper.hide();
                                        Toast.makeText(SignupActivity.this,
                                                "Failed to create user document: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Hide loading on failure
                        LoadingDialogHelper.hide();
                        Toast.makeText(SignupActivity.this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        });
    }

    private EditText.OnTouchListener getVisibilityToggleListener(EditText field) {
        return (v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (field.getRight() - field.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (isPasswordVisible) {
                        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        field.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_visibility_off, 0);
                        isPasswordVisible = false;
                    } else {
                        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        field.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0);
                        isPasswordVisible = true;
                    }
                    field.setSelection(field.getText().length());
                    return true;
                }
            }
            return false;
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Hide the loading dialog if the activity is stopped
        LoadingDialogHelper.hide();
    }
}
