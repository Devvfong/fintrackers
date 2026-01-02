package com.example.fintracker;

public class TransactionListItem {
    private final String dateHeader;
    private final Transaction transaction;
    private final boolean isHeader;

    public TransactionListItem(String dateHeader) {
        this.dateHeader = dateHeader;
        this.transaction = null;
        this.isHeader = true;
    }

    public TransactionListItem(Transaction transaction) {
        this.dateHeader = null;
        this.transaction = transaction;
        this.isHeader = false;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public String getDateHeader() {
        return dateHeader;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
