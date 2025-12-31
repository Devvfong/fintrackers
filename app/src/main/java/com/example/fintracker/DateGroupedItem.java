package com.example.fintracker;

import androidx.annotation.Nullable;

public class DateGroupedItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TRANSACTION = 1;

    private final int viewType;
    private final String headerTitle;       // for TYPE_HEADER
    private final Transaction transaction;  // for TYPE_TRANSACTION

    public DateGroupedItem(int viewType,
                           @Nullable String headerTitle,
                           @Nullable Transaction transaction) {
        this.viewType = viewType;
        this.headerTitle = headerTitle;
        this.transaction = transaction;
    }

    public int getViewType() {
        return viewType;
    }

    @Nullable
    public String getHeaderTitle() {
        return headerTitle;
    }

    @Nullable
    public Transaction getTransaction() {
        return transaction;
    }
}
