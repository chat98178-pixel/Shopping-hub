package com.example.shopping;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.UUID;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvGems;
    private ImageView ivProfile;
    private androidx.appcompat.widget.SwitchCompat switchDarkMode;
    private FirebaseFirestore db;
    private Uri imageUri;
    private String currentProfileUrl;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    uploadProfileImage();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadUserData();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvGems = findViewById(R.id.tvHubGems);
        ivProfile = findViewById(R.id.ivProfilePic);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        Button btnAddressBook = findViewById(R.id.btnSavedAddress);
        Button btnMyOrders = findViewById(R.id.btnMyOrders);
        Button btnAdmin = findViewById(R.id.btnAdminPanel);
        Button btnLogout = findViewById(R.id.btnLogoutProfile);

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        btnAddressBook.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, ManageAddressActivity.class));
        });

        findViewById(R.id.cvProfileImage).setOnClickListener(v -> showProfileOptions());

        btnMyOrders.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, OrderHistoryActivity.class));
        });

        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, AdminHubActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            // Clear session and logout
            getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        setupThemeSwitch();
    }

    private void setupThemeSwitch() {
        boolean isDark = getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("isDarkMode", false);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((v, isChecked) -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putBoolean("isDarkMode", isChecked).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isChecked ? 
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : 
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void showProfileOptions() {
        String[] options = {"View Photo", "Change Photo"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (currentProfileUrl != null && !currentProfileUrl.isEmpty()) {
                            Intent intent = new Intent(this, FullScreenImageActivity.class);
                            intent.putExtra("image_url", currentProfileUrl);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "No profile photo set", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        imagePickerLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadProfileImage() {
        if (imageUri == null) return;
        Toast.makeText(this, "Uploading to Cloudinary...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(imageUri)
                .option("unsigned", true)
                .option("upload_preset", "shopping app") // Updated with your Preset Name
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        updateUserImageUrl(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(ProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void updateUserImageUrl(String imageUrl) {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        db.collection("users")
                .document(email)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    // Update UI immediately
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(ivProfile);
                });
    }

    private void loadUserData() {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        if (email.isEmpty()) {
            Toast.makeText(this, "Session error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users")
                .document(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (task.isSuccessful() && task.getResult() != null) {
                        try {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) {
                                tvName.setText(user.getFullName());
                                tvEmail.setText(user.getEmail());
                                tvPhone.setText(user.getPhoneNumber());
                                currentProfileUrl = user.getProfileImageUrl();

                                if (tvGems != null) {
                                    tvGems.setText("Hub Gems: " + user.getHubGems() + " 💎");
                                }
                                
                                // Load profile image
                                if (currentProfileUrl != null && !currentProfileUrl.isEmpty()) {
                                    ivProfile.setColorFilter(null);
                                    Glide.with(this)
                                            .load(currentProfileUrl)
                                            .placeholder(R.drawable.ic_person)
                                            .into(ivProfile);
                                } else {
                                    ivProfile.setImageResource(R.drawable.ic_person);
                                    ivProfile.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.purple_500));
                                }

                                if (user.isAdmin()) {
                                    findViewById(R.id.btnAdminPanel).setVisibility(View.VISIBLE);
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Profile Data Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}