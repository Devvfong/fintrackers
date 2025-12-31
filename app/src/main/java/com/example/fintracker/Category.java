package com.example.fintracker;

public class Category {
    private String name;

    // "income" or "expense"

    public Category(String name) {
        this.name = name;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}