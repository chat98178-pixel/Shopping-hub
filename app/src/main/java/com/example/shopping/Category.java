package com.example.shopping;

public class Category {
    private String name;
    private int iconRes;
    private int colorRes;

    public Category(String name, int iconRes, int colorRes) {
        this.name = name;
        this.iconRes = iconRes;
        this.colorRes = colorRes;
    }

    public String getName() {
        return name;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getColorRes() {
        return colorRes;
    }
}