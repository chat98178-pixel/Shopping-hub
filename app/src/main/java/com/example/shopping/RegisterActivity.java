package com.example.shopping;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etRegEmail, etPhoneNumber, etRegPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        try {
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            etFullName = findViewById(R.id.etFullName);
            etRegEmail = findViewById(R.id.etRegEmail);
            etPhoneNumber = findViewById(R.id.etPhoneNumber);
            etRegPassword = findViewById(R.id.etRegPassword);
            etConfirmPassword = findViewById(R.id.etConfirmPassword);
            btnRegister = findViewById(R.id.btnRegister);
            tvLogin = findViewById(R.id.tvLogin);

            btnRegister.setOnClickListener(v -> {
                String fullName = etFullName.getText().toString().trim();
                String email = etRegEmail.getText().toString().trim();
                String phoneNumber = etPhoneNumber.getText().toString().trim();
                String password = etRegPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(fullName)) {
                    etFullName.setError(getString(R.string.full_name_required));
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    etRegEmail.setError(getString(R.string.email_required));
                    return;
                }

                if (!email.toLowerCase().endsWith("@gmail.com")) {
                    etRegEmail.setError(getString(R.string.gmail_required_error));
                    return;
                }

                if (TextUtils.isEmpty(phoneNumber)) {
                    etPhoneNumber.setError(getString(R.string.phone_required));
                    return;
                }

                if (phoneNumber.length() != 10) {
                    etPhoneNumber.setError(getString(R.string.invalid_phone_error));
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    etRegPassword.setError(getString(R.string.password_required));
                    return;
                }

                if (password.length() < 8) {
                    etRegPassword.setError(getString(R.string.password_too_short));
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    etConfirmPassword.setError(getString(R.string.passwords_do_not_match));
                    return;
                }

                registerUser(fullName, email, phoneNumber, password);
            });

            tvLogin.setOnClickListener(v -> finish());
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void registerUser(String fullName, String email, String phoneNumber, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(fullName, email, phoneNumber);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(RegisterActivity.this, "Security Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String fullName, String email, String phoneNumber) {
        boolean isAdmin = email.equalsIgnoreCase("admin@gmail.com");
        String finalPhoneNumber = "+977" + phoneNumber;
        User user = new User(fullName, email, finalPhoneNumber, isAdmin);
        db.collection("users")
                .document(email) 
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    getSharedPreferences("UserSession", MODE_PRIVATE)
                            .edit()
                            .putString("userEmail", email)
                            .apply();

                    Toast.makeText(RegisterActivity.this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}