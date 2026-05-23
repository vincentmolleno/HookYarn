package com.example.hookyarn;

public class CartItem {
    private String productId;
    private ProductModel product;
    private int quantity;

    public CartItem() {
    }

    public CartItem(ProductModel product, int quantity) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
        this.quantity = quantity;
    }

    public String getProductId() {
        if (productId == null && product != null) {
            productId = product.getId();
        }
        return productId;
    }

    public ProductModel getProduct() {
        return product;
    }

    public void setProduct(ProductModel product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}