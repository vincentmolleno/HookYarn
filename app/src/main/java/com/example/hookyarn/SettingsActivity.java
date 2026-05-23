package com.example.hookyarn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "HookYarnPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupSwitches();
        setupClickables();
        setupLogout();
        loadSettingsUI();
    }

    private void loadSettingsUI() {
        ((android.widget.TextView)findViewById(R.id.tvMeasurement)).setText(prefs.getString("measurement_unit", "Metric (cm/mm)"));
        ((android.widget.TextView)findViewById(R.id.tvHookStandard)).setText(prefs.getString("hook_standard", "Metric (mm)"));
        ((android.widget.TextView)findViewById(R.id.tvYarnStandard)).setText(prefs.getString("yarn_standard", "Standard (0-7)"));
    }

    private void setupSwitches() {
        androidx.appcompat.widget.SwitchCompat switchStitchTerm = findViewById(R.id.switchStitchTerm);
        switchStitchTerm.setChecked(prefs.getBoolean("us_terms", true));
        switchStitchTerm.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("us_terms", isChecked).apply();
            Toast.makeText(this, "Terminology updated", Toast.LENGTH_SHORT).show();
        });

        androidx.appcompat.widget.SwitchCompat switchDarkMode = findViewById(R.id.switchDarkMode);
        switchDarkMode.setVisibility(android.view.View.GONE);
        if (switchDarkMode.getParent() instanceof android.view.View) {
            ((android.view.View)switchDarkMode.getParent()).setVisibility(android.view.View.GONE);
        }
    }

    private void setupClickables() {
        findViewById(R.id.btnMeasurement).setOnClickListener(v -> {
            showSelectionDialog("Measurement Units", new String[]{"Metric (cm/mm)", "Imperial (in/yd)"}, "measurement_unit");
        });

        findViewById(R.id.btnHookStandard).setOnClickListener(v -> {
            showSelectionDialog("Hook Size Standard", new String[]{"Metric (mm)", "US (Letter/Number)", "UK (Old)"}, "hook_standard");
        });

        findViewById(R.id.btnYarnStandard).setOnClickListener(v -> {
            showSelectionDialog("Yarn Weight Standard", new String[]{"Standard (0-7)", "UK/AU (Ply)", "WPI"}, "yarn_standard");
        });

        findViewById(R.id.btnBackup).setOnClickListener(v -> {
            Toast.makeText(this, "Cloud sync started...", Toast.LENGTH_SHORT).show();
        });
    }

    private void showSelectionDialog(String title, String[] options, String prefKey) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    prefs.edit().putString(prefKey, options[which]).apply();
                    Toast.makeText(this, title + " updated to " + options[which], Toast.LENGTH_SHORT).show();
                    loadSettingsUI();
                })
                .show();
    }

    private void setupLogout() {
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}