package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private String productId, productName, productImage, productDescription;
    private double productPrice, productOriginalPrice;
    private int productImageRes;
    private int quantity = 1;
    private TextView tvQuantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Get data from intent
        productId = getIntent().getStringExtra("product_id");
        productName = getIntent().getStringExtra("product_name");
        productPrice = getIntent().getDoubleExtra("product_price", 0.0);
        productOriginalPrice = getIntent().getDoubleExtra("product_original_price", 0.0);
        productImage = getIntent().getStringExtra("product_image");
        productImageRes = getIntent().getIntExtra("product_image_res", 0);
        productDescription = getIntent().getStringExtra("product_description");

        initViews();
    }

    private void initViews() {
        TextView tvName = findViewById(R.id.tvProductName);
        TextView tvPrice = findViewById(R.id.tvPrice);
        TextView tvDesc = findViewById(R.id.tvDescription);
        ImageView imgProduct = findViewById(R.id.imgProduct);
        ImageView btnBack = findViewById(R.id.btnBack);
        
        tvQuantity = findViewById(R.id.tvQuantity);
        ImageView btnMinus = findViewById(R.id.btnMinus);
        ImageView btnPlus = findViewById(R.id.btnPlus);

        tvName.setText(productName);
        tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", productPrice));
        if (productDescription != null) tvDesc.setText(productDescription);

        if (productImage != null && !productImage.isEmpty()) {
            Glide.with(this).load(productImage).into(imgProduct);
        } else if (productImageRes != 0) {
            imgProduct.setImageResource(productImageRes);
        }

        btnBack.setOnClickListener(v -> finish());
        
        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });
        
        btnPlus.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            ProductModel product = new ProductModel(productId, productName, productDescription, productPrice, productOriginalPrice, 100, productImage, false);
            product.setImageResId(productImageRes);
            CartManager.getInstance(this).addToCart(product, quantity);
            
            // Log for debugging
            android.util.Log.d("ProductDetail", "Added to cart: " + productName + " (ID: " + productId + ")");

            Toast.makeText(this, "Added " + quantity + " to cart!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });

        findViewById(R.id.btnBuyNow).setOnClickListener(v -> {
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra("product_id", productId);
            intent.putExtra("product_name", productName);
            intent.putExtra("product_price", productPrice);
            intent.putExtra("product_image", productImage);
            intent.putExtra("product_image_res", productImageRes);
            intent.putExtra("product_quantity", quantity);
            startActivity(intent);
        });
    }
}
