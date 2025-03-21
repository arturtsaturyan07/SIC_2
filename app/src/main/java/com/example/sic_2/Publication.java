package com.example.sic_2;

public class Publication {
    private String authorId;
    private String content;
    private long timestamp;

    public Publication() {
        // Default constructor required for Firebase
    }

    public Publication(String authorId, String content, long timestamp) {
        this.authorId = authorId;
        this.content = content;
        this.timestamp = timestamp;
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