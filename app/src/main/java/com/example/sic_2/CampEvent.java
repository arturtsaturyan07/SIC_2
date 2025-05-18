package com.example.sic_2;

public class CampEvent {
    private String id;
    private long date; // millis (exact event time, not just start of day)
    private String title;
    private String description;
    private String createdBy; // userId
    private String imageUrl;  // <-- new field for event image

    public CampEvent() {}

    public CampEvent(String id, long date, String title, String description, String createdBy, String imageUrl) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}