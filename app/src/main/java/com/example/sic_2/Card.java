package com.example.sic_2;

public class Card {
    private String id;
    private String message;
    private String authorId;
    private long timestamp;

    // Default constructor (required for Firebase)
    public Card() {
    }

    // Parameterized constructor
    public Card(String id, String message, String authorId, long timestamp) {
        this.id = id;
        this.message = message;
        this.authorId = authorId;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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