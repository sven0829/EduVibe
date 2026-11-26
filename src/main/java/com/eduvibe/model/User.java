
package com.eduvibe.model;

import java.time.LocalDateTime;

public class User {

    private int id;
    private String fullName;
    private String username;
    private String email;
    private String bio;
    private String profilePic;
    private String role;
    private String status;
    private int points;
    private int entertainmentLimitMinutes;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(int id, String fullName, String username,
                String email, String role, String status,
                int points) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.points = points;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getEntertainmentLimitMinutes() {
        return entertainmentLimitMinutes;
    }

    public void setEntertainmentLimitMinutes(int minutes) {
        this.entertainmentLimitMinutes = minutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
