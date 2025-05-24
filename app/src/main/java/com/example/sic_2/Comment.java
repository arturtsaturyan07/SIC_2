package com.example.sic_2;

import java.util.HashMap;
import java.util.Map;

public class Comment {
    private String id;
    private String userId;
    private String content;
    private long timestamp;
    // For likes: key is userId, value is true
    private Map<String, Boolean> likes = new HashMap<>();

    public Comment() {}

    public Comment(String userId, String content, long timestamp) {
        this.userId = userId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Optionally add id constructor if you want
    public Comment(String id, String userId, String content, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
}