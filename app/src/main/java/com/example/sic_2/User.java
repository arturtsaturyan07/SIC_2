package com.example.sic_2;

import static com.example.sic_2.LoginActivity.email_;
import static com.example.sic_2.RegisterActivity.name_;

public class User {
    private String name;
    private String email;

    public User() {} // Empty constructor needed for Firebase

    public User(String userId, String name, String email) {
        this.name = name_;
        this.email = email_;
    }

    public String getName() { return this.name; }
    public String getEmail() { return this.email; }
}
