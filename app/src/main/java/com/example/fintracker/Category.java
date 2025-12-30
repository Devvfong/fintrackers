package com.example.fintrack.models;

public class Category {
    private String name;
    private String type; // "income" or "expense"
    private int iconResId;
    private String colorHex;

    public Category(String name, String type, int iconResId, String colorHex) {
        this.name = name;
        this.type = type;
        this.iconResId = iconResId;
        this.colorHex = colorHex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}