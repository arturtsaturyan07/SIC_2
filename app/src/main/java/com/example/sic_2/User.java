package com.example.sic_2;

import com.google.firebase.database.Exclude;
import java.util.HashMap;
import java.util.Map;

public class User {

    private String userId;
    private String name;
    private String surname;
    private String email;
    private String profileImageUrl;
    private String bio;
    private String status; // "online", "offline", "typing"
    private long lastSeen;

    // 🔹 Add unreadCount for chat badge
    private int unreadCount = 0;

    // 🔹 Required empty constructor for Firebase
    public User() {}

    // 🔹 Lightweight constructor for cases where we only have userId
    public User(String userId) {
        this.userId = userId;
        this.name = null;
        this.surname = null;
        this.email = null;
        this.profileImageUrl = null;
        this.bio = null;
        this.status = "offline";
        this.lastSeen = 0;
        this.unreadCount = 0;
    }

    // 🔹 Constructor used in MainActivity (for new user creation)
    public User(String userId, String name, String surname, String email) {
        this.userId = userId;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.profileImageUrl = null;
        this.bio = null;
        this.status = "offline";
        this.lastSeen = 0;
        this.unreadCount = 0;
    }

    // 🔹 Constructor used in DirectChatsFragment
    public User(String userId, String name, String surname, String email,
                String profileImageUrl, String bio, boolean isOnline) {
        this.userId = userId;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.status = isOnline ? "online" : "offline";
        this.lastSeen = System.currentTimeMillis();
        this.unreadCount = 0;
    }

    // 🔹 Full constructor
    public User(String userId, String name, String surname, String email,
                String profileImageUrl, String bio, String status, long lastSeen) {
        this.userId = userId;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.status = status;
        this.lastSeen = lastSeen;
        this.unreadCount = 0;
    }

    // 🔹 Getters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getBio() { return bio; }
    public String getStatus() { return status; }
    public long getLastSeen() { return lastSeen; }
    public int getUnreadCount() { return unreadCount; }

    // 🔹 Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setStatus(String status) { this.status = status; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    // 🔹 Helper methods
    public String getFullName() {
        return (name != null ? name : "") + " " + (surname != null ? surname : "");
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);
        result.put("surname", surname);
        result.put("email", email);
        result.put("profileImageUrl", profileImageUrl);
        result.put("bio", bio);
        result.put("status", status);
        result.put("lastSeen", lastSeen);
        result.put("unreadCount", unreadCount);
        return result;
    }
}