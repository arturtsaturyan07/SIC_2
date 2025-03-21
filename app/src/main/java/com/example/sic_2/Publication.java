package com.example.sic_2;

public class Publication {
    private String publicationId;
    private String authorId;
    private String content;
    private long timestamp;

    public Publication() {}

    public Publication(String publicationId, String authorId, String content, long timestamp) {
        this.publicationId = publicationId;
        this.authorId = authorId;
        this.content = content;
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

    public long getTimestamp() {
        return timestamp;
    }
}