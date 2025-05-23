package com.example.sic_2;

import android.util.Log;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.HashMap;
import java.util.Map;

public class ChatMessage {

    private String id;
    private String senderId;
    private String message;
    private long timestamp;
    private String senderName;
    private String profileImageUrl;
    private String imageUrl;
    private Map<String, Boolean> delivered = new HashMap<>();
    private Map<String, Boolean> read = new HashMap<>();
    private String audioUrl;
    private String circleVideoUrl;

    // Reactions: emoji -> <uid, true>
    private Map<String, Map<String, Boolean>> reactions = new HashMap<>();

    public ChatMessage() {}

    public ChatMessage(String senderId, String name, String message, long timestamp, String profileImageUrl) {
        this.senderId = senderId;
        this.senderName = name;
        this.message = message;
        this.timestamp = timestamp;
        this.profileImageUrl = profileImageUrl;
        this.read.put(senderId, false); // Not read by sender yet
        this.delivered.put(senderId, true); // Delivered by sender
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public Map<String, Boolean> getDelivered() { return delivered; }
    public void setDelivered(Object delivered) {
        if (delivered instanceof Map) {
            this.delivered = (Map<String, Boolean>) delivered;
        } else {
            this.delivered = new HashMap<>();
        }
    }

    public Map<String, Boolean> getRead() { return read; }
    public void setRead(Object read) {
        if (read instanceof Map) {
            this.read = (Map<String, Boolean>) read;
        } else {
            this.read = new HashMap<>();
        }
    }

    @Exclude
    public boolean isDelivered() { return !delivered.isEmpty(); }
    @Exclude
    public boolean isRead() { return !read.isEmpty(); }
    @Exclude
    public boolean isDeliveredByUser(String userId) {
        return Boolean.TRUE.equals(delivered.get(userId));
    }
    @Exclude
    public boolean isReadByUser(String userId) {
        return Boolean.TRUE.equals(read.get(userId));
    }
    @Exclude
    public void markDeliveredForUser(String userId) {
        delivered.put(userId, true);
    }
    @Exclude
    public void markReadForUser(String userId) {
        read.put(userId, true);
    }

    public static final GenericTypeIndicator<Map<String, Boolean>> deliveredTypeIndicator =
            new GenericTypeIndicator<Map<String, Boolean>>() {};
    public static final GenericTypeIndicator<Map<String, Boolean>> readTypeIndicator =
            new GenericTypeIndicator<Map<String, Boolean>>() {};

    @Exclude
    public void parseDelivered(Object deliveredObj) {
        if (deliveredObj instanceof Map) {
            this.delivered = (Map<String, Boolean>) deliveredObj;
        } else {
            Log.w("ChatMessage", "Invalid delivered data: " + deliveredObj);
            this.delivered.clear();
        }
    }
    @Exclude
    public void parseRead(Object readObj) {
        if (readObj instanceof Map) {
            this.read = (Map<String, Boolean>) readObj;
        } else {
            Log.w("ChatMessage", "Invalid read data: " + readObj);
            this.read.clear();
        }
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getCircleVideoUrl() { return circleVideoUrl; }
    public void setCircleVideoUrl(String circleVideoUrl) { this.circleVideoUrl = circleVideoUrl; }

    // Reactions (per-emoji, per-user)
    public Map<String, Map<String, Boolean>> getReactions() { return reactions; }
    public void setReactions(Map<String, Map<String, Boolean>> reactions) { this.reactions = reactions; }
}