package com.example.shopping;

public class Banner {
    private String id;
    private String imageUrl;
    private long timestamp;

    public Banner() {
        // Required for Firestore
    }

    public Banner(String id, String imageUrl, long timestamp) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}