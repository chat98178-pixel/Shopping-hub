package com.example.shopping;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private TextView tvRevenue, tvOrders, tvProducts;
    private BarChart barChart;
    private FirebaseFirestore db;
    private List<Order> allOrders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadAnalytics();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAnalytics);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        tvRevenue = findViewById(R.id.tvTotalRevenue);
        tvOrders = findViewById(R.id.tvTotalOrders);
        tvProducts = findViewById(R.id.tvTotalProducts);
        barChart = findViewById(R.id.salesChart);
        
        findViewById(R.id.btnExportCSV).setOnClickListener(v -> exportToCSV());
    }

    private void loadAnalytics() {
        // Use SnapshotListener for real-time updates
        db.collection("orders")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        double totalRevenue = 0;
                        int validOrderCount = 0;
                        Map<Integer, Double> daySales = new HashMap<>();
                        allOrders.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            order.setOrderId(doc.getId());
                            allOrders.add(order);

                            String status = doc.getString("status");
                            if ("Cancelled".equalsIgnoreCase(status)) continue;

                            validOrderCount++;

                            double amount = 0.0;
                            Object amountObj = doc.get("totalAmount");
                            if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();

                            Long ts = doc.getLong("timestamp");
                            totalRevenue += amount;

                            if (ts != null) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(ts);
                                int day = cal.get(Calendar.DAY_OF_WEEK);
                                daySales.put(day, daySales.getOrDefault(day, 0.0) + amount);
                            }
                        }

                        tvRevenue.setText("Rs " + String.format(Locale.getDefault(), "%.2f", totalRevenue));
                        tvOrders.setText(String.valueOf(validOrderCount));
                        setupChart(daySales);
                    }
                });

        // Fetch Product Count (One-time or real-time)
        db.collection("products").addSnapshotListener((value, error) -> {
            if (value != null) {
                tvProducts.setText(String.valueOf(value.size()));
            }
        });
    }

    private void setupChart(Map<Integer, Double> daySales) {
        List<BarEntry> entries = new ArrayList<>();
        // Simple 1-7 mapping (Sun-Sat)
        for (int i = 1; i <= 7; i++) {
            float val = daySales.getOrDefault(i, 0.0).floatValue();
            entries.add(new BarEntry(i, val));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Sales (Rs)");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(android.graphics.Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void exportToCSV() {
        if (allOrders.isEmpty()) {
            Toast.makeText(this, "No orders to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder("OrderID,Date,User,Amount,Status,Payment\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (Order o : allOrders) {
            String id = o.getOrderId() != null ? o.getOrderId() : "N/A";
            if (id.length() > 8) id = id.substring(0, 8);
            
            csv.append(id).append(",")
                    .append(sdf.format(new Date(o.getTimestamp()))).append(",")
                    .append(o.getUserEmail()).append(",")
                    .append(o.getTotalAmount()).append(",")
                    .append(o.getStatus()).append(",")
                    .append(o.getPaymentMethod()).append("\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Shopping Hub Sales Report");
        intent.putExtra(Intent.EXTRA_TEXT, csv.toString());
        startActivity(Intent.createChooser(intent, "Share CSV Report"));
    }
}