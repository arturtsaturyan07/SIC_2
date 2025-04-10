package com.example.sic_2;

public class CustomNotification {
    private String id;
    private String cardId;
    private String cardName;
    private String message;
    private long timestamp;
    private boolean isRead;

    public CustomNotification() {
        // Required for Firebase
    }

    public CustomNotification(String id, String cardId, String cardName, String message, long timestamp, boolean isRead) {
        this.id = id;
        this.cardId = cardId;
        this.cardName = cardName;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCardId() { return cardId; }
    public String getCardName() { return cardName; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}