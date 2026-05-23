package com.example.hookyarn;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class StoryViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_view);

        ImageView ivStoryFull = findViewById(R.id.ivStoryFull);
        ImageView ivAvatar = findViewById(R.id.ivStoryUserAvatar);
        TextView tvUsername = findViewById(R.id.tvStoryUsername);
        ImageButton btnClose = findViewById(R.id.btnStoryClose);

        String storyImageUrl = getIntent().getStringExtra("storyImageUrl");
        String username = getIntent().getStringExtra("username");
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");

        tvUsername.setText(username);

        Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_account_circle)
                .into(ivAvatar);

        Glide.with(this)
                .load(storyImageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(ivStoryFull);

        btnClose.setOnClickListener(v -> finish());
        
        ivStoryFull.postDelayed(this::finish, 5000);
    }
}