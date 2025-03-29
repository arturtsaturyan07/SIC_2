package com.example.sic_2;

public class Publication {
    private String userId;
    private String content;
    private String imageUrl;
    private long timestamp;

    // Single constructor that handles both text and image posts
    public Publication(String userId, String content, String imageUrl, long timestamp) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    // Required empty constructor for Firebase
    public Publication() {
        // Default constructor required for calls to DataSnapshot.getValue(Publication.class)
    }

    // Getters
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp; }

    // Setters (required for Firebase to properly deserialize objects)
    public void setUserId(String userId) { this.userId = userId; }
    public void setContent(String content) { this.content = content; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}