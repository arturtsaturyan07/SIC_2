package com.example.sic_2;

public class UnreadChatPreview {
    private String cardId;
    private String lastMessage;
    private long timestamp;

    public UnreadChatPreview(String cardId, String lastMessage, long timestamp) {
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