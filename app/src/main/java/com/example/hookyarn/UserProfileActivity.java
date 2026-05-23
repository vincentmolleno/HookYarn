package com.example.hookyarn;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    public static final String EXTRA_UID = "targetUid";

    private String targetUid;
    private String currentUid;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private LinearLayout tabGrid, tabFeed, tabProjects;
    private FrameLayout panelGrid, panelFeed, panelProjects;
    private View tabIndicatorGrid, tabIndicatorFeed, tabIndicatorProjects;
    
    private RecyclerView  rvGrid, rvFeed, rvProjects;
    private View          emptyGrid, emptyFeed, emptyProjects;
    private TextView      tvPostCount, tvFollowers, tvFollowing, tvUsername, tvAvatarInitials;
    private ImageView     imgAvatar;
    private com.google.android.material.button.MaterialButton btnFollow;

    private PostGridAdapter gridAdapter;
    private PostFeedAdapter feedAdapter;
    private ProjectAdapter projectAdapter;
    
    private ArrayList<PostModel> postList = new ArrayList<>();
    private ArrayList<ProjectModel> projectList = new ArrayList<>();
    private List<String> followingIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        targetUid = getIntent().getStringExtra(EXTRA_UID);
        if (targetUid == null) {
            targetUid = getIntent().getStringExtra("uid");
        }
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        db = FirebaseFirestore.getInstance();

        if (targetUid == null || targetUid.equals(currentUid)) {
            // If it's the current user, we could either finish or show own profile
            // For now, let's just finish as the user should use ProfileFragment
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupTabListeners();
        setupRecyclerViews();

        loadUserData();
        loadUserPosts();
        loadUserProjects();
    }

    private void initializeViews() {
        tabGrid = findViewById(R.id.tabGrid);
        tabFeed = findViewById(R.id.tabFeed);
        tabProjects = findViewById(R.id.tabProjects);

        panelGrid = findViewById(R.id.panelGrid);
        panelFeed = findViewById(R.id.panelFeed);
        panelProjects = findViewById(R.id.panelProjects);

        tabIndicatorGrid = findViewById(R.id.tabIndicatorGrid);
        tabIndicatorFeed = findViewById(R.id.tabIndicatorFeed);
        tabIndicatorProjects = findViewById(R.id.tabIndicatorProjects);

        rvGrid = findViewById(R.id.rvGrid);
        rvFeed = findViewById(R.id.rvFeed);
        rvProjects = findViewById(R.id.rvProjects);

        emptyGrid = findViewById(R.id.emptyGrid);
        emptyFeed = findViewById(R.id.emptyFeed);
        emptyProjects = findViewById(R.id.emptyProjects);

        tvPostCount = findViewById(R.id.tvPostCount);
        tvFollowers = findViewById(R.id.tvFollowers);
        tvFollowing = findViewById(R.id.tvFollowing);
        tvUsername = findViewById(R.id.tvUsername);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnFollow = findViewById(R.id.btnFollow);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        rvGrid.setLayoutManager(new GridLayoutManager(this, 3));
        gridAdapter = new PostGridAdapter(this, postList, null); // No options for other user's posts
        rvGrid.setAdapter(gridAdapter);

        rvFeed.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new PostFeedAdapter(this, postList, followingIds, new PostFeedAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(PostModel post) {
                android.content.Intent intent = new android.content.Intent(UserProfileActivity.this, PostDetailActivity.class);
                intent.putExtra("postId", post.getId());
                intent.putExtra("collection", "posts");
                startActivity(intent);
            }

            @Override
            public void onOptionsClick(PostModel post) {
                // Usually no options for other user's posts in this context
            }
        });
        rvFeed.setAdapter(feedAdapter);

        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        projectAdapter = new ProjectAdapter(projectList, null);
        rvProjects.setAdapter(projectAdapter);
    }

    private void loadUserData() {
        if (currentUid != null) {
            db.collection("users").document(currentUid).addSnapshotListener((doc, error) -> {
                if (error != null || doc == null || !doc.exists()) return;
                List<String> following = (List<String>) doc.get("following");
                followingIds.clear();
                if (following != null) followingIds.addAll(following);
                if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
            });
        }

        db.collection("users").document(targetUid).addSnapshotListener((doc, error) -> {
            if (error != null || doc == null || !doc.exists()) return;

            String username = doc.getString("username");
            if (username != null) {
                tvUsername.setText(username);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle(username);
            }

            // Stats
            List<String> followers = (List<String>) doc.get("followers");
            List<String> following = (List<String>) doc.get("following");
            tvFollowers.setText(String.valueOf(followers != null ? followers.size() : 0));
            tvFollowing.setText(String.valueOf(following != null ? following.size() : 0));

            // Follow button state
            if (currentUid != null) {
                if (followers != null && followers.contains(currentUid)) {
                    btnFollow.setText("Following");
                    btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF9E9E9E));
                } else {
                    btnFollow.setText("Follow");
                    btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5722));
                }
                btnFollow.setOnClickListener(v -> toggleFollow(followers != null && followers.contains(currentUid)));
            }

            String profileImageUrl = doc.getString("profileImageUrl");
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .load(profileImageUrl)
                        .circleCrop()
                        .into(imgAvatar);
                imgAvatar.setVisibility(View.VISIBLE);
                tvAvatarInitials.setVisibility(View.GONE);
            } else {
                imgAvatar.setVisibility(View.GONE);
                tvAvatarInitials.setVisibility(View.VISIBLE);
            }
        });
    }

    private void toggleFollow(boolean isFollowing) {
        if (currentUid == null || targetUid == null) return;

        if (isFollowing) {
            db.collection("users").document(currentUid)
                    .update("following", FieldValue.arrayRemove(targetUid));
            db.collection("users").document(targetUid)
                    .update("followers", FieldValue.arrayRemove(currentUid));
        } else {
            db.collection("users").document(currentUid)
                    .update("following", FieldValue.arrayUnion(targetUid));
            db.collection("users").document(targetUid)
                    .update("followers", FieldValue.arrayUnion(currentUid));
        }
    }

    private void loadUserPosts() {
        db.collection("posts")
                .whereEqualTo("uid", targetUid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    postList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            // Only include regular posts, not thread posts if they are in the same collection
                            // Assuming regular posts don't have a 'isThread' field or similar
                            Boolean isThread = doc.getBoolean("isThread");
                            if (isThread != null && isThread) continue;

                            PostModel post = doc.toObject(PostModel.class);
                            post.setId(doc.getId());
                            postList.add(post);
                        }
                    }
                    java.util.Collections.sort(postList, (p1, p2) -> {
                        if (p1.getCreatedAt() == null || p2.getCreatedAt() == null) return 0;
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    });
                    gridAdapter.notifyDataSetChanged();
                    feedAdapter.notifyDataSetChanged();
                    tvPostCount.setText(String.valueOf(postList.size()));
                    updateEmptyStates();
                });
    }

    private void loadUserProjects() {
        db.collection("projects")
                .whereEqualTo("uid", targetUid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    projectList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            ProjectModel model = doc.toObject(ProjectModel.class);
                            model.setId(doc.getId());
                            projectList.add(model);
                        }
                    }
                    projectAdapter.notifyDataSetChanged();
                    updateEmptyStates();
                });
    }

    private void updateEmptyStates() {
        boolean hasPosts = !postList.isEmpty();
        rvGrid.setVisibility(hasPosts ? View.VISIBLE : View.GONE);
        emptyGrid.setVisibility(hasPosts ? View.GONE : View.VISIBLE);
        rvFeed.setVisibility(hasPosts ? View.VISIBLE : View.GONE);
        emptyFeed.setVisibility(hasPosts ? View.GONE : View.VISIBLE);
        boolean hasProjects = !projectList.isEmpty();
        rvProjects.setVisibility(hasProjects ? View.VISIBLE : View.GONE);
        emptyProjects.setVisibility(hasProjects ? View.GONE : View.VISIBLE);
    }

    private void setupTabListeners() {
        tabGrid.setOnClickListener(v -> showPanel("grid"));
        tabFeed.setOnClickListener(v -> showPanel("feed"));
        tabProjects.setOnClickListener(v -> showPanel("projects"));
    }

    private void showPanel(String panel) {
        panelGrid.setVisibility("grid".equals(panel) ? View.VISIBLE : View.GONE);
        panelFeed.setVisibility("feed".equals(panel) ? View.VISIBLE : View.GONE);
        panelProjects.setVisibility("projects".equals(panel) ? View.VISIBLE : View.GONE);

        tabIndicatorGrid.setVisibility("grid".equals(panel) ? View.VISIBLE : View.INVISIBLE);
        tabIndicatorFeed.setVisibility("feed".equals(panel) ? View.VISIBLE : View.INVISIBLE);
        tabIndicatorProjects.setVisibility("projects".equals(panel) ? View.VISIBLE : View.INVISIBLE);
    }
}
