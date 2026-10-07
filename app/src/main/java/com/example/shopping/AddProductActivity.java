package com.example.shopping;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    private EditText etName, etPrice, etDescription, etStock, etSizes, etColors;
    private Spinner spinnerCategory;
    private ImageView ivProduct;
    private RecyclerView rvSelected;
    private FirebaseFirestore db;
    private List<Uri> selectedUris = new ArrayList<>();
    private SelectedImageAdapter imageAdapter;
    private List<String> uploadedUrls = new ArrayList<>();

    private final ActivityResultLauncher<String> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (selectedUris.size() < 4) {
                            selectedUris.add(uri);
                        }
                    }
                    imageAdapter.notifyDataSetChanged();
                    if (!selectedUris.isEmpty()) {
                        ivProduct.setImageURI(selectedUris.get(0));
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        db = FirebaseFirestore.getInstance();
        
        initToolbar();
        initViews();
        setupSpinner();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAddProduct);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etName = findViewById(R.id.etAddProductName);
        etPrice = findViewById(R.id.etAddProductPrice);
        etDescription = findViewById(R.id.etAddProductDescription);
        etStock = findViewById(R.id.etAddProductStock);
        etSizes = findViewById(R.id.etAddProductSizes);
        etColors = findViewById(R.id.etAddProductColors);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        ivProduct = findViewById(R.id.ivSelectedProduct);
        rvSelected = findViewById(R.id.rvSelectedImages);
        Button btnSave = findViewById(R.id.btnAddProduct);

        imageAdapter = new SelectedImageAdapter(selectedUris);
        rvSelected.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelected.setAdapter(imageAdapter);

        findViewById(R.id.cvAddImage).setOnClickListener(v -> {
            pickerLauncher.launch("image/*");
        });

        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadImagesAndSaveProduct(0);
            }
        });
    }

    private void setupSpinner() {
        String[] categories = getResources().getStringArray(R.array.categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
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
        if (selectedUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadImagesAndSaveProduct(int index) {
        if (index >= selectedUris.size()) {
            saveProductToFirestore();
            return;
        }

        Toast.makeText(this, "Uploading image " + (index + 1) + " of " + selectedUris.size(), Toast.LENGTH_SHORT).show();
        
        MediaManager.get().upload(selectedUris.get(index))
                .option("unsigned", true)
                .option("upload_preset", "shopping app")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        uploadedUrls.add(imageUrl);
                        uploadImagesAndSaveProduct(index + 1);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AddProductActivity.this, "Upload failed at " + (index+1), Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveProductToFirestore() {
        String name = etName.getText().toString().trim();
        double price = Double.parseDouble(etPrice.getText().toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        long stock = Long.parseLong(etStock.getText().toString().trim());
        
        List<String> sizes = new ArrayList<>();
        String sizeInput = etSizes.getText().toString().trim();
        if (!sizeInput.isEmpty()) {
            for (String s : sizeInput.split(",")) sizes.add(s.trim());
        }

        List<String> colors = new ArrayList<>();
        String colorInput = etColors.getText().toString().trim();
        if (!colorInput.isEmpty()) {
            for (String c : colorInput.split(",")) colors.add(c.trim());
        }

        String mainImage = uploadedUrls.get(0);
        List<String> extraImages = new ArrayList<>();
        if (uploadedUrls.size() > 1) {
            extraImages.addAll(uploadedUrls.subList(1, uploadedUrls.size()));
        }

        java.util.Map<String, Object> productMap = new java.util.HashMap<>();
        productMap.put("name", name);
        productMap.put("description", description);
        productMap.put("price", price);
        productMap.put("imageUrl", mainImage);
        productMap.put("category", category);
        productMap.put("stock", stock);
        productMap.put("extraImages", extraImages);
        productMap.put("availableSizes", sizes);
        productMap.put("availableColors", colors);
        productMap.put("timestamp", System.currentTimeMillis());
        productMap.put("rating", 0.0);
        productMap.put("reviewCount", 0L);

        db.collection("products")
                .add(productMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Product Added Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}