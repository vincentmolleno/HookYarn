package com.example.hookyarn;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;

    private ImageView btnBack;
    private ImageView ivPostImage;
    private TextView tvUsername;
    private TextView tvTimestamp;
    private TextView tvDescription;
    private TextView tvLikes;
    private TextView tvComments;
    private RecyclerView rvComments;
    private android.widget.EditText etComment;
    private android.widget.Button btnSendComment;
    private MaterialButton btnFollow;

    private CommentAdapter commentAdapter;
    private List<CommentModel> commentList;
    private String currentPostId;
    private String sourceCollection = "posts";
    private ListenerRegistration followListener;

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid() != null ? FirebaseAuth.getInstance().getUid() : "";

        bindViews();

        String postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getIntent().hasExtra("collection")) {
            sourceCollection = getIntent().getStringExtra("collection");
        }

        loadPost(postId);

        btnBack.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        ivPostImage = findViewById(R.id.ivPostImage);
        tvUsername = findViewById(R.id.tvUsername);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvDescription = findViewById(R.id.tvDescription);
        tvLikes = findViewById(R.id.tvLikes);
        tvComments = findViewById(R.id.tvComments);
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        btnFollow = findViewById(R.id.btnFollow);
        
        btnSendComment.setOnClickListener(v -> postComment());
    }

    private void postComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty() || TextUtils.isEmpty(currentUid)) return;
        
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            String username = doc.getString("username");
            if (username == null) username = "user" + (currentUid.length() >= 5 ? currentUid.substring(0, 5) : "XXXXX");
            
            String cid = db.collection(sourceCollection).document(currentPostId).collection("comments").document().getId();
            CommentModel comment = new CommentModel(cid, currentPostId, currentUid, username, text);
            
            db.collection(sourceCollection).document(currentPostId).collection("comments").document(cid).set(comment)
                .addOnSuccessListener(aVoid -> {
                    db.collection(sourceCollection).document(currentPostId).update("commentCount", FieldValue.increment(1));
                    etComment.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show());
        });
    }

    private void loadPost(String postId) {
        this.currentPostId = postId;
        
        if (getIntent().hasExtra("collection")) {
            db.collection(sourceCollection).document(postId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    PostModel post = doc.toObject(PostModel.class);
                    if (post != null) bindPostData(post);
                } else {
                    Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            return;
        }

        db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                sourceCollection = "posts";
                PostModel post = doc.toObject(PostModel.class);
                if (post != null) bindPostData(post);
            } else {
                db.collection("threads").document(postId).get().addOnSuccessListener(threadDoc -> {
                    if (threadDoc.exists()) {
                        sourceCollection = "threads";
                        PostModel post = threadDoc.toObject(PostModel.class);
                        if (post != null) bindPostData(post);
                    } else {
                        Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }

    private void bindPostData(PostModel post) {
        tvUsername.setText(post.getUsername() != null ? post.getUsername() : "Anonymous");
        tvDescription.setText(post.getCaption() != null ? post.getCaption() : "");
        tvLikes.setText(post.getLikeCount() + " likes");
        tvComments.setText(post.getCommentCount() + " comments");

        if (post.getCreatedAt() != null) {
            tvTimestamp.setText(DATE_FMT.format(post.getCreatedAt().toDate()));
        }

        if (!TextUtils.isEmpty(post.getImageUrl())) {
            ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(post.getImageUrl())
                    .placeholder(R.drawable.ic_yarn)
                    .into(ivPostImage);
        } else {
            ivPostImage.setVisibility(View.GONE);
        }

        String postUid = post.getUid();
        boolean isOwnPost = !currentUid.isEmpty() && postUid != null && currentUid.equals(postUid);

        if (!isOwnPost && postUid != null) {
            btnFollow.setVisibility(View.VISIBLE);
            setupFollowListener(postUid);
        } else {
            btnFollow.setVisibility(View.GONE);
        }

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        
        attachCommentListener();
    }

    private void setupFollowListener(String targetUid) {
        if (followListener != null) followListener.remove();
        if (currentUid.isEmpty()) return;

        followListener = db.collection("users").document(currentUid).addSnapshotListener((doc, error) -> {
            if (doc == null || !doc.exists()) return;
            
            List<String> following = (List<String>) doc.get("following");
            boolean isFollowing = following != null && following.contains(targetUid);
            btnFollow.setText(isFollowing ? "Following" : "Follow");
            
            btnFollow.setOnClickListener(v -> {
                if (isFollowing) {
                    db.collection("users").document(currentUid).update("following", FieldValue.arrayRemove(targetUid));
                    db.collection("users").document(targetUid).update("followers", FieldValue.arrayRemove(currentUid));
                } else {
                    db.collection("users").document(currentUid).update("following", FieldValue.arrayUnion(targetUid));
                    db.collection("users").document(targetUid).update("followers", FieldValue.arrayUnion(currentUid));
                }
            });
        });
    }

    private void attachCommentListener() {
        db.collection(sourceCollection).document(currentPostId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    commentList.clear();
                    for (DocumentSnapshot d : value.getDocuments()) {
                        CommentModel c = d.toObject(CommentModel.class);
                        if (c != null) commentList.add(c);
                    }
                    commentAdapter.notifyDataSetChanged();
                    tvComments.setText(commentList.size() + " comments");
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (followListener != null) followListener.remove();
    }
}