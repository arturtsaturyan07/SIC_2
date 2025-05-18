package com.example.sic_2;

import java.util.HashMap;
import java.util.Map;

/**
 * Publication model for posts with reactions, editing, and comments support.
 */
public class Publication {
    private String id;
    private String cardId;
    private String userId;
    private String content;
    private String imageUrl;
    private long timestamp;
    private String userProfileImageUrl;
    private String authorFullName;
    private Map<String, String> reactions = new HashMap<>(); // userId -> reaction ("like", "dislike", or emoji)

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
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUserProfileImageUrl() { return userProfileImageUrl; }
    public void setUserProfileImageUrl(String userProfileImageUrl) { this.userProfileImageUrl = userProfileImageUrl; }

    public String getAuthorFullName() { return authorFullName; }
    public void setAuthorFullName(String authorFullName) { this.authorFullName = authorFullName; }

    public Map<String, String> getReactions() { return reactions; }
    public void setReactions(Map<String, String> reactions) { this.reactions = reactions; }
}