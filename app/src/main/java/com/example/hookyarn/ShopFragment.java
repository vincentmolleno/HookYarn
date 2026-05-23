package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ShopFragment extends Fragment implements HomeActivity.SearchableFragment {

    private ViewPager2 vpBanner;
    private RecyclerView rvCategories;
    private RecyclerView rvFeaturedProducts;
    private RecyclerView rvRecommendedProducts;

    private BannerAdapter bannerAdapter;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter featuredAdapter;
    private ProductAdapter recommendedAdapter;

    private List<BannerModel> bannerList = new ArrayList<>();
    private List<CategoryModel> categoryList = new ArrayList<>();
    private List<ProductModel> featuredProducts = new ArrayList<>();
    private List<ProductModel> recommendedProducts = new ArrayList<>();
    private List<ProductModel> allProducts = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ActivityResultLauncher<Intent> shopActivityResultLauncher;

    private int cartItemCount = 0;

    private android.widget.ProgressBar pbShop;
    private android.widget.TextView tvEmptyShop;
    private String lastSearchQuery = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shopActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateCartCount()
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews(view);
        setupClickListeners();
        loadBanners();
        loadCategories();
        loadFeaturedProducts();
        loadRecommendedProducts();
        updateCartCount();

        return view;
    }

    private void initViews(View view) {
        vpBanner = view.findViewById(R.id.vpBanner);
        rvCategories = view.findViewById(R.id.rvCategories);
        rvFeaturedProducts = view.findViewById(R.id.rvFeaturedProducts);
        rvRecommendedProducts = view.findViewById(R.id.rvRecommendedProducts);
        pbShop = view.findViewById(R.id.pbShop);
        tvEmptyShop = view.findViewById(R.id.tvEmptyShop);

        bannerAdapter = new BannerAdapter(bannerList, this);
        vpBanner.setAdapter(bannerAdapter);

        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            filterProductsByCategory(category);
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        featuredAdapter = new ProductAdapter(featuredProducts, product -> {
            openProductDetail(product);
        });
        rvFeaturedProducts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedProducts.setAdapter(featuredAdapter);

        recommendedAdapter = new ProductAdapter(recommendedProducts, product -> {
            openProductDetail(product);
        });
        rvRecommendedProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvRecommendedProducts.setAdapter(recommendedAdapter);
    }

    private void setupClickListeners() {
    }

    @Override
    public void onSearch(String query) {
        lastSearchQuery = query;
        if (query.isEmpty()) {
            if (pbShop != null) pbShop.setVisibility(View.GONE);
            loadRecommendedProducts();
            return;
        }

        if (pbShop != null) pbShop.setVisibility(View.VISIBLE);
        if (tvEmptyShop != null) tvEmptyShop.setVisibility(View.GONE);

        db.collection("products")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!query.equals(lastSearchQuery)) return;
                    
                    if (pbShop != null) pbShop.setVisibility(View.GONE);
                    recommendedProducts.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ProductModel product = doc.toObject(ProductModel.class);
                        product.setId(doc.getId());
                        recommendedProducts.add(product);
                    }
                    recommendedAdapter.notifyDataSetChanged();
                    
                    if (recommendedProducts.isEmpty()) {
                        if (tvEmptyShop != null) {
                            tvEmptyShop.setVisibility(View.VISIBLE);
                            tvEmptyShop.setText("No products found for \"" + query + "\"");
                        }
                    } else {
                        if (tvEmptyShop != null) tvEmptyShop.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!query.equals(lastSearchQuery)) return;
                    if (pbShop != null) pbShop.setVisibility(View.GONE);
                    android.widget.Toast.makeText(getContext(), "Search failed", android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void filterProductsByCategory(CategoryModel category) {
        Intent intent = new Intent(getContext(), CategoryProductsActivity.class);
        intent.putExtra("category_id", category.getId());
        intent.putExtra("category_name", category.getName());
        startActivity(intent);
    }

    private void loadBanners() {
        bannerList.add(new BannerModel("https://example.com/banner1.jpg", "Spring Sale", "Up to 50% off"));
        bannerList.add(new BannerModel("https://example.com/banner2.jpg", "New Arrivals", "Latest yarn collections"));
        bannerList.add(new BannerModel("https://example.com/banner3.jpg", "Free Shipping", "On orders over $50"));
        bannerAdapter.notifyDataSetChanged();
    }

    private void loadCategories() {
        if (pbShop != null) pbShop.setVisibility(View.VISIBLE);
        db.collection("categories")
                .orderBy("order")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        loadSampleCategories();
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            CategoryModel category = doc.toObject(CategoryModel.class);
                            category.setId(doc.getId());
                            categoryList.add(category);
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();
                    checkEmptyShop();
                })
                .addOnFailureListener(e -> {
                    loadSampleCategories();
                    checkEmptyShop();
                });
    }

    private void checkEmptyShop() {
        if (pbShop != null) pbShop.setVisibility(View.GONE);
        if (categoryList.isEmpty() && featuredProducts.isEmpty() && recommendedProducts.isEmpty()) {
            if (tvEmptyShop != null) tvEmptyShop.setVisibility(View.VISIBLE);
        } else {
            if (tvEmptyShop != null) tvEmptyShop.setVisibility(View.GONE);
        }
    }

    private void loadSampleCategories() {
        categoryList.add(new CategoryModel("1", "Yarn", R.drawable.ic_yarn));
        categoryList.add(new CategoryModel("2", "Hook", R.drawable.crochet_hook));
        categoryList.add(new CategoryModel("3", "Needles", R.drawable.ic_home));
        categoryList.add(new CategoryModel("4", "Patterns", R.drawable.ic_book));
        categoryList.add(new CategoryModel("5", "Bundles", R.drawable.ic_folder));
        categoryList.add(new CategoryModel("6", "Tools", R.drawable.ic_settings));
        categoryAdapter.notifyDataSetChanged();
    }

    private void loadFeaturedProducts() {
        db.collection("products")
                .whereEqualTo("featured", true)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    featuredProducts.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        loadSampleFeaturedProducts();
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            ProductModel product = doc.toObject(ProductModel.class);
                            product.setId(doc.getId());
                            featuredProducts.add(product);
                        }
                    }
                    featuredAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    loadSampleFeaturedProducts();
                });
    }

    private void loadSampleFeaturedProducts() {
        featuredProducts.add(new ProductModel("1", "Merino Wool Yarn", "Soft merino wool, perfect for sweaters", 12.99, 19.99, 50, R.drawable.ic_product_placeholder, true));
        featuredProducts.add(new ProductModel("2", "Bamboo Needles Set", "Eco-friendly bamboo knitting needles", 24.99, 39.99, 30, R.drawable.ic_product_placeholder, true));
        featuredProducts.add(new ProductModel("3", "Beginner Kit", "Everything you need to start knitting", 39.99, 59.99, 20, R.drawable.ic_product_placeholder, true));
        featuredAdapter.notifyDataSetChanged();
    }

    private void loadRecommendedProducts() {
        db.collection("products")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recommendedProducts.clear();
                    allProducts.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        loadSampleRecommendedProducts();
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            ProductModel product = doc.toObject(ProductModel.class);
                            product.setId(doc.getId());
                            recommendedProducts.add(product);
                            allProducts.add(product);
                        }
                    }
                    recommendedAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    loadSampleRecommendedProducts();
                });
    }

    private void loadSampleRecommendedProducts() {
        recommendedProducts.clear();
        recommendedProducts.add(new ProductModel("4", "Cotton Yarn", "100% cotton, breathable and soft", 8.99, 14.99, 100, R.drawable.ic_product_placeholder, false));
        recommendedProducts.add(new ProductModel("5", "Crochet Hook Set", "Colorful ergonomic hooks", 15.99, 25.99, 45, R.drawable.ic_product_placeholder, false));
        recommendedProducts.add(new ProductModel("6", "Pattern Book", "50 beginner-friendly patterns", 19.99, 29.99, 35, R.drawable.ic_product_placeholder, false));
        recommendedProducts.add(new ProductModel("7", "Stitch Markers", "Cute animal-shaped markers", 4.99, 9.99, 200, R.drawable.ic_product_placeholder, false));
        recommendedAdapter.notifyDataSetChanged();
    }

    private void openProductDetail(ProductModel product) {
        Intent intent = new Intent(getContext(), ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        intent.putExtra("product_name", product.getName());
        intent.putExtra("product_price", product.getPrice());
        intent.putExtra("product_original_price", product.getOriginalPrice());
        intent.putExtra("product_image", product.getImageUrl());
        intent.putExtra("product_image_res", product.getImageResId());
        intent.putExtra("product_description", product.getDescription());
        shopActivityResultLauncher.launch(intent);
    }

    private void updateCartCount() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).updateCartCount();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCartCount();
    }
}