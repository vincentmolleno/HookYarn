package com.example.hookyarn;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class ThreadPost {

    private String postId;
    private String userId;
    private String username;
    private String text;
    private String imageUrl;
    private String profileImageUrl;
    private long likesCount;
    private long commentCount;
    private java.util.List<String> likedBy;
    private Timestamp createdAt;

    public ThreadPost(){}

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() { return createdAt; }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @PropertyName("id")
    public String getPostId() { return postId; }
    
    @PropertyName("id")
    public void setPostId(String postId) { this.postId = postId; }

    @PropertyName("uid")
    public String getUserId() { return userId; }
    
    @PropertyName("uid")
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @PropertyName("caption")
    public String getText() { return text; }
    
    @PropertyName("caption")
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("profileImageUrl")
    public String getProfileImageUrl() { return profileImageUrl; }

    @PropertyName("profileImageUrl")
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    @PropertyName("likeCount")
    public long getLikesCount() { return likesCount; }
    
    @PropertyName("likeCount")
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }

    @PropertyName("commentCount")
    public long getCommentCount() { return commentCount; }

    @PropertyName("commentCount")
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }

    @PropertyName("likedBy")
    public java.util.List<String> getLikedBy() {
        if (likedBy == null) likedBy = new java.util.ArrayList<>();
        return likedBy;
    }

    @PropertyName("likedBy")
    public void setLikedBy(java.util.List<String> likedBy) { this.likedBy = likedBy; }
}