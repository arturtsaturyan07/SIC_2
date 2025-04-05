package com.example.sic_2;

public class ChatMessage {
    private String id;
    private String senderId;
    private String message;
    private long timestamp;

    public ChatMessage() {
        // Default constructor required for Firebase
    }

    public ChatMessage(String senderId, String message, long timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Add getters and setters for all fields including ID
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}