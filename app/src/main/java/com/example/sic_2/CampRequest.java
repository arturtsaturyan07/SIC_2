package com.example.sic_2;

public class CampRequest {
    private String requestId;
    private String cardId;
    private String cardTitle;
    private String requesterId;
    private String requesterName;
    private String ownerId;
    private long timestamp;
    private String status; // "pending", "approved", "rejected"

    public CampRequest() {
        // Default constructor required for Firebase
    }

    public CampRequest(String requestId, String cardId, String cardTitle, String requesterId, String requesterName, String ownerId) {
        this.requestId = requestId;
        this.cardId = cardId;
        this.cardTitle = cardTitle;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.ownerId = ownerId;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getCardTitle() { return cardTitle; }
    public void setCardTitle(String cardTitle) { this.cardTitle = cardTitle; }
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}