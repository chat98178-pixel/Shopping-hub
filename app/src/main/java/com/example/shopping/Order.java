package com.example.shopping;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    private String orderId;
    private String userEmail;
    private List<Product> items;
    private double totalAmount;
    private long timestamp;
    private String shippingName;
    private String shippingAddress;
    private String shippingCity;
    private String paymentMethod;
    private String status; // Pending, Shipped, Delivered, Cancelled
    private double deliveryCharge;
    private String cancelReason;
    private int gemsEarned;

    public Order() {
        // Required for Firestore
    }

    public Order(String userEmail, List<Product> items, double totalAmount, long timestamp, 
                 String shippingName, String shippingAddress, String shippingCity, String paymentMethod, double deliveryCharge) {
        this.userEmail = userEmail;
        this.items = items;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
        this.shippingName = shippingName;
        this.shippingAddress = shippingAddress;
        this.shippingCity = shippingCity;
        this.paymentMethod = paymentMethod;
        this.deliveryCharge = deliveryCharge;
        this.status = "Pending";
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public List<Product> getItems() {
        return items;
    }

    public void setItems(List<Product> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getShippingName() {
        return shippingName;
    }

    public void setShippingName(String shippingName) {
        this.shippingName = shippingName;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public int getGemsEarned() {
        return gemsEarned;
    }

    public void setGemsEarned(int gemsEarned) {
        this.gemsEarned = gemsEarned;
    }
}