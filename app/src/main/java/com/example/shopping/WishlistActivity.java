package com.example.shopping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WishlistActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private RecyclerView rvWishlist;
    private TextView tvEmptyWishlist;
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        initToolbar();
        initViews();
        setupWishlist();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarWishlist);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvWishlist = findViewById(R.id.rvWishlist);
        tvEmptyWishlist = findViewById(R.id.tvEmptyWishlist);
    }

    private void setupWishlist() {
        List<Product> wishlist = WishlistManager.getInstance().getWishlistItems();
        if (wishlist.isEmpty()) {
            tvEmptyWishlist.setVisibility(View.VISIBLE);
        } else {
            tvEmptyWishlist.setVisibility(View.GONE);
        }

        adapter = new ProductAdapter(wishlist, this);
        rvWishlist.setLayoutManager(new GridLayoutManager(this, 2));
        rvWishlist.setAdapter(adapter);
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product", product);
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        CartManager.getInstance().addProduct(product);
        Toast.makeText(this, "Added to cart: " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFavoriteClick(Product product) {
        WishlistManager.getInstance().toggleWishlist(product);
        setupWishlist(); // Refresh the list
    }
}