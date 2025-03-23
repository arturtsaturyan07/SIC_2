package com.example.sic_2;

public class CustomNotification {
    private String id;
    private String cardId;
    private String cardName;
    private String message;
    private long timestamp;
    private boolean isRead;

    // Default constructor (required for Firebase)
    public CustomNotification() {}

    public CustomNotification(String id, String cardId, String cardName, String message, long timestamp, boolean isRead) {
        this.id = id;
        this.cardId = cardId;
        this.cardName = cardName;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}