package com.example.fintracker;

public class Transaction {
    private String id;
    private String userId;
    private double amount;
    private String category;
    private String wallet;
    private String description;
    private String type; // "Income" or "Expense"
    private long timestamp;

    public Transaction() {
        // Required empty constructor for Firestore
    }

    public Transaction(String userId, double amount, String category, String wallet, String description, String type, long timestamp) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.wallet = wallet;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
