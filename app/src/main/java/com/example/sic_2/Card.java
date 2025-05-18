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
    private Map<String, Boolean> campMembers;
    private boolean global = false;
    private long campStartDate;
    private long campEndDate;
    private String imageUrl;

    // Card Preferences/Features
    private Boolean archived = false;         // Hidden from list, but searchable
    private Boolean favorite = false;         // Mark as favorite/starred
    private String category = "None";         // Work/Fun/School/Personal etc
    private String repeat = "None";           // None/Daily/Weekly/Monthly
    private Integer color;                    // Card color theme (ARGB int)
    private String customTitle;               // User's custom title override
    private String notes;                     // Optional notes/description field
    private Boolean reminderEnabled = false;  // Remind me before event

    public Card() {
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
        this.global = false;
        this.campStartDate = 0;
        this.campEndDate = 0;
        this.imageUrl = null;
        this.archived = false;
        this.favorite = false;
        this.category = "None";
        this.repeat = "None";
        this.color = null;
        this.customTitle = null;
        this.notes = null;
        this.reminderEnabled = false;
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
    public boolean isGlobal() { return global; }
    public void setGlobal(boolean global) { this.global = global; }
    public long getCampStartDate() { return campStartDate; }
    public void setCampStartDate(long campStartDate) { this.campStartDate = campStartDate; }
    public long getCampEndDate() { return campEndDate; }
    public void setCampEndDate(long campEndDate) { this.campEndDate = campEndDate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Map<String, Boolean> getCampMembers() {
        if (campMembers == null) {
            campMembers = new HashMap<>();
        }
        return campMembers;
    }
    public void setCampMembers(Map<String, Boolean> campMembers) {
        this.campMembers = campMembers != null ? campMembers : new HashMap<>();
    }
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
    public List<String> getCampMembersAsList() {
        return new ArrayList<>(getCampMembers().keySet());
    }
    public void setCampMembersFromList(List<String> members) {
        this.campMembers = new HashMap<>();
        if (members != null) {
            for (String member : members) {
                this.campMembers.put(member, true);
            }
        }
    }

    // --- Card Preferences Getters/Setters ---
    public Boolean getArchived() { return archived != null && archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }
    public Boolean getFavorite() { return favorite != null && favorite; }
    public void setFavorite(Boolean favorite) { this.favorite = favorite; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRepeat() { return repeat; }
    public void setRepeat(String repeat) { this.repeat = repeat; }
    public Integer getColor() { return color; }
    public void setColor(Integer color) { this.color = color; }
    public String getCustomTitle() { return customTitle; }
    public void setCustomTitle(String customTitle) { this.customTitle = customTitle; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Boolean getReminderEnabled() { return reminderEnabled != null && reminderEnabled; }
    public void setReminderEnabled(Boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }
}