package com.example.shopping;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener, CategoryAdapter.OnCategoryClickListener {

    private ViewPager2 viewPagerSlider;
    private RecyclerView rvCategories, rvProducts, rvHistory, rvRecentlyViewed;
    private BottomNavigationView bottomNavigation;
    private EditText etSearch;
    private ProductAdapter productAdapter, recentAdapter;
    private FirebaseFirestore db;
    private List<Product> productList = new ArrayList<>(), recentList = new ArrayList<>();
    private SearchHistoryManager historyManager;
    private View cvHistory, llRecentHeader;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerView;
    private TextView tvNotifBadge;
    private ListenerRegistration productListener;

    private final android.os.Handler sliderHandler = new android.os.Handler();
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerSlider != null && viewPagerSlider.getAdapter() != null && viewPagerSlider.getAdapter().getItemCount() > 0) {
                int nextItem = viewPagerSlider.getCurrentItem() + 1;
                if (nextItem >= viewPagerSlider.getAdapter().getItemCount()) {
                    nextItem = 0;
                }
                viewPagerSlider.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 3000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            db = FirebaseFirestore.getInstance();
            historyManager = new SearchHistoryManager(this);

            String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
            CartManager.getInstance().setUserEmail(email);
            WishlistManager.getInstance().setUserEmail(email);

            initViews();
            setupSlider();
            setupCategories();
            setupProducts();
            setupBottomNavigation();
            setupSearch();
            setupHistory();
            checkLoginRequests();
            listenForBroadcasts();
            checkConnectivity();

            if (findViewById(R.id.mainLayout) != null) {
                findViewById(R.id.mainLayout).setOnClickListener(v -> {
                    if (etSearch != null) etSearch.clearFocus();
                    if (cvHistory != null) cvHistory.setVisibility(View.GONE);
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error starting dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productListener != null) productListener.remove();
    }

    private void initViews() {
        viewPagerSlider = findViewById(R.id.viewPagerSlider);
        rvCategories = findViewById(R.id.rvCategories);
        rvProducts = findViewById(R.id.rvProducts);
        rvRecentlyViewed = findViewById(R.id.rvRecentlyViewed);
        llRecentHeader = findViewById(R.id.llRecentlyViewed);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        etSearch = findViewById(R.id.etSearch);
        rvHistory = findViewById(R.id.rvHistory);
        cvHistory = findViewById(R.id.cvHistory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        shimmerView = findViewById(R.id.shimmerView);
        tvNotifBadge = findViewById(R.id.tvNotifBadge);

        findViewById(R.id.ivSort).setOnClickListener(v -> showSortMenu());
        findViewById(R.id.ivFilter).setOnClickListener(v -> showFilterSheet());
        findViewById(R.id.ivNotifications).setOnClickListener(v -> {
            if (tvNotifBadge != null) tvNotifBadge.setVisibility(View.GONE);
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putLong("notifLastCleared", System.currentTimeMillis()).apply();
            startActivity(new Intent(this, NotificationInboxActivity.class));
        });

        // Initialize Adapters
        productAdapter = new ProductAdapter(productList, this);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setAdapter(productAdapter);

        recentAdapter = new ProductAdapter(recentList, this);
        rvRecentlyViewed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecentlyViewed.setAdapter(recentAdapter);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.purple_500);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                setupSlider();
                setupProducts();
                new android.os.Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }
        
        viewPagerSlider.setPageTransformer(new ViewPager2.PageTransformer() {
            private static final float MIN_SCALE = 0.75f;
            @Override
            public void transformPage(View view, float position) {
                int pageWidth = view.getWidth();
                if (position < -1) { view.setAlpha(0f); }
                else if (position <= 0) { view.setAlpha(1f); view.setTranslationX(0f); view.setScaleX(1f); view.setScaleY(1f); }
                else if (position <= 1) {
                    view.setAlpha(1 - position);
                    view.setTranslationX(pageWidth * -position);
                    float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                    view.setScaleX(scaleFactor);
                    view.setScaleY(scaleFactor);
                } else { view.setAlpha(0f); }
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (productAdapter != null) productAdapter.getFilter().filter(s);
                if (s.length() > 0) cvHistory.setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etSearch.getText().length() == 0) showHistory();
            else cvHistory.setVisibility(View.GONE);
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                historyManager.saveSearch(query);
                etSearch.clearFocus();
                cvHistory.setVisibility(View.GONE);
                return true;
            }
            return false;
        });
    }

    private void setupHistory() {
        findViewById(R.id.tvClearHistory).setOnClickListener(v -> {
            historyManager.clearHistory();
            cvHistory.setVisibility(View.GONE);
        });
    }

    private void showHistory() {
        List<String> history = historyManager.getHistory();
        if (history.isEmpty()) {
            cvHistory.setVisibility(View.GONE);
            return;
        }

        SearchHistoryAdapter historyAdapter = new SearchHistoryAdapter(history, query -> {
            etSearch.setText(query);
            etSearch.setSelection(query.length());
            cvHistory.setVisibility(View.GONE);
            if (productAdapter != null) {
                productAdapter.getFilter().filter(query);
            }
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);
        cvHistory.setVisibility(View.VISIBLE);
    }

    private void checkLoginRequests() {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        if (!"admin@gmail.com".equalsIgnoreCase(email)) return;

        db.collection("login_requests")
                .whereEqualTo("status", "Pending")
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) showApprovalDialog(doc);
                    }
                });
    }

    private void listenForBroadcasts() {
        long startupTime = System.currentTimeMillis();
        long lastCleared = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("notifLastCleared", startupTime);

        db.collection("broadcasts")
                .whereGreaterThan("timestamp", lastCleared)
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        if (tvNotifBadge != null) {
                            tvNotifBadge.setText(String.valueOf(value.size()));
                            tvNotifBadge.setVisibility(View.VISIBLE);
                        }
                        for (QueryDocumentSnapshot doc : value) {
                            String title = doc.getString("title");
                            String msg = doc.getString("message");
                            showBroadcastDialog(title, msg);
                        }
                    }
                });
    }

    private void showBroadcastDialog(String title, String msg) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void checkConnectivity() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("Please check your network settings to use Shopping Hub.")
                    .setCancelable(false)
                    .setPositiveButton("Retry", (dialog, which) -> {
                        checkConnectivity();
                        setupSlider();
                        setupProducts();
                    })
                    .show();
        }
    }

    private void showApprovalDialog(QueryDocumentSnapshot requestDoc) {
        String devId = requestDoc.getString("deviceID");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Login Request")
                .setMessage("A new device is trying to log in as admin. Approve this login?")
                .setCancelable(false)
                .setPositiveButton("Approve", (dialog, which) -> {
                    db.collection("login_requests").document(requestDoc.getId()).update("status", "Approved");
                    db.collection("admin_config").document("authorized_devices")
                            .update("ids", com.google.firebase.firestore.FieldValue.arrayUnion(devId));
                })
                .setNegativeButton("Deny", (dialog, which) -> db.collection("login_requests").document(requestDoc.getId()).delete())
                .show();
    }

    private void setupSlider() {
        List<String> banners = new ArrayList<>();
        SliderAdapter sliderAdapter = new SliderAdapter(banners);
        viewPagerSlider.setAdapter(sliderAdapter);

        db.collection("banners").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isFinishing() || isDestroyed()) return;
            banners.clear();
            List<DocumentSnapshot> docs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
            
            docs.sort((d1, d2) -> {
                Long t1 = d1.getLong("timestamp");
                Long t2 = d2.getLong("timestamp");
                if (t1 == null) t1 = 0L;
                if (t2 == null) t2 = 0L;
                return t2.compareTo(t1);
            });

            for (DocumentSnapshot doc : docs) {
                String url = doc.getString("imageUrl");
                if (url != null && !url.isEmpty()) banners.add(url);
            }

            if (banners.isEmpty()) banners.add("https://via.placeholder.com/800x400.png?text=Welcome+to+Shopping+Hub");
            sliderAdapter.notifyDataSetChanged();
        });
    }

    private void setupCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("All", R.drawable.ic_home, R.color.cat_all));
        
        String[] categoryNames = getResources().getStringArray(R.array.categories);
        int[] categoryIcons = {R.drawable.ic_fashion, R.drawable.ic_electronics, R.drawable.ic_grocery, R.drawable.ic_beauty, R.drawable.ic_other_cat};
        int[] categoryColors = {R.color.cat_fashion, R.color.cat_electronics, R.color.cat_grocery, R.color.cat_beauty, R.color.cat_other};

        for (int i = 0; i < categoryNames.length; i++) {
            int icon = (i < categoryIcons.length) ? categoryIcons[i] : R.drawable.ic_other_cat;
            int color = (i < categoryColors.length) ? categoryColors[i] : R.color.cat_all;
            categories.add(new Category(categoryNames[i], icon, color));
        }

        CategoryAdapter categoryAdapter = new CategoryAdapter(categories, this);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupProducts() {
        if (shimmerView != null) {
            shimmerView.startShimmer();
            shimmerView.setVisibility(View.VISIBLE);
            rvProducts.setVisibility(View.GONE);
        }

        if (productListener != null) productListener.remove();

        // Remove orderBy to ensure all items are fetched, even if timestamp is missing
        productListener = db.collection("products")
                .addSnapshotListener((value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (value != null) {
                        parseProducts(value.getDocuments());
                        loadRecentlyViewed();
                    } else if (error != null) {
                        Toast.makeText(this, "Load Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRecentlyViewed() {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("userEmail", "");
        if (email.isEmpty()) return;

        db.collection("users").document(email).get().addOnSuccessListener(doc -> {
            List<String> ids = (List<String>) doc.get("recentlyViewed");
            if (ids != null && !ids.isEmpty()) {
                final List<String> lastIds = new ArrayList<>(ids);
                java.util.Collections.reverse(lastIds);
                final List<String> limitedIds = lastIds.size() > 10 ? lastIds.subList(0, 10) : lastIds;

                db.collection("products").whereIn(com.google.firebase.firestore.FieldPath.documentId(), limitedIds).get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            recentList.clear();
                            List<DocumentSnapshot> sortedDocs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                            sortedDocs.sort((d1, d2) -> Integer.compare(limitedIds.indexOf(d1.getId()), limitedIds.indexOf(d2.getId())));
                            
                            for (DocumentSnapshot d : sortedDocs) {
                                recentList.add(parseSingleProduct(d));
                            }
                            if (!recentList.isEmpty()) {
                                llRecentHeader.setVisibility(View.VISIBLE);
                                recentAdapter.updateList(recentList);
                            }
                        });
            }
        });
    }

    private Product parseSingleProduct(DocumentSnapshot doc) {
        String id = doc.getId();
        String name = doc.getString("name");
        String desc = doc.getString("description");
        String category = doc.getString("category");
        String imageUrl = doc.getString("imageUrl");

        double price = 0.0;
        Object pObj = doc.get("price");
        if (pObj instanceof Number) price = ((Number) pObj).doubleValue();

        long stock = 0;
        Object sObj = doc.get("stock");
        if (sObj instanceof Number) stock = ((Number) sObj).longValue();

        Long ts = doc.getLong("timestamp");
        Double rating = doc.getDouble("rating");
        Long reviews = doc.getLong("reviewCount");

        Product product = new Product(id, name != null ? name : "Unnamed", desc != null ? desc : "", price, imageUrl != null ? imageUrl : "", category != null ? category : "Other");
        product.setStock(stock);
        if (rating != null) product.setRating(rating);
        if (reviews != null) product.setReviewCount(reviews);
        
        // Use rating field to store temporary timestamp for sorting in parseProducts
        if (ts != null) product.setRating(ts.doubleValue()); 

        List<String> extras = (List<String>) doc.get("extraImages");
        if (extras != null) product.setExtraImages(extras);
        return product;
    }

    private void parseProducts(List<DocumentSnapshot> documents) {
        try {
            List<Product> newProducts = new ArrayList<>();
            for (DocumentSnapshot doc : documents) {
                newProducts.add(parseSingleProduct(doc));
            }

            // Sort locally: Newest first
            newProducts.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));
            
            // Fix ratings for display (if we used rating field as temp timestamp)
            // Ideally we'd have a separate field or a wrapper class, but let's re-fetch from doc if needed or just accept it's a hack.
            // Better way: parse actual rating after sort
            for (int i = 0; i < newProducts.size(); i++) {
                for (DocumentSnapshot d : documents) {
                    if (d.getId().equals(newProducts.get(i).getId())) {
                        Double actualRating = d.getDouble("rating");
                        newProducts.get(i).setRating(actualRating != null ? actualRating : 0.0);
                        break;
                    }
                }
            }

            if (shimmerView != null) {
                shimmerView.stopShimmer();
                shimmerView.setVisibility(View.GONE);
                rvProducts.setVisibility(View.VISIBLE);
            }

            productAdapter.updateList(newProducts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            else if (id == R.id.nav_wishlist) { startActivity(new Intent(this, WishlistActivity.class)); return true; }
            else if (id == R.id.nav_cart) { startActivity(new Intent(this, CartActivity.class)); return true; }
            else if (id == R.id.nav_profile) { startActivity(new Intent(MainActivity.this, ProfileActivity.class)); return true; }
            return false;
        });
    }

    @Override public void onProductClick(Product product) { Intent intent = new Intent(this, ProductDetailActivity.class); intent.putExtra("product", product); startActivity(intent); }
    @Override public void onAddToCartClick(Product product) { CartManager.getInstance().addProduct(product); Toast.makeText(this, "Added to cart: " + product.getName(), Toast.LENGTH_SHORT).show(); }
    @Override public void onFavoriteClick(Product product) { WishlistManager.getInstance().toggleWishlist(product); Toast.makeText(this, WishlistManager.getInstance().isInWishlist(product) ? "Added to Wishlist" : "Removed from Wishlist", Toast.LENGTH_SHORT).show(); }
    @Override public void onCategoryClick(Category category) { if (productAdapter != null) productAdapter.filterByCategory(category.getName()); }

    private void showSortMenu() {
        String[] options = {"Price: Low to High", "Price: High to Low", "Top Rated", "A-Z"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Sort Products");
        builder.setItems(options, (dialog, which) -> {
            if (productAdapter != null) {
                switch (which) {
                    case 0: productList.sort((p1, o2) -> Double.compare(p1.getPrice(), o2.getPrice())); break;
                    case 1: productList.sort((p1, o2) -> Double.compare(o2.getPrice(), p1.getPrice())); break;
                    case 2: productList.sort((p1, o2) -> Double.compare(o2.getRating(), p1.getRating())); break;
                    case 3: productList.sort((p1, o2) -> p1.getName().compareToIgnoreCase(o2.getName())); break;
                }
                productAdapter.updateList(new ArrayList<>(productList));
            }
        });
        builder.show();
    }

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_filter, null);
        EditText etPrice = view.findViewById(R.id.etMaxPrice);
        android.widget.CheckBox cbRating = view.findViewById(R.id.cbHighRated);
        Button btnApply = view.findViewById(R.id.btnApplyFilter);
        btnApply.setOnClickListener(v -> {
            String priceStr = etPrice.getText().toString();
            double price = priceStr.isEmpty() ? 0 : Double.parseDouble(priceStr);
            if (productAdapter != null) productAdapter.setAdvancedFilter(price, cbRating.isChecked());
            dialog.dismiss();
        });
        dialog.setContentView(view);
        dialog.show();
    }
}