package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryProductsActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private List<ProductModel> productList = new ArrayList<>();
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_products);

        categoryName = getIntent().getStringExtra("category_name");
        ((TextView) findViewById(R.id.tvCategoryName)).setText(categoryName);

        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        
        adapter = new ProductAdapter(productList, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.getId());
            intent.putExtra("product_name", product.getName());
            intent.putExtra("product_price", product.getPrice());
            intent.putExtra("product_original_price", product.getOriginalPrice());
            intent.putExtra("product_image", product.getImageUrl());
            intent.putExtra("product_description", product.getDescription());
            startActivity(intent);
        });
        rvProducts.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadProducts();
    }

    private void loadProducts() {
        FirebaseFirestore.getInstance().collection("products")
                .whereEqualTo("category", categoryName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ProductModel product = doc.toObject(ProductModel.class);
                        product.setId(doc.getId());
                        productList.add(product);
                    }
                    if (productList.isEmpty()) {
                        loadSampleProducts();
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> loadSampleProducts());
    }

    private void loadSampleProducts() {
        productList.add(new ProductModel("1", "Sample " + categoryName + " 1", "Description for item 1", 10.99, 15.00, 10, null, false));
        productList.add(new ProductModel("2", "Sample " + categoryName + " 2", "Description for item 2", 20.99, 25.00, 5, null, false));
        adapter.notifyDataSetChanged();
    }
}
