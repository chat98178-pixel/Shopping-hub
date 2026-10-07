package com.example.shopping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageProductsActivity extends AppCompatActivity implements ManageProductAdapter.OnProductManageListener {

    private RecyclerView rvProducts;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private List<Product> productList;
    private ManageProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_products);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadProducts();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarManageProducts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvProducts = findViewById(R.id.rvManageProducts);
        tvEmpty = findViewById(R.id.tvEmptyCatalog);
        productList = new ArrayList<>();
        adapter = new ManageProductAdapter(productList, this);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(adapter);
    }

    private void loadProducts() {
        db.collection("products")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        productList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Product product = doc.toObject(Product.class);
                                product.setId(doc.getId());
                                productList.add(product);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        if (productList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onEditClick(Product product) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("product", product);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Delete '" + product.getName() + "' from catalog?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProduct(product);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct(Product product) {
        db.collection("products")
                .document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product removed", Toast.LENGTH_SHORT).show();
                });
    }
}