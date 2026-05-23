package com.example.hookyarn;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private LinearLayout tabGrid, tabFeed, tabProjects;
    private FrameLayout panelGrid, panelFeed, panelProjects;
    private View tabIndicatorGrid, tabIndicatorFeed, tabIndicatorProjects;
    private View btnEditProfile;
    
    private RecyclerView rvGrid, rvFeed, rvProjects;
    private View emptyGrid, emptyFeed, emptyProjects;
    private TextView tvPostCount, tvFollowers, tvFollowing, tvUsername, tvAvatarInitials;
    private ImageView imgAvatar;

    private PostGridAdapter gridAdapter;
    private PostFeedAdapter feedAdapter;
    private ProjectAdapter projectAdapter;
    private ArrayList<PostModel> postList = new ArrayList<>();
    private ArrayList<ProjectModel> projectList = new ArrayList<>();
    private List<String> followingIds = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        loadUserData();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews(view);
        setupTabListeners();
        setupRecyclerViews();

        loadUserData();
        loadUserPosts();
        loadUserProjects();

        return view;
    }

    private void initializeViews(View view) {
        tabGrid = view.findViewById(R.id.tabGrid);
        tabFeed = view.findViewById(R.id.tabFeed);
        tabProjects = view.findViewById(R.id.tabProjects);

        panelGrid = view.findViewById(R.id.panelGrid);
        panelFeed = view.findViewById(R.id.panelFeed);
        panelProjects = view.findViewById(R.id.panelProjects);

        tabIndicatorGrid = view.findViewById(R.id.tabIndicatorGrid);
        tabIndicatorFeed = view.findViewById(R.id.tabIndicatorFeed);
        tabIndicatorProjects = view.findViewById(R.id.tabIndicatorProjects);

        rvGrid = view.findViewById(R.id.rvGrid);
        rvFeed = view.findViewById(R.id.rvFeed);
        rvProjects = view.findViewById(R.id.rvProjects);

        emptyGrid = view.findViewById(R.id.emptyGrid);
        emptyFeed = view.findViewById(R.id.emptyFeed);
        emptyProjects = view.findViewById(R.id.emptyProjects);

        tvPostCount = view.findViewById(R.id.tvPostCount);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        tvFollowing = view.findViewById(R.id.tvFollowing);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
    }

    private void setupRecyclerViews() {
        rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 3));
        gridAdapter = new PostGridAdapter(getContext(), postList, this::showPostOptionsDialog);
        rvGrid.setAdapter(gridAdapter);

        rvFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        feedAdapter = new PostFeedAdapter(getContext(), postList, followingIds, new PostFeedAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(PostModel post) {
                Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                intent.putExtra("postId", post.getId());
                intent.putExtra("collection", "posts");
                startActivity(intent);
            }

            @Override
            public void onOptionsClick(PostModel post) {
                showPostOptionsDialog(post);
            }
        });
        rvFeed.setAdapter(feedAdapter);

        rvProjects.setLayoutManager(new LinearLayoutManager(getContext()));
        projectAdapter = new ProjectAdapter(projectList, new ProjectAdapter.OnProjectClickListener() {
            @Override
            public void onProjectClick(ProjectModel project) {
                Intent intent = new Intent(getActivity(), ProjectDetailActivity.class);
                intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getId());
                intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_TITLE, project.getTitle());
                intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_STATUS, project.getStatus());
                startActivity(intent);
            }

            @Override
            public void onProjectRemove(ProjectModel project, int position) {
                confirmDeleteProject(project);
            }
        });
        rvProjects.setAdapter(projectAdapter);
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).addSnapshotListener((documentSnapshot, error) -> {
            if (error != null || documentSnapshot == null || !documentSnapshot.exists()) return;

            String username = documentSnapshot.getString("username");
            if (username != null) tvUsername.setText(username);

            List<String> followers = (List<String>) documentSnapshot.get("followers");
            List<String> following = (List<String>) documentSnapshot.get("following");
            
            followingIds.clear();
            if (following != null) followingIds.addAll(following);
            
            tvFollowers.setText(String.valueOf(followers != null ? followers.size() : 0));
            tvFollowing.setText(String.valueOf(following != null ? following.size() : 0));
            
            if (feedAdapter != null) feedAdapter.notifyDataSetChanged();

            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                if (getContext() != null) {
                    com.bumptech.glide.Glide.with(getContext())
                            .load(profileImageUrl)
                            .circleCrop()
                            .into(imgAvatar);
                    imgAvatar.setVisibility(View.VISIBLE);
                    tvAvatarInitials.setVisibility(View.GONE);
                }
            } else {
                imgAvatar.setVisibility(View.GONE);
                tvAvatarInitials.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadUserPosts() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("posts")
                .whereEqualTo("uid", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    postList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
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
                    
                    updateTotalPostCount();
                    updateEmptyStates();
                });
    }

    private void updateTotalPostCount() {
        int total = postList.size();
        tvPostCount.setText(String.valueOf(total));
    }

    private void loadUserProjects() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("projects")
                .whereEqualTo("uid", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

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

    private void confirmDeleteProject(ProjectModel project) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete this project?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("projects").document(project.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Project deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPostOptionsDialog(PostModel post) {
        String[] options = {"View Post", "Edit Caption", "Delete Post"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Post Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                        intent.putExtra("postId", post.getId());
                        intent.putExtra("collection", "posts");
                        startActivity(intent);
                    } else if (which == 1) {
                        showEditCaptionDialog(post);
                    } else if (which == 2) {
                        confirmDeletePost(post);
                    }
                })
                .show();
    }

    private void showEditCaptionDialog(PostModel post) {
        android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setText(post.getCaption());
        et.setPadding(50, 20, 50, 20);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Caption")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCaption = et.getText().toString().trim();
                    db.collection("posts").document(post.getId())
                            .update("caption", newCaption)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Post updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeletePost(PostModel post) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("posts").document(post.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Post deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        tabGrid.setOnClickListener(v -> showGridPanel());
        tabFeed.setOnClickListener(v -> showFeedPanel());
        tabProjects.setOnClickListener(v -> showProjectsPanel());
        
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            editProfileLauncher.launch(intent);
        });
    }

    private void showGridPanel() {
        panelGrid.setVisibility(View.VISIBLE);
        panelFeed.setVisibility(View.GONE);
        panelProjects.setVisibility(View.GONE);

        tabIndicatorGrid.setVisibility(View.VISIBLE);
        tabIndicatorFeed.setVisibility(View.INVISIBLE);
        tabIndicatorProjects.setVisibility(View.INVISIBLE);

        updateTabColors("grid");
    }

    private void showFeedPanel() {
        panelGrid.setVisibility(View.GONE);
        panelFeed.setVisibility(View.VISIBLE);
        panelProjects.setVisibility(View.GONE);

        tabIndicatorGrid.setVisibility(View.INVISIBLE);
        tabIndicatorFeed.setVisibility(View.VISIBLE);
        tabIndicatorProjects.setVisibility(View.INVISIBLE);

        updateTabColors("feed");
    }

    private void showProjectsPanel() {
        panelGrid.setVisibility(View.GONE);
        panelFeed.setVisibility(View.GONE);
        panelProjects.setVisibility(View.VISIBLE);

        tabIndicatorGrid.setVisibility(View.INVISIBLE);
        tabIndicatorFeed.setVisibility(View.INVISIBLE);
        tabIndicatorProjects.setVisibility(View.VISIBLE);

        updateTabColors("projects");
    }

    private void updateTabColors(String activeTab) {
        updateSingleTab(tabGrid, "grid".equals(activeTab));
        updateSingleTab(tabFeed, "feed".equals(activeTab));
        updateSingleTab(tabProjects, "projects".equals(activeTab));
    }

    private void updateSingleTab(LinearLayout tabLayout, boolean isActive) {
        int color = isActive ? 
            ContextCompat.getColor(requireContext(), android.R.color.black) :
            Color.parseColor("#8D6255");
        
        if (tabLayout.getChildCount() >= 2) {
            View icon = tabLayout.getChildAt(0);
            View text = tabLayout.getChildAt(1);
            
            if (icon instanceof ImageView) {
                ((ImageView) icon).setColorFilter(color);
            }
            if (text instanceof TextView) {
                ((TextView) text).setTextColor(color);
            }
        }
    }
}