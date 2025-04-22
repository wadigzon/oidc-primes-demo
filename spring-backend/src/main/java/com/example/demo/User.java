package com.example.demo;

public class User {
    private String email;
    private String hashedPassword;
    public User(String email2, String hashedPassword2) {
        email = email2;
        hashedPassword = hashedPassword2;
    }
    public String getHashedPassword() {
        return hashedPassword;
    }
    public String getEmail() {
        return email;
    }
}

