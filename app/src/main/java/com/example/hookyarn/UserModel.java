package com.example.hookyarn;

public class UserModel {
    private String uid;
    private String username;
    private String fullName;
    private String profileImageUrl;

    public UserModel() {}

    public UserModel(String uid, String username, String fullName, String profileImageUrl) {
        this.uid = uid;
        this.username = username;
        this.fullName = fullName;
        this.profileImageUrl = profileImageUrl;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
