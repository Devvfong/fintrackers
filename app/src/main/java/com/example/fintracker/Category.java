package com.example.fintracker;

/**
 * Category Model - Shared between Transactions and Budgets
 *
 * Categories are global and stored in Firestore "categories" collection
 * Any user can create categories, and they're available everywhere
 */
public class Category {

    private String id;              // Firestore document ID
    private String name;            // Category name (e.g., "Food", "Sport")
    private String color;           // Hex color (e.g., "#FF0000")
    private String icon;            // Icon name or emoji (e.g., "🍔", "⚽")
    private String userId;          // User who created it (optional)
    private long createdAt;

    // Empty constructor for Firestore
    public Category() {
        this.createdAt = System.currentTimeMillis();
    }

    public Category(String name, String color, String icon) {
        this();
        this.name = name;
        this.color = color;
        this.icon = icon;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name; // For Spinner display
    }
}