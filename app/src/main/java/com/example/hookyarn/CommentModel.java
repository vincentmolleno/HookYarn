package com.example.hookyarn;

import com.google.firebase.Timestamp;

public class CommentModel {
    private String commentId;
    private String postId;
    private String uid;
    private String username;
    private String commentText;
    private Timestamp createdAt;

    public CommentModel() {}

    public CommentModel(String commentId, String postId, String uid, String username, String commentText) {
        this.commentId = commentId;
        this.postId = postId;
        this.uid = uid;
        this.username = username;
        this.commentText = commentText;
        this.createdAt = Timestamp.now();
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
