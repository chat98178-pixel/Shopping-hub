package com.example.shopping;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrderHistory;
    private TextView tvEmptyHistory;
    private FirebaseFirestore db;
    private List<Order> orderList;
    private OrderHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadOrders();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        orderList = new ArrayList<>();
        adapter = new OrderHistoryAdapter(orderList, new OrderHistoryAdapter.OnOrderActionListener() {
            @Override
            public void onCancelClick(Order order) {
                cancelOrder(order);
            }

            @Override
            public void onReorderClick(Order order) {
                reorderItems(order);
            }
        });
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        rvOrderHistory.setAdapter(adapter);
    }

    private void reorderItems(Order order) {
        for (Product item : order.getItems()) {
            CartManager.getInstance().addProduct(item);
        }
        Toast.makeText(this, "Items added to cart!", Toast.LENGTH_SHORT).show();
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOrders() {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        if (email.isEmpty()) {
            finish();
            return;
        }

        db.collection("orders")
                .whereEqualTo("userEmail", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        orderList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = document.toObject(Order.class);
                            order.setOrderId(document.getId());
                            orderList.add(order);
                        }
                        
                        // Sort locally (Newest first) to avoid Index requirement
                        orderList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                        if (orderList.isEmpty()) {
                            tvEmptyHistory.setVisibility(View.VISIBLE);
                        } else {
                            tvEmptyHistory.setVisibility(View.GONE);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "History Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}