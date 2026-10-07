package com.example.shopping;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        initToolbar();
        initViews();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarForgot);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etEmail = findViewById(R.id.etForgotEmail);
        Button btnReset = findViewById(R.id.btnForgotAction);
        
        // Update UI to be simpler for Email reset
        findViewById(R.id.etForgotPhone).setVisibility(android.view.View.GONE);
        btnReset.setText("Send Reset Link");

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email required");
                return;
            }
            
            sendResetEmail(email);
        });
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Reset link sent to your Gmail!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Failed to send email";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}