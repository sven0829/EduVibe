
package com.eduvibe.model;

import java.time.LocalDateTime;

public class Post {

    private int id;
    private int userId;
    private String username;
    private String category;
    private String title;
    private String description;
    private String mediaType;
    private String mediaPath;
    private boolean reel;
    private LocalDateTime createdAt;
    private int likeCount;
    private int commentCount;
    private boolean likedByCurrentUser;
    private String authorProfilePic;

    public String getAuthorProfilePic() { return authorProfilePic; }
    public void setAuthorProfilePic(String authorProfilePic) {
        this.authorProfilePic = authorProfilePic;
    }

    public Post() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaPath() { return mediaPath; }
    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public boolean isReel() { return reel; }
    public void setReel(boolean reel) { this.reel = reel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    /** Pending user reports on this post (drives the content warning). */
    private int pendingReportCount;

    public int getPendingReportCount() { return pendingReportCount; }
    public void setPendingReportCount(int pendingReportCount) {
        this.pendingReportCount = Math.max(0, pendingReportCount);
    }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }
}
