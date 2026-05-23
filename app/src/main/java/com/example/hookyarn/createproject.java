package com.example.hookyarn;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class createproject extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bottom_sheet_create_options);

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("Create New Project");
    }
}