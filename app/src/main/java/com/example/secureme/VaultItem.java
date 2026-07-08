package com.example.secureme;

public class VaultItem {
    private String title;
    private String username;
    private String password;
    private String notes;

    public VaultItem() {} // Needed for Firestore

    public VaultItem(String title, String username, String password, String notes) {
        this.title = title;
        this.username = username;
        this.password = password;
        this.notes = notes;
    }

    public String getTitle() { return title; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNotes() { return notes; }
}
