package com.example.shopping;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private List<Review> reviewList;
    private ReviewAdapter reviewAdapter;
    private EditText etReview;
    private Product currentProduct;
    private ViewPager2 vpGallery;
    private TabLayout tlDots;
    private Uri reviewMediaUri;
    private boolean isVideoReview = false;
    private ImageView ivReviewPreview;
    private com.google.android.material.chip.ChipGroup cgSizes, cgColors;
    private String selectedSize, selectedColor;

    private final ActivityResultLauncher<Intent> mediaPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    reviewMediaUri = result.getData().getData();
                    ivReviewPreview.setVisibility(View.VISIBLE);
                    if (isVideoReview) {
                        ivReviewPreview.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        ivReviewPreview.setImageURI(reviewMediaUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        currentProduct = (Product) getIntent().getSerializableExtra("product");
        if (currentProduct == null) {
            finish();
            return;
        }

        initToolbar();
        initViews(currentProduct);
        setupReviews(currentProduct);
        checkVouchers();
        addToRecentlyViewed();
    }

    private void addToRecentlyViewed() {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        if (email.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("users").document(email)
                .update("recentlyViewed", FieldValue.arrayUnion(currentProduct.getId()));
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.product_detail_menu, menu);
        
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        boolean isAdmin = "admin@gmail.com".equalsIgnoreCase(email);
        
        menu.findItem(R.id.menu_edit).setVisible(isAdmin);
        menu.findItem(R.id.menu_delete).setVisible(isAdmin);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            shareProduct();
            return true;
        } else if (item.getItemId() == R.id.menu_edit) {
            Intent intent = new Intent(this, EditProductActivity.class);
            intent.putExtra("product", currentProduct);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_delete) {
            showDeleteConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProduct())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct() {
        if (currentProduct.getId() == null) return;
        
        FirebaseFirestore.getInstance().collection("products")
                .document(currentProduct.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void shareProduct() {
        String shareText = "Check out this " + currentProduct.getName() + " on Shopping Hub!\n\n" +
                "Price: Rs " + String.format("%.2f", currentProduct.getPrice()) + "\n" +
                "Category: " + currentProduct.getCategory() + "\n\n" +
                "Download the app now to buy!";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, currentProduct.getName());
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void initViews(Product product) {
        vpGallery = findViewById(R.id.vpProductGallery);
        tlDots = findViewById(R.id.tlDots);
        cgSizes = findViewById(R.id.cgSizes);
        cgColors = findViewById(R.id.cgColors);
        TextView tvSizeTitle = findViewById(R.id.tvSizeTitle);
        TextView tvColorTitle = findViewById(R.id.tvColorTitle);
        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvPrice = findViewById(R.id.tvDetailPrice);
        TextView tvDescription = findViewById(R.id.tvDetailDescription);
        TextView tvReviewsCount = findViewById(R.id.tvReviewCount);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        Button btnAddToCart = findViewById(R.id.btnAddToCartDetail);
        Button btnBuyNow = findViewById(R.id.btnBuyNow);
        etReview = findViewById(R.id.etReview);
        Button btnSubmitReview = findViewById(R.id.btnSubmitReview);
        ImageButton btnReviewMedia = findViewById(R.id.btnReviewPhoto);
        ivReviewPreview = findViewById(R.id.ivReviewPhotoPreview);
        com.google.android.material.floatingactionbutton.FloatingActionButton fabWhatsApp = findViewById(R.id.fabWhatsApp);
        rvReviews = findViewById(R.id.rvReviews);

        // Multi-Image Gallery
        List<String> images = new ArrayList<>();
        if (product.getImageUrl() != null) images.add(product.getImageUrl());
        if (product.getExtraImages() != null) images.addAll(product.getExtraImages());
        
        if (images.isEmpty()) {
            images.add(""); // Placeholder empty
        }

        ProductImageAdapter adapter = new ProductImageAdapter(images, url -> {
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putExtra("image_url", url);
            startActivity(intent);
        });
        vpGallery.setAdapter(adapter);
        new TabLayoutMediator(tlDots, vpGallery, (tab, position) -> {}).attach();

        if (tvName != null) tvName.setText(currentProduct.getName() != null ? currentProduct.getName() : "Unknown Product");
        if (tvPrice != null) tvPrice.setText("Rs " + String.format("%.2f", currentProduct.getPrice()));
        if (tvDescription != null) tvDescription.setText(currentProduct.getDescription() != null ? currentProduct.getDescription() : "");
        if (tvReviewsCount != null) tvReviewsCount.setText("(" + currentProduct.getReviewCount() + " reviews)");
        if (ratingBar != null) ratingBar.setRating((float) currentProduct.getRating());

        // Setup Variants
        setupVariants(tvSizeTitle, cgSizes, product.getAvailableSizes(), true);
        setupVariants(tvColorTitle, cgColors, product.getAvailableColors(), false);

        // Stock check
        if (currentProduct.getStock() <= 0) {
            if (btnAddToCart != null) {
                btnAddToCart.setEnabled(false);
                btnAddToCart.setText("Out of Stock");
            }
            if (btnBuyNow != null) {
                btnBuyNow.setEnabled(false);
                btnBuyNow.setText("Out of Stock");
            }
        }

        if (btnAddToCart != null && currentProduct.getStock() > 0) {
            btnAddToCart.setOnClickListener(v -> {
                if (validateSelection()) {
                    currentProduct.setSelectedSize(selectedSize);
                    currentProduct.setSelectedColor(selectedColor);
                    CartManager.getInstance().addProduct(currentProduct);
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                if (validateSelection()) {
                    currentProduct.setSelectedSize(selectedSize);
                    currentProduct.setSelectedColor(selectedColor);
                    Intent intent = new Intent(this, CheckoutActivity.class);
                    intent.putExtra("single_product", currentProduct);
                    startActivity(intent);
                }
            });
        }

        if (fabWhatsApp != null) {
            fabWhatsApp.setOnClickListener(v -> {
                String wpNumber = "9779800000000"; // Replace with your number
                String message = "Hi Shopping Hub! I am interested in: " + currentProduct.getName() + " (Rs " + currentProduct.getPrice() + ")";
                String url = "https://api.whatsapp.com/send?phone=" + wpNumber + "&text=" + Uri.encode(message);
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnReviewMedia.setOnClickListener(v -> showMediaPickerOptions());

        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> postReview(currentProduct));
        }

        if (ratingBar != null) {
            ratingBar.setOnRatingBarChangeListener((ratingBar1, rating, fromUser) -> {
                if (fromUser) {
                    updateRating(product, rating);
                }
            });
        }
    }

    private void showMediaPickerOptions() {
        String[] options = {"Pick Photo", "Pick Video (Max 10s)"};
        new AlertDialog.Builder(this)
                .setTitle("Add Media to Review")
                .setItems(options, (dialog, which) -> {
                    isVideoReview = (which == 1);
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType(isVideoReview ? "video/*" : "image/*");
                    mediaPicker.launch(intent);
                })
                .show();
    }

    private void checkVouchers() {
        FirebaseFirestore.getInstance().collection("coupons").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String code = doc.getString("code");
                        Double amt = doc.getDouble("discountAmount");
                        
                        View banner = findViewById(R.id.cvVoucherBanner);
                        TextView text = findViewById(R.id.tvVoucherText);
                        Button btn = findViewById(R.id.btnCollectVoucher);
                        
                        if (amt != null) {
                            text.setText("Collect Rs " + amt.intValue() + " OFF Voucher!");
                        }
                        banner.setVisibility(View.VISIBLE);
                        
                        btn.setOnClickListener(v -> {
                            String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
                            FirebaseFirestore.getInstance().collection("users").document(email)
                                    .update("collectedVouchers", FieldValue.arrayUnion(code))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Voucher Collected!", Toast.LENGTH_SHORT).show();
                                        banner.setVisibility(View.GONE);
                                    });
                        });
                    }
                });
    }

    private void setupReviews(Product product) {
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        if (product.getId() == null) return;

        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Review review = doc.toObject(Review.class);
                            reviewList.add(review);
                        }
                        reviewAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void postReview(Product product) {
        String comment = etReview.getText().toString().trim();
        if (TextUtils.isEmpty(comment)) {
            etReview.setError("Write something");
            return;
        }

        if (reviewMediaUri != null) {
            uploadReviewMedia(product, comment);
        } else {
            saveReviewToFirestore(product, comment, null, false);
        }
    }

    private void uploadReviewMedia(Product product, String comment) {
        Toast.makeText(this, "Uploading review media...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(reviewMediaUri)
                .option("unsigned", true)
                .option("upload_preset", "shopping app")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        saveReviewToFirestore(product, comment, url, isVideoReview);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(ProductDetailActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        saveReviewToFirestore(product, comment, null, false);
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveReviewToFirestore(Product product, String comment, String mediaUrl, boolean isVideo) {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String email = prefs.getString("userEmail", "User");
        
        Review review = new Review(email, comment, new Date().getTime());
        if (mediaUrl != null) {
            if (isVideo) {
                review.setVideoUrl(mediaUrl);
                review.setVideo(true);
            } else {
                review.setImageUrl(mediaUrl);
            }
        }

        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .collection("reviews")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    etReview.setText("");
                    reviewMediaUri = null;
                    ivReviewPreview.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Review posted!", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateRating(Product product, float newRating) {
        if (product.getId() == null) return;
        
        double currentRating = product.getRating();
        long count = product.getReviewCount();
        
        double finalRating = ((currentRating * count) + newRating) / (count + 1);
        long finalCount = count + 1;

        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", finalRating);
        updates.put("reviewCount", finalCount);

        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupVariants(TextView title, com.google.android.material.chip.ChipGroup group, List<String> variants, boolean isSize) {
        if (variants == null || variants.isEmpty()) {
            title.setVisibility(View.GONE);
            group.setVisibility(View.GONE);
            return;
        }

        title.setVisibility(View.VISIBLE);
        group.setVisibility(View.VISIBLE);
        group.removeAllViews();

        for (String v : variants) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(v);
            chip.setCheckable(true);
            chip.setClickable(true);
            group.addView(chip);
        }

        group.setOnCheckedStateChangeListener((group1, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                com.google.android.material.chip.Chip chip = findViewById(checkedIds.get(0));
                if (isSize) selectedSize = chip.getText().toString();
                else selectedColor = chip.getText().toString();
            }
        });
    }

    private boolean validateSelection() {
        if (currentProduct.getAvailableSizes() != null && !currentProduct.getAvailableSizes().isEmpty() && selectedSize == null) {
            Toast.makeText(this, "Please select a Size", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (currentProduct.getAvailableColors() != null && !currentProduct.getAvailableColors().isEmpty() && selectedColor == null) {
            Toast.makeText(this, "Please select a Color", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}