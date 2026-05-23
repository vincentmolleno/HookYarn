package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class thread extends AppCompatActivity {

    private RecyclerView recyclerThreads;
    private FloatingActionButton btnAddPost;
    private FirebaseFirestore db;
    private List<ThreadPost> postList;
    private ThreadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_thread);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerThreads = findViewById(R.id.recyclerThreads);
        btnAddPost = findViewById(R.id.btnAddPost);

        db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
        db.setFirestoreSettings(settings);

        postList = new ArrayList<>();
        adapter = new ThreadAdapter(this, postList, new ArrayList<>(), new ThreadAdapter.OnThreadAction() {
            @Override
            public void onEdit(ThreadPost post) {
                Intent intent = new Intent(thread.this, CreateThreadPostActivity.class);
                intent.putExtra("postId", post.getPostId());
                intent.putExtra("text", post.getText());
                intent.putExtra("imageUrl", post.getImageUrl());
                startActivity(intent);
            }

            @Override
            public void onDelete(ThreadPost post) {
                new androidx.appcompat.app.AlertDialog.Builder(thread.this)
                        .setTitle("Delete Post")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.collection("threads").document(post.getPostId()).delete();
                            Toast.makeText(thread.this, "Post deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onComment(ThreadPost post) {
                Intent intent = new Intent(thread.this, PostDetailActivity.class);
                intent.putExtra("postId", post.getPostId());
                startActivity(intent);
            }

            @Override
            public void onMessage(ThreadPost post) {
                Intent intent = new Intent(thread.this, ChatActivity.class);
                intent.putExtra("receiverId", post.getUserId());
                startActivity(intent);
            }
        });

        recyclerThreads.setLayoutManager(new LinearLayoutManager(this));
        recyclerThreads.setAdapter(adapter);

        btnAddPost.setOnClickListener(v -> {
            startActivity(new Intent(thread.this, CreateThreadPostActivity.class));
        });

        loadPosts();
    }

    private void loadPosts() {
        db.collection("threads")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (value != null) {
                        postList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ThreadPost post = doc.toObject(ThreadPost.class);
                            if (post != null) {
                                postList.add(post);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
