package com.example.fintracker;


import java.util.Date;

public class Transaction {
    private String id;
    private String userId;
    private String type; // "income", "expense", "transfer"
    private double amount;
    private String category;
    private String wallet; // account/wallet name
    private String description;
    private String attachmentUrl; // Firebase Storage URL for receipt
    private long timestamp;
    private boolean isRepeated;
    private String repeatFrequency; // "daily", "weekly", "monthly", "yearly"
    private long repeatEndDate;

    // For transfers
    private String fromWallet;
    private String toWallet;

    // Constructors
    public Transaction() {
        // Required for Firebase
    }

    public Transaction(String userId, String type, double amount, String category,
                       String wallet, String description, long timestamp) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.wallet = wallet;
        this.description = description;
        this.timestamp = timestamp;
        this.isRepeated = false;
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

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRepeated() {
        return isRepeated;
    }

    public void setRepeated(boolean repeated) {
        isRepeated = repeated;
    }

    public String getRepeatFrequency() {
        return repeatFrequency;
    }

    public void setRepeatFrequency(String repeatFrequency) {
        this.repeatFrequency = repeatFrequency;
    }

    public long getRepeatEndDate() {
        return repeatEndDate;
    }

    public void setRepeatEndDate(long repeatEndDate) {
        this.repeatEndDate = repeatEndDate;
    }

    public String getFromWallet() {
        return fromWallet;
    }

    public void setFromWallet(String fromWallet) {
        this.fromWallet = fromWallet;
    }

    public String getToWallet() {
        return toWallet;
    }

    public void setToWallet(String toWallet) {
        this.toWallet = toWallet;
    }
}