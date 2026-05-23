package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemChangeListener {

    private RecyclerView rvCart;
    private CartAdapter adapter;
    private CartManager cartManager;
    private TextView tvTotalPrice;
    private View layoutEmptyCart, layoutBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartManager = CartManager.getInstance(this);

        initViews();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cartManager.loadCart();
        updateUI();
    }

    private void initViews() {
        rvCart = findViewById(R.id.rvCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        layoutBottom = findViewById(R.id.layoutBottom);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnClearCart).setOnClickListener(v -> {
            if (cartManager.getCartItems().isEmpty()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Clear Cart")
                    .setMessage("Are you sure you want to remove all items from your cart?")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        cartManager.clearCart();
                        updateUI();
                        Toast.makeText(this, "Cart cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            List<CartItem> items = cartManager.getCartItems();
            if (!items.isEmpty()) {
                Intent intent = new Intent(this, CheckoutActivity.class);
                CartItem firstItem = items.get(0);
                intent.putExtra("product_name", firstItem.getProduct().getName());
                intent.putExtra("product_price", cartManager.getTotalPrice());
                intent.putExtra("product_image", firstItem.getProduct().getImageUrl());
                intent.putExtra("product_image_res", firstItem.getProduct().getImageResId());
                startActivity(intent);
            }
        });

        findViewById(R.id.btnStartShopping).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(cartManager.getCartItems(), this);
        rvCart.setAdapter(adapter);
    }

    private void updateUI() {
        List<CartItem> items = cartManager.getCartItems();
        if (items.isEmpty()) {
            Log.d("CartActivity", "updateUI: Cart is empty");
            layoutEmptyCart.setVisibility(View.VISIBLE);
            layoutBottom.setVisibility(View.GONE);
            rvCart.setVisibility(View.GONE);
        } else {
            Log.d("CartActivity", "updateUI: Showing " + items.size() + " items");
            layoutEmptyCart.setVisibility(View.GONE);
            layoutBottom.setVisibility(View.VISIBLE);
            rvCart.setVisibility(View.VISIBLE);
            adapter.updateItems(items);
            tvTotalPrice.setText(String.format(Locale.getDefault(), "$%.2f", cartManager.getTotalPrice()));
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        cartManager.updateQuantity(item.getProductId(), newQuantity);
        updateUI();
    }

    @Override
    public void onItemDeleted(CartItem item) {
        cartManager.removeFromCart(item.getProductId());
        updateUI();
    }
}
