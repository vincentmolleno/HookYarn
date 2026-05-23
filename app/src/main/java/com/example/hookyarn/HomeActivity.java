package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.widget.LinearLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout navHome, navShop, navCreate, navThread, navProfile;
    private android.widget.TextView tvHeaderTitle, tvHeaderCartCount;
    private android.widget.EditText etHeaderSearch;
    private android.widget.ImageView ivHeaderSearch, ivHeaderSettings, ivHeaderCart;
    private android.widget.FrameLayout layoutHeaderCart;
    private android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onResume() {
        super.onResume();
        updateCartCount();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        
        android.util.Log.d("HOOKYARN", "HomeActivity: Layout set");

        navHome = findViewById(R.id.navHome);
        navShop = findViewById(R.id.navShop);
        navCreate = findViewById(R.id.navCreate);
        navThread = findViewById(R.id.navThread);
        navProfile = findViewById(R.id.navProfile);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etHeaderSearch = findViewById(R.id.etHeaderSearch);
        ivHeaderSearch = findViewById(R.id.ivHeaderSearch);
        ivHeaderSettings = findViewById(R.id.ivHeaderSettings);
        layoutHeaderCart = findViewById(R.id.layoutHeaderCart);
        ivHeaderCart = findViewById(R.id.ivHeaderCart);
        tvHeaderCartCount = findViewById(R.id.tvHeaderCartCount);

        ivHeaderSearch.setOnClickListener(v -> {
            if (etHeaderSearch.getVisibility() == android.view.View.GONE) {
                tvHeaderTitle.setVisibility(android.view.View.GONE);
                etHeaderSearch.setVisibility(android.view.View.VISIBLE);
                etHeaderSearch.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etHeaderSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            } else {
                String query = etHeaderSearch.getText().toString().trim();
                if (query.isEmpty()) {
                    hideSearch();
                } else {
                    performSearch(query);
                }
            }
        });

        etHeaderSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> performSearch(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        etHeaderSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                performSearch(etHeaderSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        ivHeaderSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        layoutHeaderCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, CartActivity.class);
            startActivity(intent);
        });

        navHome.setOnClickListener(v -> loadFragment(new Home()));
        navShop.setOnClickListener(v -> loadFragment(new ShopFragment()));
        navCreate.setOnClickListener(v -> showCreateBottomSheet());
        navThread.setOnClickListener(v -> loadFragment(new ThreadFragment()));
        navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment()));

        if (savedInstanceState == null) {
            loadFragment(new Home());
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("SHOW_CREATE_SHEET", false)) {
            getWindow().getDecorView().post(this::showCreateBottomSheet);
            intent.removeExtra("SHOW_CREATE_SHEET");
        }
    }

    private void showCreateBottomSheet() {
        CreateBottomSheetFragment bottomSheet = new CreateBottomSheetFragment();
        bottomSheet.show(getSupportFragmentManager(), "CreateBottomSheet");
    }

    private void loadFragment(Fragment fragment) {
        hideSearch();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
        updateHeader(fragment);
        updateNavSelection(fragment);
    }

    private void updateNavSelection(Fragment fragment) {
        resetNavItem(navHome);
        resetNavItem(navShop);
        resetNavItem(navCreate);
        resetNavItem(navThread);
        resetNavItem(navProfile);

        if (fragment instanceof Home) {
            highlightNavItem(navHome);
        } else if (fragment instanceof ShopFragment) {
            highlightNavItem(navShop);
        } else if (fragment instanceof ThreadFragment) {
            highlightNavItem(navThread);
        } else if (fragment instanceof ProfileFragment) {
            highlightNavItem(navProfile);
        }
    }

    private void resetNavItem(LinearLayout navItem) {
        if (navItem == null) return;
        android.widget.ImageView icon = (android.widget.ImageView) navItem.getChildAt(0);
        android.widget.TextView label = (android.widget.TextView) navItem.getChildAt(1);
        android.view.View indicator = navItem.getChildAt(2);
        
        icon.setColorFilter(android.graphics.Color.parseColor("#8C7F6E"));
        label.setTextColor(android.graphics.Color.parseColor("#8C7F6E"));
        if (indicator != null) indicator.setVisibility(android.view.View.INVISIBLE);
    }

    private void highlightNavItem(LinearLayout navItem) {
        if (navItem == null) return;
        android.widget.ImageView icon = (android.widget.ImageView) navItem.getChildAt(0);
        android.widget.TextView label = (android.widget.TextView) navItem.getChildAt(1);
        android.view.View indicator = navItem.getChildAt(2);
        
        icon.setColorFilter(android.graphics.Color.parseColor("#2B5F6B"));
        label.setTextColor(android.graphics.Color.parseColor("#2B5F6B"));
        if (indicator != null) indicator.setVisibility(android.view.View.VISIBLE);
    }

    private void hideSearch() {
        if (etHeaderSearch != null) {
            etHeaderSearch.setText("");
            etHeaderSearch.setVisibility(android.view.View.GONE);
            tvHeaderTitle.setVisibility(android.view.View.VISIBLE);
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etHeaderSearch.getWindowToken(), 0);
        }
    }

    private void performSearch(String query) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment instanceof SearchableFragment) {
            ((SearchableFragment) currentFragment).onSearch(query);
        } else {
            android.widget.Toast.makeText(this, "Searching for: " + query, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    public interface SearchableFragment {
        void onSearch(String query);
    }

    public void updateCartCount() {
        if (layoutHeaderCart == null || tvHeaderCartCount == null) return;
        
        int count = CartManager.getInstance(this).getCartItemCount();
        if (count > 0) {
            layoutHeaderCart.setVisibility(android.view.View.VISIBLE);
            tvHeaderCartCount.setVisibility(android.view.View.VISIBLE);
            tvHeaderCartCount.setText(String.valueOf(count));
        } else {
            tvHeaderCartCount.setVisibility(android.view.View.GONE);
        }
    }

    private void updateHeader(Fragment fragment) {
        if (tvHeaderTitle == null) return;

        if (layoutHeaderCart != null) layoutHeaderCart.setVisibility(android.view.View.GONE);

        if (fragment instanceof Home) {
            tvHeaderTitle.setText("HOOK/YARN");
            ivHeaderSearch.setVisibility(android.view.View.VISIBLE);
            ivHeaderSettings.setVisibility(android.view.View.GONE);
        } else if (fragment instanceof ProfileFragment) {
            tvHeaderTitle.setText("HOOK/YARN");
            ivHeaderSearch.setVisibility(android.view.View.GONE);
            ivHeaderSettings.setVisibility(android.view.View.VISIBLE);
        } else if (fragment instanceof ShopFragment) {
            tvHeaderTitle.setText("HOOK/YARN SHOP");
            ivHeaderSearch.setVisibility(android.view.View.VISIBLE);
            ivHeaderSettings.setVisibility(android.view.View.GONE);
            if (layoutHeaderCart != null) {
                layoutHeaderCart.setVisibility(android.view.View.VISIBLE);
                updateCartCount();
            }
        } else if (fragment instanceof ThreadFragment) {
            tvHeaderTitle.setText("THREAD");
            ivHeaderSearch.setVisibility(android.view.View.GONE);
            ivHeaderSettings.setVisibility(android.view.View.GONE);
        }
    }
}