package com.example.sic_2;

import java.util.HashMap;
import java.util.Map;

public class ChatMessage {

    private String id;
    private String senderId;
    private String message;
    private long timestamp;
    private String senderName;
    private String profileImageUrl;

    // Status fields as boolean for simplicity
    private boolean delivered;
    private boolean read;

    public ChatMessage() {
        // Required empty constructor for Firebase
    }

    public ChatMessage(String senderId, String name, String message, long timestamp, String profileImageUrl) {
        this.senderId = senderId;
        this.senderName = name;
        this.message = message;
        this.timestamp = timestamp;
        this.profileImageUrl = profileImageUrl;
        this.read = false;
        this.delivered = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
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

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }


}