package com.example.shopping;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageBannersActivity extends AppCompatActivity {

    private RecyclerView rvBanners;
    private FirebaseFirestore db;
    private List<Banner> bannerList;
    private BannerAdapter adapter;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    uploadBanner(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_banners);

        db = FirebaseFirestore.getInstance();

        initToolbar();
        initViews();
        loadBanners();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarManageBanners);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvBanners = findViewById(R.id.rvBanners);
        Button btnAdd = findViewById(R.id.btnAddBanner);

        bannerList = new ArrayList<>();
        adapter = new BannerAdapter(bannerList, banner -> {
            showDeleteConfirmation(banner);
        });
        rvBanners.setLayoutManager(new LinearLayoutManager(this));
        rvBanners.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
    }

    private void showDeleteConfirmation(Banner banner) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Banner")
                .setMessage("Are you sure you want to remove this banner?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBanner(banner))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBanner(Banner banner) {
        if (banner.getId() == null) return;
        
        db.collection("banners")
                .document(banner.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Banner deleted", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBanners() {
        db.collection("banners")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        bannerList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Banner banner = doc.toObject(Banner.class);
                            banner.setId(doc.getId());
                            bannerList.add(banner);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void uploadBanner(Uri uri) {
        Toast.makeText(this, "Uploading banner to Cloudinary...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(uri)
                .option("unsigned", true)
                .option("upload_preset", "shopping app")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveBannerToFirestore(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(ManageBannersActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveBannerToFirestore(String url) {
        Map<String, Object> banner = new HashMap<>();
        banner.put("imageUrl", url);
        banner.put("timestamp", new Date().getTime());

        db.collection("banners")
                .add(banner)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Banner added", Toast.LENGTH_SHORT).show();
                });
    }
}