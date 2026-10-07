package com.example.shopping;

public class Coupon {
    private String code;
    private double discountAmount;
    private double minOrderValue;

    public Coupon() {
        // Required for Firestore
    }

    public Coupon(String code, double discountAmount, double minOrderValue) {
        this.code = code;
        this.discountAmount = discountAmount;
        this.minOrderValue = minOrderValue;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }
}