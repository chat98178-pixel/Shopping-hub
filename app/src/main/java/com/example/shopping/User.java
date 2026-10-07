package com.example.shopping;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String fullName;
    private String email;
    private String phoneNumber;
    private boolean isAdmin;
    private String profileImageUrl;
    private String savedAddress;
    private String savedCity;
    private int hubGems = 0;
    private List<String> collectedVouchers = new ArrayList<>();
    private List<String> recentlyViewed = new ArrayList<>();

    public User() {
        // Required for Firestore
    }

    public User(String fullName, String email, String phoneNumber, boolean isAdmin) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isAdmin = isAdmin;
        this.collectedVouchers = new ArrayList<>();
    }

    public User(String fullName, String email, String phoneNumber, boolean isAdmin, String profileImageUrl) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isAdmin = isAdmin;
        this.profileImageUrl = profileImageUrl;
        this.collectedVouchers = new ArrayList<>();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getSavedAddress() {
        return savedAddress;
    }

    public void setSavedAddress(String savedAddress) {
        this.savedAddress = savedAddress;
    }

    public String getSavedCity() {
        return savedCity;
    }

    public void setSavedCity(String savedCity) {
        this.savedCity = savedCity;
    }

    public int getHubGems() {
        return hubGems;
    }

    public void setHubGems(int hubGems) {
        this.hubGems = hubGems;
    }

    public List<String> getCollectedVouchers() {
        return collectedVouchers;
    }

    public void setCollectedVouchers(List<String> collectedVouchers) {
        this.collectedVouchers = collectedVouchers;
    }

    public List<String> getRecentlyViewed() {
        return recentlyViewed;
    }

    public void setRecentlyViewed(List<String> recentlyViewed) {
        this.recentlyViewed = recentlyViewed;
    }
}