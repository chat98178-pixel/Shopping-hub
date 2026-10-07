package com.example.shopping;

public class Review {
    private String userName;
    private String comment;
    private long timestamp;
    private String imageUrl;
    private String videoUrl;
    private boolean isVideo;

    public Review() {
        // Required for Firestore
    }

    public Review(String userName, String comment, long timestamp) {
        this.userName = userName;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public Review(String userName, String comment, long timestamp, String imageUrl) {
        this.userName = userName;
        this.comment = comment;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }
}