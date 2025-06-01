package org.neracaku.neracaku.models;

// Menggunakan java.time untuk tanggal yang lebih modern
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String pinHash;
    private String role; // "user" atau "admin"
    private LocalDateTime createdAt;

    // Formatter untuk parsing dan formatting tanggal dari/ke String SQLite
    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructor default
    public User() {
    }

    // Constructor untuk membuat user baru (tanpa id dan createdAt, karena itu dari DB)
    public User(String username, String passwordHash, String pinHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.pinHash = pinHash;
        this.role = role;
    }

    // Getter dan Setter
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setter untuk createdAt dari String (misal dari database)
    public void setCreatedAt(String createdAtString) {
        if (createdAtString != null && !createdAtString.isEmpty()) {
            this.createdAt = LocalDateTime.parse(createdAtString, SQLITE_DATETIME_FORMATTER);
        }
    }

    // Setter untuk createdAt dari LocalDateTime
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Opsional: toString untuk debugging
    @Override
    public String toString() {
        return "User{userId=" + userId + ", username='" + username + "', role='" + role + "', createdAt=" + (createdAt != null ? createdAt.format(SQLITE_DATETIME_FORMATTER) : "null") + "}";
    }
}