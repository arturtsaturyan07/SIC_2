package com.example.sic_2;

import static com.example.sic_2.LoginActivity.email_;
import static com.example.sic_2.RegisterActivity.name_;

public class User {
    private String userId;
    private String name = name_;
    private String email = email_;

    public User(String userId, String name_, String email_, String email) {} // Empty constructor needed for Firebase

    public String getName() { return name; }
    public String getEmail() { return email; }

    public String getUserId() {
        return userId;
    }
}
