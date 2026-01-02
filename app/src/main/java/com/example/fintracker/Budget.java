package com.example.fintracker;

public class Budget {
    private String id;
    private String userId;
    private String category;
    private double amount;
    private double spent;
    private long timestamp;

    public Budget() {}

    public Budget(String userId, String category, double amount, double spent, long timestamp) {
        this.userId = userId;
        this.category = category;
        this.amount = amount;
        this.spent = spent;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getSpent() { return spent; }
    public void setSpent(double spent) { this.spent = spent; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getLimit() { return amount; }
    public double getRemaining() { return amount - spent; }
    public int getPercentageSpent() {
        if (amount == 0) return 0;
        return (int) ((spent / amount) * 100);
    }
    public boolean isExceeded() { return spent > amount; }
}