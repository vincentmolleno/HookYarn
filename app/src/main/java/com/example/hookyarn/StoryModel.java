package com.example.hookyarn;

public class StoryModel {
    private String id;
    private String userId;
    private String username;
    private String profileImageUrl;
    private String storyImageUrl;
    private long   createdAt;
    private boolean isOwn;

    public StoryModel() {}

    public StoryModel(String userId, String username, String profileImageUrl,
                      String storyImageUrl, long createdAt, boolean isOwn) {
        this.userId          = userId;
        this.username        = username;
        this.profileImageUrl = profileImageUrl;
        this.storyImageUrl   = storyImageUrl;
        this.createdAt       = createdAt;
        this.isOwn           = isOwn;
    }

    public String  getId()              { return id; }
    public String  getUserId()          { return userId; }
    public String  getUsername()        { return username; }
    public String  getProfileImageUrl() { return profileImageUrl; }
    public String  getStoryImageUrl()   { return storyImageUrl; }
    public long    getCreatedAt()       { return createdAt; }
    public boolean isOwn()              { return isOwn; }

    public void setId(String v)              { id = v; }
    public void setUserId(String v)          { userId = v; }
    public void setUsername(String v)        { username = v; }
    public void setProfileImageUrl(String v) { profileImageUrl = v; }
    public void setStoryImageUrl(String v)   { storyImageUrl = v; }
    public void setCreatedAt(long v)         { createdAt = v; }
    public void setOwn(boolean v)            { isOwn = v; }
}