package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ThreadFragment extends Fragment implements HomeActivity.SearchableFragment {

    private RecyclerView recyclerView;
    private ThreadAdapter adapter;
    private List<ThreadPost> list;
    private FirebaseFirestore db;
    private String lastSearchQuery = "";

    @Override
    public void onSearch(String query) {
        lastSearchQuery = query;
        if (query.isEmpty()) {
            loadPosts();
            return;
        }

        db.collection("threads")
                .whereGreaterThanOrEqualTo("caption", query)
                .whereLessThanOrEqualTo("caption", query + "\uf8ff")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!query.equals(lastSearchQuery)) return;
                    list.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        ThreadPost post = doc.toObject(ThreadPost.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            list.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_thread, container, false);

        recyclerView = view.findViewById(R.id.recyclerThreads);
        FloatingActionButton btnAddPost = view.findViewById(R.id.btnAddPost);

        db = FirebaseFirestore.getInstance();
        
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
        db.setFirestoreSettings(settings);

        list = new ArrayList<>();
        List<String> followingIds = new ArrayList<>();
        
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            db.collection("users").document(currentUid).addSnapshotListener((doc, error) -> {
                if (doc != null && doc.exists()) {
                    List<String> following = (List<String>) doc.get("following");
                    followingIds.clear();
                    if (following != null) {
                        followingIds.addAll(following);
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        adapter = new ThreadAdapter(getContext(), list, followingIds, new ThreadAdapter.OnThreadAction() {
            @Override
            public void onEdit(ThreadPost post) {
                Intent intent = new Intent(getContext(), CreateThreadPostActivity.class);
                intent.putExtra("postId", post.getPostId());
                intent.putExtra("text", post.getText());
                intent.putExtra("imageUrl", post.getImageUrl());
                startActivity(intent);
            }

            @Override
            public void onDelete(ThreadPost post) {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("Delete Post")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.collection("threads").document(post.getPostId()).delete();
                            db.collection("posts").document(post.getPostId()).delete();
                            Toast.makeText(getContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onComment(ThreadPost post) {
                Intent intent = new Intent(getContext(), PostDetailActivity.class);
                intent.putExtra("postId", post.getPostId());
                startActivity(intent);
            }

            @Override
            public void onMessage(ThreadPost post) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("receiverId", post.getUserId());
                startActivity(intent);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        btnAddPost.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), CreateThreadPostActivity.class));
        });

        loadPosts();

        return view;
    }

    private void loadPosts() {
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        db.collection("users").document(currentUid).get().addOnSuccessListener(userDoc -> {
            List<String> following = (List<String>) userDoc.get("following");
            List<String> queryUids = new ArrayList<>();
            queryUids.add(currentUid);
            if (following != null) {
                queryUids.addAll(following);
            }

            List<String> limitedUids = queryUids.size() > 30 ? queryUids.subList(0, 30) : queryUids;

            db.collection("threads")
                    .whereIn("uid", limitedUids)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            if (getContext() != null) {
                                android.util.Log.e("FirestoreError", error.getMessage());
                                loadGlobalThreadsFallback();
                            }
                            return;
                        }

                        if (value != null) {
                            list.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                ThreadPost post = doc.toObject(ThreadPost.class);
                                if (post != null) {
                                    post.setPostId(doc.getId());
                                    list.add(post);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    });
        });
    }

    private void loadGlobalThreadsFallback() {
        db.collection("threads")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    list.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ThreadPost post = doc.toObject(ThreadPost.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            list.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}