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
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Check for existing session
            String savedEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
            if (!savedEmail.isEmpty()) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
                return;
            }

            setContentView(R.layout.login);

            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            etEmail = findViewById(R.id.etEmail);
            etPassword = findViewById(R.id.etPassword);
            btnLogin = findViewById(R.id.btnLogin);
            tvRegister = findViewById(R.id.tvRegister);

            btnLogin.setOnClickListener(v -> {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    etEmail.setError(getString(R.string.email_required));
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    etPassword.setError(getString(R.string.password_required));
                    return;
                }

                loginUser(email, password);
            });

            tvRegister.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });

            findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            });
        } catch (Exception e) {
            Toast.makeText(this, "Startup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if ("admin@gmail.com".equalsIgnoreCase(email)) {
                            checkAdminAuthorization(email);
                        } else {
                            proceedToLogin(email);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAdminAuthorization(String email) {
        String currentDeviceID = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("deviceID", "");
        
        db.collection("admin_config").document("authorized_devices").get()
                .addOnSuccessListener(doc -> {
                    java.util.List<String> devices = (java.util.List<String>) doc.get("ids");
                    
                    if (devices == null || devices.isEmpty()) {
                        // First time setup - authorize this device automatically as primary
                        java.util.List<String> initialList = new java.util.ArrayList<>();
                        initialList.add(currentDeviceID);
                        db.collection("admin_config").document("authorized_devices")
                                .set(new java.util.HashMap<String, Object>() {{ put("ids", initialList); }});
                        proceedToLogin(email);
                    } else if (devices.contains(currentDeviceID)) {
                        proceedToLogin(email);
                    } else {
                        // Unauthorized device - create request
                        requestAdminApproval(email, currentDeviceID);
                    }
                });
    }

    private void requestAdminApproval(String email, String deviceID) {
        androidx.appcompat.app.AlertDialog progressDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Security Check")
                .setMessage("Waiting for approval from Primary Admin device...")
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> {
                    db.collection("login_requests").document(deviceID).delete();
                    d.dismiss();
                })
                .show();

        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("email", email);
        request.put("deviceID", deviceID);
        request.put("status", "Pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("login_requests").document(deviceID).set(request);

        // Listen for approval
        db.collection("login_requests").document(deviceID)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        if ("Approved".equalsIgnoreCase(status)) {
                            progressDialog.dismiss();
                            // Cleanup
                            db.collection("login_requests").document(deviceID).delete();
                            proceedToLogin(email);
                        }
                    }
                });
    }

    private void proceedToLogin(String email) {
        CartManager.getInstance().setUserEmail(email);
        WishlistManager.getInstance().setUserEmail(email);
        
        getSharedPreferences("UserSession", MODE_PRIVATE)
                .edit()
                .putString("userEmail", email)
                .apply();

        Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}