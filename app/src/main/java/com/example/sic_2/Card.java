package com.example.sic_2;

public class Card {
    private String id;
    private String title;
    private String description;
    private String priority;
    private String authorId;
    private long timestamp;

    public Card() {
        // Default constructor required for Firebase
    }

    public Card(String id, String title, String description, String priority, String authorId, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.authorId = authorId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}