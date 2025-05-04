package com.example.sic_2;

public class CustomNotification {
    public final String cardId;
    public final String cardName;
    public final String senderName;
    public final String message;
    public final long timestamp;

    public CustomNotification() {
        this("", "", "", "", 0);
    }
    public CustomNotification(String cardId, String cardName, String senderName, String message, long timestamp) {
        this.cardId = cardId;
        this.cardName = cardName;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public CustomNotification(String cardId, String cardName, String message, String senderName) {
        this.cardId = cardId;
        this.cardName = cardName;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}