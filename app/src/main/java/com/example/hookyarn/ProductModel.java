package com.example.hookyarn;

import java.util.Date;

public class ProductModel {
    private String id;
    private String name;
    private String description;
    private double price;
    private double originalPrice;
    private int stock;
    private String imageUrl;
    private int imageResId;
    private boolean featured;
    private String categoryId;
    private Date createdAt;

    public ProductModel() {}

    public ProductModel(String id, String name, String description, double price, double originalPrice, int stock, String imageUrl, boolean featured) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.featured = featured;
    }

    public ProductModel(String id, String name, String description, double price, double originalPrice, int stock, int imageResId, boolean featured) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.imageResId = imageResId;
        this.featured = featured;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}