package com.example.shopping;

import java.util.ArrayList;
import java.util.List;

public class WishlistManager {
    private static WishlistManager instance;
    private List<Product> wishlistItems;
    private String userEmail;

    private WishlistManager() {
        wishlistItems = new ArrayList<>();
    }

    public static synchronized WishlistManager getInstance() {
        if (instance == null) {
            instance = new WishlistManager();
        }
        return instance;
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
        if (email != null && !email.isEmpty()) {
            loadFromCloud();
        }
    }

    public void toggleWishlist(Product product) {
        if (isInWishlist(product)) {
            wishlistItems.removeIf(p -> p.getId() != null && p.getId().equals(product.getId()));
        } else {
            wishlistItems.add(product);
        }
        syncWithCloud();
    }

    public boolean isInWishlist(Product product) {
        if (product.getId() == null) return false;
        for (Product item : wishlistItems) {
            if (item.getId() != null && item.getId().equals(product.getId())) {
                return true;
            }
        }
        return false;
    }

    public List<Product> getWishlistItems() {
        return wishlistItems;
    }

    private void syncWithCloud() {
        if (userEmail == null || userEmail.isEmpty()) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userEmail)
                .update("wishlist", wishlistItems);
    }

    private void loadFromCloud() {
        if (userEmail == null || userEmail.isEmpty()) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<java.util.Map<String, Object>> cloudWish = (List<java.util.Map<String, Object>>) documentSnapshot.get("wishlist");
                        if (cloudWish != null) {
                            wishlistItems.clear();
                            for (java.util.Map<String, Object> map : cloudWish) {
                                Product p = new Product();
                                p.setId((String) map.get("id"));
                                p.setName((String) map.get("name"));
                                p.setPrice(((Number) map.get("price")).doubleValue());
                                p.setImageUrl((String) map.get("imageUrl"));
                                wishlistItems.add(p);
                            }
                        }
                    }
                });
    }
}
