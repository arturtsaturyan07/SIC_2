package com.example.sic_2;

public class ImageData {
    public String authorId;
    public String imageUrl;
    public long timestamp;

    public ImageData() {
        // Default constructor required for Firebase
    }

    public ImageData(String authorId, String imageUrl, long timestamp) {
        this.authorId = authorId;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }
}