package com.example.hookyarn;

public class CategoryModel {
    private String id;
    private String name;
    private int iconRes;
    private String iconUrl;
    private int order;

    public CategoryModel() {}

    public CategoryModel(String id, String name, int iconRes) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
    }

    public CategoryModel(String id, String name, String iconUrl) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}