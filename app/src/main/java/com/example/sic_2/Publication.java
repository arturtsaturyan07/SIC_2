package com.example.sic_2;

public class Publication {
    private String userId;
    private String content;
    private String imageUrl;
    private long timestamp;
    private String userProfileImageUrl;

    public Publication() {
        // Required for Firebase
    }

    public Publication(String userId, String content, String imageUrl, long timestamp, String userProfileImageUrl) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.userProfileImageUrl = userProfileImageUrl;
    }

    // Convenience constructor if no profile image url is provided
    public Publication(String userId, String content, String imageUrl, long timestamp) {
        this(userId, content, imageUrl, timestamp, null);
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

    public String getUserProfileImageUrl() {
        return userProfileImageUrl;
    }

    public void setUserProfileImageUrl(String userProfileImageUrl) {
        this.userProfileImageUrl = userProfileImageUrl;
    }
}