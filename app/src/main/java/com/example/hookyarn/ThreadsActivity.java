package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;

public class ThreadsActivity extends AppCompatActivity {

    RecyclerView recyclerThreads;
    FloatingActionButton btnAddPost;

    FirebaseFirestore db;

    List<ThreadPost> postList;
    ThreadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);

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
                Intent intent = new Intent(ThreadsActivity.this, CreateThreadPostActivity.class);
                intent.putExtra("postId", post.getPostId());
                intent.putExtra("text", post.getText());
                intent.putExtra("imageUrl", post.getImageUrl());
                startActivity(intent);
            }

            @Override
            public void onDelete(ThreadPost post) {
                new androidx.appcompat.app.AlertDialog.Builder(ThreadsActivity.this)
                        .setTitle("Delete Post")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.collection("threads").document(post.getPostId()).delete();
                            Toast.makeText(ThreadsActivity.this, "Post deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onComment(ThreadPost post) {
                Intent intent = new Intent(ThreadsActivity.this, PostDetailActivity.class);
                intent.putExtra("postId", post.getPostId());
                intent.putExtra("collection", "threads");
                startActivity(intent);
            }

            @Override
            public void onMessage(ThreadPost post) {
                Intent intent = new Intent(ThreadsActivity.this, ChatActivity.class);
                intent.putExtra("receiverId", post.getUserId());
                startActivity(intent);
            }
        });

        recyclerThreads.setLayoutManager(
                new LinearLayoutManager(this));

        recyclerThreads.setAdapter(adapter);

        loadPosts();

        btnAddPost.setOnClickListener(v -> {
            startActivity(new Intent(
                    ThreadsActivity.this,
                    CreateThreadPostActivity.class
            ));
        });
    }

    private void loadPosts() {
        db.collection("threads")
                .orderBy("createdAt",
                        Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (value != null) {
                        postList.clear();
                        for(DocumentSnapshot doc : value.getDocuments()){
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