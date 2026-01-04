package com.example.fintracker;

import com.example.fintracker.utils.DateUtils;

/**
 * Budget Model with Period Support
 *
 * Period Types:
 * - WEEK: 7 days
 * - MONTH: 1 calendar month
 * - YEAR: 12 months
 */
public class Budget {

    public enum Period {
        WEEK,
        MONTH,
        YEAR
    }

    private String id;               // Firestore document ID
    private String userId;
    private String categoryId;       // Foreign key to Category
    private String categoryName;     // Cached for display
    private double amount;
    private String currency;         // "USD" or "KHR"

    // Period fields
    private String period;           // "WEEK", "MONTH", or "YEAR"
    private long startDate;          // Period start timestamp
    private long endDate;            // Period end timestamp

    private double spent;            // Calculated from transactions
    private long createdAt;
    private long updatedAt;

    // For multi-select delete (NOT saved to Firestore)
    private boolean isSelected = false;

    // Empty constructor for Firestore
    public Budget() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.spent = 0.0;
    }

    public Budget(String userId, String categoryId, String categoryName,
                  double amount, String currency, Period period, long startDate) {
        this();
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.currency = currency;
        this.period = period.name();
        this.startDate = startDate;
        this.endDate = calculateEndDate(period, startDate);
    }

    /**
     * Calculate end date based on period
     */
    private long calculateEndDate(Period period, long startDate) {
        switch (period) {
            case WEEK:
                return startDate + (7 * 24 * 60 * 60 * 1000L); // +7 days
            case MONTH:
                return DateUtils.addMonths(startDate, 1); // +1 month
            case YEAR:
                return DateUtils.addMonths(startDate, 12); // +12 months
            default:
                return startDate;
        }
    }

    /**
     * Check if budget is currently active
     */
    public boolean isActive() {
        long now = System.currentTimeMillis();
        return now >= startDate && now <= endDate;
    }

    /**
     * Check if budget is exceeded
     */
    public boolean isExceeded() {
        return spent > amount;
    }

    /**
     * Get percentage of budget spent
     */
    public int getPercentageSpent() {
        if (amount == 0) return 0;
        return (int) ((spent / amount) * 100);
    }

    /**
     * Get remaining amount
     */
    public double getRemaining() {
        return amount - spent;
    }

    /**
     * Get period display name
     */
    public String getPeriodDisplayName() {
        if (period == null) return "Month"; // Default

        switch (Period.valueOf(period)) {
            case WEEK:
                return "Week";
            case MONTH:
                return "Month";
            case YEAR:
                return "Year";
            default:
                return "Month";
        }
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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    /**
     * BACKWARD COMPATIBILITY: getMonthTimestamp()
     * Maps to startDate for old code compatibility
     */
    public long getMonthTimestamp() {
        return startDate;
    }

    public void setMonthTimestamp(long monthTimestamp) {
        this.startDate = monthTimestamp;
        this.updatedAt = System.currentTimeMillis();
    }
}