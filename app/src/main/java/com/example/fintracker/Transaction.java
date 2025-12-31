package com.example.fintracker;

public class Transaction {
    private String id, userId, type, category, wallet, description, attachmentUrl;
    private double amount;
    private long timestamp;
    private boolean isRepeated = false;  // ✅ ADD THIS LINE

    public Transaction() {}

    // Getters/Setters (ALL)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getWallet() { return wallet; }
    public void setWallet(String wallet) { this.wallet = wallet; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // ✅ ADD THESE 2 METHODS:
    public boolean isRepeated() { return isRepeated; }
    public void setRepeated(boolean repeated) { isRepeated = repeated; }
}
