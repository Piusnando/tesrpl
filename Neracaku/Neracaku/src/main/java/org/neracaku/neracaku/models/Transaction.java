package org.neracaku.neracaku.models; // PASTIKAN PACKAGE SESUAI

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private int transactionId;
    private int userId; // Foreign key ke tabel users
    private int categoryId; // Foreign key ke tabel categories
    private double amount;
    private LocalDate transactionDate; // Tanggal terjadinya transaksi
    private String description;
    private String imagePath; // Path ke file gambar (nota/kwitansi)
    private String type; // "pemasukan" atau "pengeluaran"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Formatter untuk parsing dan formatting tanggal dari/ke String SQLite (YYYY-MM-DD)
    public static final DateTimeFormatter SQLITE_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
    // Formatter untuk timestamp SQLite (YYYY-MM-DD HH:MM:SS)
    public static final DateTimeFormatter SQLITE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    // Constructor default
    public Transaction() {
    }

    // Constructor untuk membuat objek baru (biasanya tanpa ID dan timestamp, karena itu dari DB)
    public Transaction(int userId, int categoryId, double amount, LocalDate transactionDate, String description, String imagePath, String type) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.imagePath = imagePath;
        this.type = type;
    }

    // Getters and Setters
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    // Setter untuk transactionDate dari String (misal dari database)
    public void setTransactionDate(String transactionDateString) {
        if (transactionDateString != null && !transactionDateString.isEmpty()) {
            this.transactionDate = LocalDate.parse(transactionDateString, SQLITE_DATE_FORMATTER);
        }
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Setter untuk createdAt dari String (misal dari database)
    public void setCreatedAt(String createdAtString) {
        if (createdAtString != null && !createdAtString.isEmpty()) {
            this.createdAt = LocalDateTime.parse(createdAtString, SQLITE_DATETIME_FORMATTER);
        }
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Setter untuk updatedAt dari String (misal dari database)
    public void setUpdatedAt(String updatedAtString) {
        if (updatedAtString != null && !updatedAtString.isEmpty()) {
            this.updatedAt = LocalDateTime.parse(updatedAtString, SQLITE_DATETIME_FORMATTER);
        }
    }

    @Override
    public String toString() {
        return "Transaction{" +
               "transactionId=" + transactionId +
               ", userId=" + userId +
               ", categoryId=" + categoryId +
               ", amount=" + amount +
               ", transactionDate=" + (transactionDate != null ? transactionDate.format(SQLITE_DATE_FORMATTER) : "null") +
               ", description='" + description + '\'' +
               ", type='" + type + '\'' +
               '}';
    }
}