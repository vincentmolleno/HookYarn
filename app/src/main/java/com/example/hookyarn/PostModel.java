package com.example.hookyarn;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class PostModel {

    private String id;
    private String uid;
    private String username;
    private String profileImageUrl;

    private String description;
    private String imageUrl;
    private String caption;

    private int likeCount;
    private int commentCount;
    private java.util.List<String> likedBy;

    private Timestamp createdAt;

    public PostModel() {
    }

    public PostModel(String uid, String imageUrl, String caption) {

        this.uid = uid;
        this.imageUrl = imageUrl;
        this.caption = caption;

        this.likeCount = 0;
        this.commentCount = 0;

        this.createdAt = Timestamp.now();
    }

    @PropertyName("id")
    public String getId() {
        return id;
    }

    @PropertyName("uid")
    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("caption")
    public String getCaption() {
        return caption;
    }

    @PropertyName("likeCount")
    public int getLikeCount() {
        return likeCount;
    }

    @PropertyName("commentCount")
    public int getCommentCount() {
        return commentCount;
    }

    @PropertyName("likedBy")
    public java.util.List<String> getLikedBy() {
        if (likedBy == null) likedBy = new java.util.ArrayList<>();
        return likedBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("id")
    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("uid")
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("caption")
    public void setCaption(String caption) {
        this.caption = caption;
    }

    @PropertyName("likeCount")
    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    @PropertyName("commentCount")
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    @PropertyName("likedBy")
    public void setLikedBy(java.util.List<String> likedBy) {
        this.likedBy = likedBy;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}