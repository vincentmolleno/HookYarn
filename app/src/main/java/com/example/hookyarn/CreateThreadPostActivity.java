package com.example.hookyarn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class CreateThreadPostActivity extends AppCompatActivity {

    private static final String TAG = "CreateThreadPost";

    EditText edtPost;
    Button btnPost;
    ProgressBar progressBar;

    String existingPostId;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_thread_post);

        edtPost = findViewById(R.id.edtPost);
        progressBar = findViewById(R.id.progressBar);
        btnPost = findViewById(R.id.btnPost);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        existingPostId = getIntent().getStringExtra("postId");
        if (existingPostId != null) {
            edtPost.setText(getIntent().getStringExtra("text"));
            btnPost.setText("Update Post");
        }

        btnPost.setOnClickListener(v -> uploadPost());
    }

    private void uploadPost(){
        String postText = edtPost.getText().toString().trim();
        if(postText.isEmpty()){
            Toast.makeText(this, "Write something", Toast.LENGTH_SHORT).show();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to post", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnPost.setEnabled(false);

        savePost(postText, "");
    }

    private void savePost(String text, String imageUrl) {
        String uid = FirebaseAuth.getInstance().getUid();
        
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String username = doc.exists() ? doc.getString("username") : null;
            if (username == null || username.isEmpty()) {
                username = "user" + (uid != null && uid.length() >= 5 ? uid.substring(0, 5) : "XXXXX");
            }

            String postId = existingPostId != null ? existingPostId : db.collection("threads").document().getId();
            String profileImageUrl = doc.getString("profileImageUrl");

            HashMap<String, Object> map = new HashMap<>();
            map.put("id", postId);
            map.put("uid", uid);
            map.put("username", username);
            map.put("profileImageUrl", profileImageUrl);
            map.put("caption", text);
            map.put("imageUrl", "");
            
            if (existingPostId == null) {
                map.put("likeCount", 0);
                map.put("commentCount", 0);
                map.put("likedBy", new java.util.ArrayList<String>());
                map.put("createdAt", Timestamp.now());
            }

            db.collection("threads").document(postId).set(map, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, existingPostId != null ? "Post updated!" : "Post uploaded!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnPost.setEnabled(true);
                        Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
