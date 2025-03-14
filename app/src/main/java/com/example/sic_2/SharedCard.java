package com.example.sic_2;

public class SharedCard {
    private String cardId;
    private String message;
    private String sharedBy;

    public SharedCard() {}

    public SharedCard(String cardId, String message, String sharedBy) {
        this.cardId = cardId;
        this.message = message;
        this.sharedBy = sharedBy;
    }

    public String getCardId() {
        return cardId;
    }

    public String getMessage() {
        return message;
    }

    public String getSharedBy() {
        return sharedBy;
    }
}