package com.example.hookyarn;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class profile extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;

    private TextView tvAvatarInitials,tvUsername,tvPostCount,btnEditProfile;

    private LinearLayout tabGrid, tabFeed, tabProjects;
    private View tabIndicatorGrid, tabIndicatorFeed, tabIndicatorProjects;

    private FrameLayout panelGrid, panelFeed, panelProjects;

    private RecyclerView rvGrid, rvFeed, rvProjects;

    private LinearLayout emptyGrid, emptyFeed, emptyProjects;

    private LinearLayout navHome, navShop, navCreate, navThread, navProfile;

    private PostGridAdapter gridAdapter;
    private PostFeedAdapter feedAdapter;
    private ProjectListAdapter projectAdapter;

    private List<PostModel> postList = new ArrayList<>();
    private List<ProjectModel> projectList = new ArrayList<>();
    private List<String> followingIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        currentUid = user.getUid();

        bindViews();
        setupTabs();
        setupBottomNav();
        setupRecyclerViews();

        loadUserProfile();
        loadUserPosts();
        loadUserProjects();
    }

    @SuppressLint("WrongViewCast")
    private void bindViews() {

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvUsername       = findViewById(R.id.tvUsername);
        tvPostCount      = findViewById(R.id.tvPostCount);
        btnEditProfile   = findViewById(R.id.btnEditProfile);

        tabGrid          = findViewById(R.id.tabGrid);
        tabFeed          = findViewById(R.id.tabFeed);
        tabProjects      = findViewById(R.id.tabProjects);
        tabIndicatorGrid     = findViewById(R.id.tabIndicatorGrid);
        tabIndicatorFeed     = findViewById(R.id.tabIndicatorFeed);
        tabIndicatorProjects = findViewById(R.id.tabIndicatorProjects);

        panelGrid        = findViewById(R.id.panelGrid);
        panelFeed        = findViewById(R.id.panelFeed);
        panelProjects    = findViewById(R.id.panelProjects);

        rvGrid           = findViewById(R.id.rvGrid);
        rvFeed           = findViewById(R.id.rvFeed);
        rvProjects       = findViewById(R.id.rvProjects);

        emptyGrid        = findViewById(R.id.emptyGrid);
        emptyFeed        = findViewById(R.id.emptyFeed);
        emptyProjects    = findViewById(R.id.emptyProjects);

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(profile.this, EditProfileActivity.class))
        );
    }

    private void setupTabs() {
        showTab("gallery");

        tabGrid.setOnClickListener(v     -> showTab("gallery"));
        tabFeed.setOnClickListener(v     -> showTab("posts"));
        tabProjects.setOnClickListener(v -> showTab("projects"));
    }

    private void showTab(String tab) {
        panelGrid.setVisibility(View.GONE);
        panelFeed.setVisibility(View.GONE);
        panelProjects.setVisibility(View.GONE);

        tabIndicatorGrid.setVisibility(View.INVISIBLE);
        tabIndicatorFeed.setVisibility(View.INVISIBLE);
        tabIndicatorProjects.setVisibility(View.INVISIBLE);

        switch (tab) {
            case "gallery":
                panelGrid.setVisibility(View.VISIBLE);
                tabIndicatorGrid.setVisibility(View.VISIBLE);
                break;
            case "posts":
                panelFeed.setVisibility(View.VISIBLE);
                tabIndicatorFeed.setVisibility(View.VISIBLE);
                break;
            case "projects":
                panelProjects.setVisibility(View.VISIBLE);
                tabIndicatorProjects.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupBottomNav() {
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        navShop.setOnClickListener(v ->
                Toast.makeText(this, "Shop coming soon!", Toast.LENGTH_SHORT).show()
        );

        navCreate.setOnClickListener(v ->
                Toast.makeText(this, "Create coming soon!", Toast.LENGTH_SHORT).show()
        );

        navThread.setOnClickListener(v ->
                Toast.makeText(this, "Thread coming soon!", Toast.LENGTH_SHORT).show()
        );

        navProfile.setOnClickListener(v -> { });
    }

    private void setupRecyclerViews() {

        gridAdapter = new PostGridAdapter(this, postList, post ->
                openPostDetail(post)
        );
        rvGrid.setLayoutManager(new GridLayoutManager(this, 3));
        rvGrid.setAdapter(gridAdapter);

        feedAdapter = new PostFeedAdapter(this, postList, followingIds, new PostFeedAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(PostModel post) {
                openPostDetail(post);
            }

            @Override
            public void onOptionsClick(PostModel post) {
            }
        });
        rvFeed.setLayoutManager(new LinearLayoutManager(this));
        rvFeed.setAdapter(feedAdapter);

        projectAdapter = new ProjectListAdapter(this, projectList, project ->
                openProjectDetail(project)
        );
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(projectAdapter);
    }

    private void loadUserProfile() {
        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        String avatar   = doc.getString("avatar");

                        String displayUsername = (username != null && !username.isEmpty()) ? username : "user" + currentUid.substring(0, 5);
                        tvUsername.setText(displayUsername);
                        tvAvatarInitials.setText(avatar != null ? avatar : getString(R.string.emoji_yarn));

                        List<String> following = (List<String>) doc.get("following");
                        followingIds.clear();
                        if (following != null) followingIds.addAll(following);
                        if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUserPosts() {
        db.collection("posts")
                .whereEqualTo("uid", currentUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    postList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        PostModel post = doc.toObject(PostModel.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            postList.add(post);
                        }
                    }

                    int count = postList.size();
                    tvPostCount.setText(String.valueOf(count));

                    gridAdapter.notifyDataSetChanged();
                    feedAdapter.notifyDataSetChanged();

                    emptyGrid.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                    rvGrid.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
                    emptyFeed.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                    rvFeed.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load posts.", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUserProjects() {
        db.collection("projects")
                .whereEqualTo("uid", currentUid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    projectList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ProjectModel project = doc.toObject(ProjectModel.class);
                        if (project != null) {
                            project.setId(doc.getId());
                            projectList.add(project);
                        }
                    }

                    projectAdapter.notifyDataSetChanged();

                    int count = projectList.size();
                    emptyProjects.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                    rvProjects.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load projects.", Toast.LENGTH_SHORT).show()
                );
    }

    private void openPostDetail(PostModel post) {
        Intent intent = new Intent(profile.this, PostDetailActivity.class);
        intent.putExtra("postId", post.getId());
        startActivity(intent);
    }

    private void openProjectDetail(ProjectModel project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra("projectId", project.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }
}