package com.example.shopping;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.Button;

public class AdminHubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_hub);

        initToolbar();
        initViews();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAdminHub);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        Button btnAddProduct = findViewById(R.id.btnAddProductHub);
        Button btnBatchAdd = findViewById(R.id.btnBatchAddHub);
        Button btnManageCatalog = findViewById(R.id.btnManageCatalogHub);
        Button btnAnalytics = findViewById(R.id.btnAnalyticsHub);
        Button btnManageOrders = findViewById(R.id.btnManageOrdersHub);
        Button btnManageBanners = findViewById(R.id.btnManageBannersHub);
        Button btnBroadcast = findViewById(R.id.btnBroadcastHub);

        btnAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(this, AddProductActivity.class));
        });

        btnBatchAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, BatchAddActivity.class));
        });

        btnManageCatalog.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageProductsActivity.class));
        });

        btnAnalytics.setOnClickListener(v -> {
            startActivity(new Intent(this, AnalyticsActivity.class));
        });

        btnManageOrders.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminOrdersActivity.class));
        });

        btnManageBanners.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageBannersActivity.class));
        });

        btnBroadcast.setOnClickListener(v -> {
            startActivity(new Intent(this, BroadcastActivity.class));
        });

        findViewById(R.id.btnManageCouponsHub).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageCouponsActivity.class));
        });

        findViewById(R.id.btnInventoryAlertsHub).setOnClickListener(v -> {
            startActivity(new Intent(this, LowStockActivity.class));
        });
    }
}
