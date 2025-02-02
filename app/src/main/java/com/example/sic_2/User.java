package com.example.sic_2;

public class User {
    private String name;
    private String email;

    public User() {} // Empty constructor needed for Firebase

    public User(String userId, String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
}
