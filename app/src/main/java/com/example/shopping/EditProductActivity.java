package com.example.shopping;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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

import java.util.HashMap;
import java.util.Map;

public class EditProductActivity extends AppCompatActivity {

    private EditText etName, etPrice, etDescription, etStock;
    private Spinner spinnerCategory;
    private ImageView ivProduct;
    private FirebaseFirestore db;
    private Uri imageUri;
    private Product product;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivProduct.setImageURI(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        initToolbar();
        initViews();
        setupSpinner();
        fillData();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAddProduct);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Product");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etName = findViewById(R.id.etAddProductName);
        etPrice = findViewById(R.id.etAddProductPrice);
        etDescription = findViewById(R.id.etAddProductDescription);
        etStock = findViewById(R.id.etAddProductStock);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        ivProduct = findViewById(R.id.ivSelectedProduct);
        Button btnSave = findViewById(R.id.btnAddProduct);
        btnSave.setText("Update Product");

        findViewById(R.id.cvAddImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                if (imageUri != null) {
                    uploadImageAndUpdateProduct();
                } else {
                    updateProduct(product.getImageUrl());
                }
            }
        });
    }

    private void fillData() {
        etName.setText(product.getName());
        etPrice.setText(String.valueOf(product.getPrice()));
        etDescription.setText(product.getDescription());
        etStock.setText(String.valueOf(product.getStock()));
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(this).load(product.getImageUrl()).into(ivProduct);
        }
    }

    private void setupSpinner() {
        String[] categories = getResources().getStringArray(R.array.categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        
        // Set selection
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(product.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etName.getText())) {
            etName.setError("Name required");
            return false;
        }
        if (TextUtils.isEmpty(etPrice.getText())) {
            etPrice.setError("Price required");
            return false;
        }
        if (TextUtils.isEmpty(etStock.getText())) {
            etStock.setError("Stock required");
            return false;
        }
        return true;
    }

    private void uploadImageAndUpdateProduct() {
        Toast.makeText(this, "Uploading to Cloudinary...", Toast.LENGTH_SHORT).show();
        
        MediaManager.get().upload(imageUri)
                .option("unsigned", true)
                .option("upload_preset", "shopping app")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        updateProduct(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(EditProductActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void updateProduct(String imageUrl) {
        String name = etName.getText().toString().trim();
        double price = Double.parseDouble(etPrice.getText().toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        long stock = Long.parseLong(etStock.getText().toString().trim());

        Map<String, Object> productMap = new HashMap<>();
        productMap.put("name", name);
        productMap.put("description", description);
        productMap.put("price", price);
        productMap.put("imageUrl", imageUrl);
        productMap.put("category", category);
        productMap.put("stock", stock);
        productMap.put("extraImages", product.getExtraImages());
        productMap.put("timestamp", System.currentTimeMillis());
        productMap.put("rating", product.getRating());
        productMap.put("reviewCount", product.getReviewCount());

        db.collection("products")
                .document(product.getId())
                .set(productMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}