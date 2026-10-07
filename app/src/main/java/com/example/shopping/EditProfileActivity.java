package com.example.shopping;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        userEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");

        initToolbar();
        initViews();
        loadCurrentData();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarEditProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etName = findViewById(R.id.etEditName);
        etPhone = findViewById(R.id.etEditPhone);
        Button btnUpdate = findViewById(R.id.btnUpdateProfile);

        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Name required");
                return;
            }
            if (phone.length() != 10) {
                etPhone.setError("10 digits required");
                return;
            }

            updateProfile(name, "+977" + phone);
        });
    }

    private void loadCurrentData() {
        if (userEmail.isEmpty()) return;

        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        etName.setText(user.getFullName());
                        String phone = user.getPhoneNumber();
                        if (phone != null && phone.startsWith("+977")) {
                            etPhone.setText(phone.replace("+977", ""));
                        }
                    }
                });
    }

    private void updateProfile(String name, String phone) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phoneNumber", phone);

        db.collection("users").document(userEmail)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}