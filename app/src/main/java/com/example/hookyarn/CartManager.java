package com.example.hookyarn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String TAG = "CartManager";
    private static CartManager instance;
    private List<CartItem> cartItems;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    private CartManager(Context context) {
        sharedPreferences = context.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE);
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();
        cartItems = new ArrayList<>();
        loadCart();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context.getApplicationContext());
        }
        return instance;
    }

    public void loadCart() {
        try {
            String json = sharedPreferences.getString("cart", "[]");
            Log.d(TAG, "loadCart: JSON read: " + json);
            
            Type type = new TypeToken<List<CartItem>>() {}.getType();
            List<CartItem> loadedItems = gson.fromJson(json, type);
            
            if (loadedItems != null && !loadedItems.isEmpty()) {
                cartItems = new ArrayList<>(loadedItems);
                Log.d(TAG, "loadCart: Successfully loaded " + cartItems.size() + " items");
            } else if (cartItems == null || cartItems.isEmpty()) {
                cartItems = new ArrayList<>();
                Log.d(TAG, "loadCart: Cart is empty or could not be loaded");
            } else {
                Log.d(TAG, "loadCart: Keeping " + cartItems.size() + " items already in memory");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cart from storage", e);
            if (cartItems == null) cartItems = new ArrayList<>();
        }
    }

    private void saveCart() {
        try {
            String json = gson.toJson(cartItems);
            sharedPreferences.edit().putString("cart", json).commit();
            Log.d(TAG, "saveCart: Saved " + cartItems.size() + " items. JSON: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error saving cart to storage", e);
        }
    }

    public void addToCart(ProductModel product, int quantity) {
        if (product == null) return;
        
        Log.d(TAG, "addToCart: Adding " + product.getName() + " x" + quantity);

        if (cartItems == null) cartItems = new ArrayList<>();

        boolean found = false;
        for (CartItem item : cartItems) {
            if (item.getProductId() != null && item.getProductId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                found = true;
                break;
            }
        }
        
        if (!found) {
            cartItems.add(new CartItem(product, quantity));
        }

        saveCart();
    }

    public void removeFromCart(String productId) {
        cartItems.removeIf(item -> item.getProductId() != null && item.getProductId().equals(productId));
        saveCart();
    }

    public void updateQuantity(String productId, int quantity) {
        if (productId == null) return;
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            if (productId.equals(item.getProductId())) {
                if (quantity <= 0) {
                    cartItems.remove(i);
                } else {
                    item.setQuantity(quantity);
                }
                saveCart();
                return;
            }
        }
    }

    public List<CartItem> getCartItems() {
        if (cartItems == null) return new ArrayList<>();
        Log.d(TAG, "getCartItems: Returning " + cartItems.size() + " items");
        return new ArrayList<>(cartItems);
    }

    public int getCartItemCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            if (item.getProduct() != null) {
                total += item.getProduct().getPrice() * item.getQuantity();
            }
        }
        return total;
    }

    public void clearCart() {
        cartItems.clear();
        saveCart();
    }
}