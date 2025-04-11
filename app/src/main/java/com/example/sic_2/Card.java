package com.example.sic_2;

import java.util.ArrayList;
import java.util.List;

public class Card {
    private String id;
    private String title;
    private String description;
    private String priority;
    private String authorId;
    private long timestamp;
    private boolean isCampCard;
    private List<String> campMembers;

    public Card() {
        // Default constructor required for Firebase
    }

    public Card(String id, String title, String description, String priority, String authorId, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.authorId = authorId;
        this.timestamp = timestamp;
        this.isCampCard = false;
        this.campMembers = new ArrayList<>();
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
    public List<String> getCampMembers() { return campMembers; }
    public void setCampMembers(List<String> campMembers) { this.campMembers = campMembers; }

    public void addCampMember(String userId) {
        if (campMembers == null) {
            campMembers = new ArrayList<>();
        }
        if (!campMembers.contains(userId)) {
            campMembers.add(userId);
        }
    }
}