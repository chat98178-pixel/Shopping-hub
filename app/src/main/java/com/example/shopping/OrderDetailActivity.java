package com.example.shopping;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();
        Order order = (Order) getIntent().getSerializableExtra("order");
        if (order == null) {
            finish();
            return;
        }

        initToolbar();
        initViews(order);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarInvoice);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews(Order order) {
        TextView tvId = findViewById(R.id.tvInvOrderId);
        TextView tvName = findViewById(R.id.tvInvName);
        TextView tvAddress = findViewById(R.id.tvInvAddress);
        TextView tvPayment = findViewById(R.id.tvInvPayment);
        TextView tvSubtotal = findViewById(R.id.tvInvSubtotal);
        TextView tvDelivery = findViewById(R.id.tvInvDelivery);
        TextView tvTotal = findViewById(R.id.tvInvTotal);
        Button btnCancel = findViewById(R.id.btnCancelOrderDetails);
        LinearLayout llItems = findViewById(R.id.llInvItems);
        LinearLayout llTimeline = findViewById(R.id.llTimeline);

        tvId.setText("Order #" + (order.getOrderId() != null ? order.getOrderId().substring(0, 8).toUpperCase() : "N/A"));
        tvName.setText("Name: " + order.getShippingName());
        tvAddress.setText("Address: " + order.getShippingAddress() + ", " + order.getShippingCity());
        tvPayment.setText("Payment: " + order.getPaymentMethod());

        double subtotal = order.getTotalAmount() - order.getDeliveryCharge();
        tvSubtotal.setText("Rs " + String.format(Locale.getDefault(), "%.2f", subtotal));
        tvDelivery.setText("Rs " + String.format(Locale.getDefault(), "%.2f", order.getDeliveryCharge()));
        tvTotal.setText("Rs " + String.format(Locale.getDefault(), "%.2f", order.getTotalAmount()));

        setupTimeline(llTimeline, order.getStatus());

        if ("Pending".equalsIgnoreCase(order.getStatus())) {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> cancelOrder(order));
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        for (Product product : order.getItems()) {
            TextView tv = new TextView(this);
            tv.setText(product.getName() + " (x" + product.getQuantity() + ") - Rs " + String.format("%.2f", product.getPrice() * product.getQuantity()));
            tv.setPadding(0, 8, 0, 8);
            tv.setTextColor(getResources().getColor(R.color.black));
            tv.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductDetailActivity.class);
                intent.putExtra("product", product);
                startActivity(intent);
            });
            llItems.addView(tv);
        }
    }

    private void setupTimeline(LinearLayout container, String status) {
        String[] steps = {"Ordered", "Packed", "Shipped", "Delivered"};
        if ("Cancelled".equalsIgnoreCase(status)) {
            steps = new String[]{"Ordered", "Cancelled"};
        }

        int currentStep = 0;
        if ("Packed".equalsIgnoreCase(status)) currentStep = 1;
        else if ("Shipped".equalsIgnoreCase(status)) currentStep = 2;
        else if ("Delivered".equalsIgnoreCase(status)) currentStep = 3;
        else if ("Cancelled".equalsIgnoreCase(status)) currentStep = 1;

        for (int i = 0; i < steps.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_timeline, container, false);
            View dot = view.findViewById(R.id.vTimelineDot);
            TextView label = view.findViewById(R.id.tvTimelineLabel);

            label.setText(steps[i]);

            if (i <= currentStep) {
                int color = ("Cancelled".equalsIgnoreCase(status) && i == 1) ? Color.RED : Color.parseColor("#4CAF50");
                dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                label.setTextColor(color);
            }

            container.addView(view);
        }
    }

    private void cancelOrder(Order order) {
        String[] reasons = {
                "Changed my mind",
                "Found a better price elsewhere",
                "Delivery time is too long",
                "Ordered by mistake",
                "Duplicate order",
                "Other"
        };

        new AlertDialog.Builder(this)
                .setTitle("Select Reason for Cancellation")
                .setItems(reasons, (dialog, which) -> {
                    String selectedReason = reasons[which];
                    processCancellation(order, selectedReason);
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void processCancellation(Order order, String reason) {
        db.collection("orders")
                .document(order.getOrderId())
                .update("status", "Cancelled", "cancelReason", reason)
                .addOnSuccessListener(aVoid -> {
                    // Restore Stock & Deduct Gems
                    db.runTransaction(transaction -> {
                        com.google.firebase.firestore.DocumentReference userRef = db.collection("users").document(order.getUserEmail());
                        DocumentSnapshot userSnap = transaction.get(userRef);

                        long currentGems = 0;
                        if (userSnap.exists() && userSnap.contains("hubGems")) {
                            currentGems = userSnap.getLong("hubGems");
                        }

                        // Deduct gems earned from this order
                        transaction.update(userRef, "hubGems", Math.max(0, currentGems - order.getGemsEarned()));

                        for (Product item : order.getItems()) {
                            if (item.getId() != null) {
                                transaction.update(db.collection("products").document(item.getId()),
                                        "stock", com.google.firebase.firestore.FieldValue.increment(item.getQuantity()));
                            }
                        }
                        return null;
                    });

                    Toast.makeText(this, "Order Cancelled. Gems deducted & stock restored.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}