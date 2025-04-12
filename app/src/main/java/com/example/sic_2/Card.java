package com.example.sic_2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Card {
    private String id;
    private String title;
    private String description;
    private String priority;
    private String authorId;
    private long timestamp;
    private boolean isCampCard;
    private Map<String, Boolean> campMembers;  // Changed from List<String> to Map<String, Boolean>

    public Card() {
        // Default constructor required for Firebase
        this.campMembers = new HashMap<>();
    }

    public Card(String id, String title, String description, String priority, String authorId, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.authorId = authorId;
        this.timestamp = timestamp;
        this.isCampCard = false;
        this.campMembers = new HashMap<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isCampCard() { return isCampCard; }
    public void setCampCard(boolean campCard) { isCampCard = campCard; }

    // Updated campMembers getter and setter
    public Map<String, Boolean> getCampMembers() {
        if (campMembers == null) {
            campMembers = new HashMap<>();
        }
        return campMembers;
    }

    public void setCampMembers(Map<String, Boolean> campMembers) {
        this.campMembers = campMembers != null ? campMembers : new HashMap<>();
    }

    // Helper methods for camp membership
    public boolean isCampMember(String userId) {
        return campMembers != null && campMembers.containsKey(userId);
    }

    public void addCampMember(String userId) {
        getCampMembers().put(userId, true);
    }

    public void removeCampMember(String userId) {
        if (campMembers != null) {
            campMembers.remove(userId);
        }
    }

    // Compatibility method for existing code expecting List<String>
    public List<String> getCampMembersAsList() {
        return new ArrayList<>(getCampMembers().keySet());
    }

    // Compatibility method for existing code using List<String>
    public void setCampMembersFromList(List<String> members) {
        this.campMembers = new HashMap<>();
        if (members != null) {
            for (String member : members) {
                this.campMembers.put(member, true);
            }
        }
    }
}