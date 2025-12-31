package com.example.fintracker;

public class Budget {
    private String id;
    private String category;
    private double amount;
    private double spent;

    // Empty constructor for Firebase
    public Budget() {
    }

    public Budget(String category, double amount, double spent) {
        this.category = category;
        this.amount = amount;
        this.spent = spent;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getSpent() {
        return spent;
    }

    // Helper methods
    public double getLimit() {
        return amount;
    }

    public double getRemaining() {
        return amount - spent;
    }

    public boolean isExceeded() {
        return spent > amount;
    }

}