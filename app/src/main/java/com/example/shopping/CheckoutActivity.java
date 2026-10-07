package com.example.shopping;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private EditText etName, etAddress, etCity, etCoupon;
    private TextView tvSubtotal, tvDeliveryFee, tvTotal, tvCouponMsg;
    private android.widget.CheckBox cbPoints;
    private RadioGroup rgPayment;
    private FirebaseFirestore db;
    private Product singleProduct;
    private double currentDiscount = 0.0;
    private double orderSubtotal = 0.0;
    private double deliveryFee = 70.0;
    private int userGems = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        singleProduct = (Product) getIntent().getSerializableExtra("single_product");

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadSavedAddressAndVouchers();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarCheckout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etName = findViewById(R.id.etCheckoutName);
        etAddress = findViewById(R.id.etCheckoutAddress);
        etCity = findViewById(R.id.etCheckoutCity);
        etCoupon = findViewById(R.id.etCouponCode);
        tvSubtotal = findViewById(R.id.tvCheckoutSubtotal);
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee);
        tvTotal = findViewById(R.id.tvCheckoutTotal);
        tvCouponMsg = findViewById(R.id.tvCouponApplied);
        cbPoints = findViewById(R.id.cbUsePoints);
        rgPayment = findViewById(R.id.rgPayment);
        Button btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        Button btnApply = findViewById(R.id.btnApplyCoupon);

        orderSubtotal = (singleProduct != null) ? (singleProduct.getPrice() * singleProduct.getQuantity()) : CartManager.getInstance().getTotalPrice();
        updatePricing();

        cbPoints.setOnCheckedChangeListener((v, isChecked) -> updatePricing());

        btnApply.setOnClickListener(v -> {
            String code = etCoupon.getText().toString().trim();
            if (!code.isEmpty()) validateCoupon(code);
        });

        btnPlaceOrder.setOnClickListener(v -> {
            if (validateInputs()) {
                double pointsToUse = cbPoints.isChecked() ? userGems : 0;
                double gemsDiscount = pointsToUse / 10.0;
                double finalTotal = orderSubtotal + deliveryFee - currentDiscount - gemsDiscount;
                placeOrder(Math.max(0, finalTotal), deliveryFee, (int) pointsToUse);
            }
        });
    }

    private void updatePricing() {
        double pointsToUse = cbPoints.isChecked() ? userGems : 0;
        double gemsDiscount = pointsToUse / 10.0;
        double total = orderSubtotal + deliveryFee - currentDiscount - gemsDiscount;
        
        tvSubtotal.setText("Rs " + String.format(Locale.getDefault(), "%.2f", orderSubtotal));
        tvDeliveryFee.setText("Rs " + String.format(Locale.getDefault(), "%.2f", deliveryFee));
        tvTotal.setText("Rs " + String.format(Locale.getDefault(), "%.2f", Math.max(0, total)));
        
        if (currentDiscount > 0) {
            tvCouponMsg.setText("Coupon applied: -Rs " + String.format(Locale.getDefault(), "%.2f", currentDiscount));
            tvCouponMsg.setVisibility(View.VISIBLE);
        } else {
            tvCouponMsg.setVisibility(View.GONE);
        }
    }

    private void validateCoupon(String code) {
        db.collection("coupons").whereEqualTo("code", code.toUpperCase()).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Coupon coupon = doc.toObject(Coupon.class);
                            if (orderSubtotal >= coupon.getMinOrderValue()) {
                                currentDiscount = coupon.getDiscountAmount();
                                updatePricing();
                                Toast.makeText(this, "Coupon Applied!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Minimum order for this coupon is Rs " + coupon.getMinOrderValue(), Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Invalid Coupon Code", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSavedAddressAndVouchers() {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        db.collection("users").document(email).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        if (user.getFullName() != null) etName.setText(user.getFullName());
                        if (user.getSavedAddress() != null) etAddress.setText(user.getSavedAddress());
                        if (user.getSavedCity() != null) etCity.setText(user.getSavedCity());

                        userGems = user.getHubGems();
                        if (userGems > 0) {
                            cbPoints.setText("Use Hub Gems (Balance: " + userGems + " 💎)");
                            cbPoints.setVisibility(View.VISIBLE);
                        }

                        // Auto-Apply Collected Vouchers
                        if (user.getCollectedVouchers() != null && !user.getCollectedVouchers().isEmpty()) {
                            autoApplyBestVoucher(user.getCollectedVouchers());
                        }
                    }
                });
    }

    private void autoApplyBestVoucher(List<String> codes) {
        db.collection("coupons").whereIn("code", codes).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double bestDiscount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Coupon c = doc.toObject(Coupon.class);
                        if (orderSubtotal >= c.getMinOrderValue()) {
                            if (c.getDiscountAmount() > bestDiscount) {
                                bestDiscount = c.getDiscountAmount();
                            }
                        }
                    }
                    if (bestDiscount > 0) {
                        currentDiscount = bestDiscount;
                        updatePricing();
                        Toast.makeText(this, "Best Voucher Applied Automatically!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etName.getText())) {
            etName.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(etAddress.getText())) {
            etAddress.setError("Address is required");
            return false;
        }
        if (TextUtils.isEmpty(etCity.getText())) {
            etCity.setError("City is required");
            return false;
        }
        return true;
    }

    private void placeOrder(double total, double deliveryFee, int gemsUsed) {
        String userEmail = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        List<Product> items;
        if (singleProduct != null) {
            items = new ArrayList<>();
            items.add(singleProduct);
        } else {
            items = CartManager.getInstance().getCartItems();
        }
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        
        int selectedId = rgPayment.getCheckedRadioButtonId();
        RadioButton selectedButton = findViewById(selectedId);
        String paymentMethod = (selectedButton != null) ? selectedButton.getText().toString() : "COD";

        // Calculate Gems to Earn: (Total / 10) + 220
        int rewardGems = (int) (total / 10) + 220;

        Order order = new Order(userEmail, items, total, new Date().getTime(), name, address, city, paymentMethod, deliveryFee);
        order.setGemsEarned(rewardGems);

        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    // Update Stock & Grant Gems
                    db.runTransaction(transaction -> {
                        com.google.firebase.firestore.DocumentReference userRef = db.collection("users").document(userEmail);
                        DocumentSnapshot userSnap = transaction.get(userRef);
                        long currentGems = 0;
                        if (userSnap.exists() && userSnap.contains("hubGems")) {
                            currentGems = userSnap.getLong("hubGems");
                        }
                        transaction.update(userRef, "hubGems", currentGems - gemsUsed + rewardGems);
                        
                        for (Product item : items) {
                            if (item.getId() != null) {
                                transaction.update(db.collection("products").document(item.getId()), 
                                        "stock", com.google.firebase.firestore.FieldValue.increment(-item.getQuantity()));
                            }
                        }
                        return null;
                    });
                    
                    Toast.makeText(this, "Order Placed! Earned " + rewardGems + " Hub Gems! 💎", Toast.LENGTH_LONG).show();
                    CartManager.getInstance().clearCart();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error placing order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}