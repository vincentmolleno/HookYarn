package com.example.hookyarn;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private EditText etFullName, etAddress, etPhone;
    private TextView tvProductName, tvProductPrice, tvSubtotal, tvShippingFee, tvTotal;
    private ImageView imgProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        initViews();
        loadOrderData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPlaceOrder).setOnClickListener(v -> placeOrder());
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        tvProductName = findViewById(R.id.tvProductName);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvTotal = findViewById(R.id.tvTotal);
        imgProduct = findViewById(R.id.imgProduct);
    }

    private void loadOrderData() {
        String name = getIntent().getStringExtra("product_name");
        double price = getIntent().getDoubleExtra("product_price", 0.0);
        String imageUrl = getIntent().getStringExtra("product_image");
        int imageRes = getIntent().getIntExtra("product_image_res", 0);
        int quantity = getIntent().getIntExtra("product_quantity", 1);

        if (name == null) {
            CartManager cm = CartManager.getInstance(this);
            tvProductName.setText("Cart Items (" + cm.getCartItemCount() + ")");
            double subtotal = cm.getTotalPrice();
            tvSubtotal.setText(String.format(Locale.getDefault(), "$%.2f", subtotal));
            tvProductPrice.setText("Mixed Items");
            double shipping = 5.00;
            tvShippingFee.setText(String.format(Locale.getDefault(), "$%.2f", shipping));
            tvTotal.setText(String.format(Locale.getDefault(), "$%.2f", subtotal + shipping));
        } else {
            tvProductName.setText(name + (quantity > 1 ? " x" + quantity : ""));
            tvProductPrice.setText(String.format(Locale.getDefault(), "$%.2f", price));
            double subtotal = price * quantity;
            tvSubtotal.setText(String.format(Locale.getDefault(), "$%.2f", subtotal));
            double shipping = 5.00;
            tvShippingFee.setText(String.format(Locale.getDefault(), "$%.2f", shipping));
            tvTotal.setText(String.format(Locale.getDefault(), "$%.2f", subtotal + shipping));
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(imgProduct);
        } else if (imageRes != 0) {
            imgProduct.setImageResource(imageRes);
        }
    }

    private void placeOrder() {
        String name = etFullName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all shipping details", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Order Placed Successfully")
                .setMessage("Thank you for your purchase, " + name + "! Your order will be delivered to " + address)
                .setPositiveButton("OK", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
