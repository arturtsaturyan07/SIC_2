package com.example.sic_2;

public class User {
    private String userId;
    private String name;
    private String surname;
    private String email;

    // Empty constructor required for Firebase
    public User() {}

    // Constructor with parameters
    public User(String userId, String name, String surname, String email) {
        this.userId = userId;
        this.name = name;
        this.surname = surname;
        this.email = email;
    }

    // Getters required for Firebase
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public String getEmail() { return email; }

    // Setters for all fields
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setEmail(String email) { this.email = email; }

    // Helper method to get full name
    public String getFullName() {
        return (name != null ? name : "") + " " + (surname != null ? surname : "");
    }

    // Method to update from RegisterActivity
    public void updateFromRegistration(String name, String surname, String email) {
        this.name = name;
        this.surname = surname;
        this.email = email;
    }
}