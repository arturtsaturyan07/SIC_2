package com.example.sic_2;

public class Publication {
    private String userId;
    private String content;
    private String imageUrl;
    private long timestamp;

    public Publication() {
        // Required for Firebase
    }

    public Publication(String userId, String content, String imageUrl, long timestamp) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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