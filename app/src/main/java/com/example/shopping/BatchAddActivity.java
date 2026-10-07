package com.example.shopping;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BatchAddActivity extends AppCompatActivity {

    private RecyclerView rvBatch;
    private ProgressBar pbBatch;
    private TextView tvStatus;
    private Button btnSelect, btnProcess;
    private List<BatchProduct> batchList;
    private BatchProductAdapter adapter;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    batchList.clear();
                    // Limit to 10
                    int count = 0;
                    for (Uri uri : uris) {
                        if (count >= 10) break;
                        batchList.add(new BatchProduct(uri));
                        count++;
                    }
                    adapter.notifyDataSetChanged();
                    tvStatus.setText("Selected " + batchList.size() + " items. Fill details above.");
                    tvStatus.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_add);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarBatch);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvBatch = findViewById(R.id.rvBatchProducts);
        pbBatch = findViewById(R.id.pbBatch);
        tvStatus = findViewById(R.id.tvBatchStatus);
        btnSelect = findViewById(R.id.btnSelectMultiple);
        btnProcess = findViewById(R.id.btnProcessAll);

        batchList = new ArrayList<>();
        adapter = new BatchProductAdapter(batchList);
        rvBatch.setLayoutManager(new LinearLayoutManager(this));
        rvBatch.setAdapter(adapter);

        btnSelect.setOnClickListener(v -> pickerLauncher.launch("image/*"));

        btnProcess.setOnClickListener(v -> {
            if (validateAll()) {
                startBatchProcessing(0);
            }
        });
    }

    private boolean validateAll() {
        if (batchList.isEmpty()) {
            Toast.makeText(this, "Select at least 1 image", Toast.LENGTH_SHORT).show();
            return false;
        }
        for (BatchProduct p : batchList) {
            if (TextUtils.isEmpty(p.getName()) || TextUtils.isEmpty(p.getPrice())) {
                Toast.makeText(this, "Please fill Name and Price for all items", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void startBatchProcessing(int index) {
        if (index >= batchList.size()) {
            pbBatch.setVisibility(View.GONE);
            tvStatus.setText("All products processed successfully!");
            Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show();
            // Go back to Hub after a small delay
            new android.os.Handler().postDelayed(this::finish, 1500);
            return;
        }

        BatchProduct product = batchList.get(index);
        pbBatch.setVisibility(View.VISIBLE);
        pbBatch.setMax(batchList.size());
        pbBatch.setProgress(index + 1);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Uploading item " + (index + 1) + " of " + batchList.size() + "...");

        // Upload to Cloudinary
        MediaManager.get().upload(product.getImageUri())
                .option("unsigned", true)
                .option("upload_preset", "shopping app")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveToFirestore(product, imageUrl, index);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(BatchAddActivity.this, "Error at item " + (index + 1) + ": " + error.getDescription(), Toast.LENGTH_LONG).show();
                        pbBatch.setVisibility(View.GONE);
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirestore(BatchProduct bp, String imageUrl, int nextIndex) {
        Product product = new Product(null, bp.getName(), bp.getDescription(), 
                Double.parseDouble(bp.getPrice()), imageUrl, bp.getCategory());
        
        try {
            product.setStock(Long.parseLong(bp.getStock()));
        } catch (Exception e) {
            product.setStock(10); // Default if invalid
        }

        java.util.Map<String, Object> productMap = new java.util.HashMap<>();
        productMap.put("name", bp.getName());
        productMap.put("description", bp.getDescription());
        productMap.put("price", Double.parseDouble(bp.getPrice()));
        productMap.put("imageUrl", imageUrl);
        productMap.put("category", bp.getCategory());
        productMap.put("stock", product.getStock());
        productMap.put("timestamp", System.currentTimeMillis());
        productMap.put("rating", 0.0);
        productMap.put("reviewCount", 0L);

        db.collection("products")
                .add(productMap)
                .addOnSuccessListener(doc -> {
                    startBatchProcessing(nextIndex + 1);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}