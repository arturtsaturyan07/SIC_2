package com.example.sic_2;

public class ChatPreview {
    private String cardId;
    private String lastMessage;
    private long timestamp;

    public ChatPreview() {
        // Required empty constructor for Firebase
    }

    public ChatPreview(String cardId, String lastMessage, long timestamp) {
        this.cardId = cardId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getCardId() {
        return cardId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }
}