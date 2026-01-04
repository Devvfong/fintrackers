package com.example.fintracker;

/**
 * Transaction Model - Updated with Budget Plan linking
 *
 * New field: budgetId (optional)
 * - Links transaction to a specific budget
 * - Helps track spending against budget plans
 */
public class Transaction {

    private String id;
    private String userId;
    private String type;            // "income" or "expense"
    private double amount;
    private String category;        // Category name
    private String categoryId;      // Category ID (NEW)
    private String description;
    private long timestamp;
    private String location;

    private String budgetId;        // Optional: Links to Budget.id
    private String budgetName;      // Optional: Budget display name (e.g., "Food $1200 - Next Week")

    // Additional fields for compatibility
    private String wallet;          // Wallet/payment method
    private String attachmentUrl;   // Receipt/attachment URL

    // For multi-select (not saved to Firestore)
    private boolean isSelected = false;

    // Empty constructor for Firestore
    public Transaction() {
        this.timestamp = System.currentTimeMillis();
    }

    public Transaction(String userId, String type, double amount, String category,
                       String description, long timestamp) {
        this();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(String budgetId) {
        this.budgetId = budgetId;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
}