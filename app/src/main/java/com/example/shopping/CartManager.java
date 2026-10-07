package com.example.shopping;

import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static CartManager instance;
    private List<Product> cartItems;
    private String userEmail;

    private CartManager() {
        cartItems = new ArrayList<>();
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
        if (email != null && !email.isEmpty()) {
            loadFromCloud();
        }
    }

    public void addProduct(Product product) {
        boolean found = false;
        for (Product item : cartItems) {
            if (item.getId() != null && item.getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + 1);
                found = true;
                break;
            }
        }
        if (!found) {
            cartItems.add(product);
        }
        syncWithCloud();
    }

    public void removeProduct(Product product) {
        cartItems.removeIf(p -> p.getId() != null && p.getId().equals(product.getId()));
        syncWithCloud();
    }

    public void updateQuantity(Product product, int newQty) {
        for (Product item : cartItems) {
            if (item.getId() != null && item.getId().equals(product.getId())) {
                item.setQuantity(newQty);
                break;
            }
        }
        syncWithCloud();
    }

    public List<Product> getCartItems() {
        return cartItems;
    }

    public double getTotalPrice() {
        double total = 0;
        for (Product item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    public void clearCart() {
        cartItems.clear();
        syncWithCloud();
    }

    private void syncWithCloud() {
        if (userEmail == null || userEmail.isEmpty()) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userEmail)
                .update("cart", cartItems);
    }

    private void loadFromCloud() {
        if (userEmail == null || userEmail.isEmpty()) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Manual parsing to avoid custom class for now
                        List<java.util.Map<String, Object>> cloudCart = (List<java.util.Map<String, Object>>) documentSnapshot.get("cart");
                        if (cloudCart != null) {
                            cartItems.clear();
                            for (java.util.Map<String, Object> map : cloudCart) {
                                Product p = new Product();
                                p.setId((String) map.get("id"));
                                p.setName((String) map.get("name"));
                                p.setPrice(((Number) map.get("price")).doubleValue());
                                p.setQuantity(((Number) map.get("quantity")).intValue());
                                p.setImageUrl((String) map.get("imageUrl"));
                                cartItems.add(p);
                            }
                        }
                    }
                });
    }
}
