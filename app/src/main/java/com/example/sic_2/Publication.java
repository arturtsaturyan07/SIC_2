package com.example.sic_2;

public class Publication {
    private String publicationId;
    private String authorId;
    private String content;
    private String imageUrl; // Field for the image URL
    private long timestamp;

    public Publication() {}

    public Publication(String publicationId, String authorId, String content, String imageUrl, long timestamp) {
        this.publicationId = publicationId;
        this.authorId = authorId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    public String getPublicationId() {
        return publicationId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl; // Getter for the image URL
    }

    public long getTimestamp() {
        return timestamp;
    }
}