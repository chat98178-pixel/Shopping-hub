package com.example.shopping;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView rvAdminOrders;
    private FirebaseFirestore db;
    private List<Order> orderList;
    private AdminOrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadAllOrders();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAdminOrders);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvAdminOrders = findViewById(R.id.rvAdminOrders);
        orderList = new ArrayList<>();
        adapter = new AdminOrderAdapter(orderList, (order, newStatus) -> {
            updateOrderStatus(order, newStatus);
        });
        rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));
        rvAdminOrders.setAdapter(adapter);
    }

    private void loadAllOrders() {
        db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        orderList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            order.setOrderId(doc.getId());
                            orderList.add(order);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void updateOrderStatus(Order order, String newStatus) {
        db.collection("orders")
                .document(order.getOrderId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                });
    }
}