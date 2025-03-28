package com.example.sic_2;

public class Publication {
    private String userId;
    private String content;
    private String imageUrl;
    private long timestamp;

    // Required empty constructor for Firebase
    public Publication() {}

    // Constructor for text posts
    public Publication(String userId, String content, long timestamp) {
        this.userId = userId;
        this.content = content;
        this.timestamp = timestamp;
        this.imageUrl = null;  // Explicitly set imageUrl to null
    }

    // Constructor for image posts
    public Publication(String userId, String imageUrl, long timestamp) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.content = null;  // Explicitly set content to null
    }

    // Combined constructor for posts with both text and image
    public Publication(String userId, String content, String imageUrl, long timestamp) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp; }

    // Setters (optional, but useful for Firebase)
    public void setUserId(String userId) { this.userId = userId; }
    public void setContent(String content) { this.content = content; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}