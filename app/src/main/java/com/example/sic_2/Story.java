package com.example.sic_2;

public class Story {
    private String id;
    private String cardId;
    private String imageUrl;
    private long timestamp;
    private String userId; // optional, for tracking who posted

    public Story() {}
    public Story(String id, String cardId, String imageUrl, long timestamp, String userId) {
        this.id = id;
        this.cardId = cardId;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    // Getters and setters...
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}