package com.example.fintracker;

public class Budget {
    private String id;
    private String userId;
    private String category;
    private double limit;
    private double spent;
    private long timestamp;

    public Budget() {
        // Required empty constructor for Firestore
    }

    public Budget(String userId, String category, double limit, double spent, long timestamp) {
        this.userId = userId;
        this.category = category;
        this.limit = limit;
        this.spent = spent;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getRemaining() {
        return limit - spent;
    }

    public boolean isExceeded() {
        return spent > limit;
    }
}
