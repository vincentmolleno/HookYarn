package com.example.hookyarn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Home extends Fragment implements HomeActivity.SearchableFragment {

    private static final String TAG = "HomeFragment";

    private RecyclerView  rvStories;
    private RecyclerView  rvHomeFeed;
    private RecyclerView  rvUserSearch;
    private LinearLayout  emptyHomeFeed;

    private StoryAdapter    storyAdapter;
    private HomeFeedAdapter feedAdapter;
    private UserAdapter     userAdapter;

    private final List<StoryModel> storyList    = new ArrayList<>();
    private final List<PostModel>  feedPostList = new ArrayList<>();
    private final List<UserModel>  userSearchList = new ArrayList<>();
    private final List<String>    followingIds = new ArrayList<>();

    private FirebaseFirestore    db;
    private FirebaseAuth         auth;
    private ListenerRegistration feedListener;
    private ListenerRegistration storyListener;
    private String lastSearchQuery = "";

    private final ActivityResultLauncher<String> storyImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadStory(uri);
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_feed, container, false);

        db      = FirebaseFirestore.getInstance();
        auth    = FirebaseAuth.getInstance();

        bindViews(view);
        setupStories();
        setupFeed();
        loadCurrentUserThenData();

        return view;
    }

    private void bindViews(View view) {
        rvStories     = view.findViewById(R.id.rvStories);
        rvHomeFeed    = view.findViewById(R.id.rvHomeFeed);
        rvUserSearch  = view.findViewById(R.id.rvUserSearch);
        emptyHomeFeed = view.findViewById(R.id.emptyHomeFeed);
    }

    private void setupStories() {
        storyAdapter = new StoryAdapter(requireContext(), storyList,
                new StoryAdapter.OnStoryClickListener() {
                    @Override
                    public void onStoryClick(StoryModel story, int position) {
                        if (story.getStoryImageUrl() != null && !story.getStoryImageUrl().isEmpty()) {
                            Intent intent = new Intent(getContext(), StoryViewActivity.class);
                            intent.putExtra("storyImageUrl", story.getStoryImageUrl());
                            intent.putExtra("username", story.getUsername());
                            intent.putExtra("profileImageUrl", story.getProfileImageUrl());
                            startActivity(intent);
                        } else {
                            Toast.makeText(getContext(),
                                    story.getUsername() + " has no active story", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAddStoryClick() {
                        StoryModel own = storyList.get(0);
                        if (own.getStoryImageUrl() != null && !own.getStoryImageUrl().isEmpty()) {
                            showOwnStoryOptions(own);
                        } else {
                            storyImagePicker.launch("image/*");
                        }
                    }
                });

        if (rvStories != null) {
            rvStories.setLayoutManager(
                    new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvStories.setAdapter(storyAdapter);
        }
    }

    private void setupFeed() {
        feedAdapter = new HomeFeedAdapter(
                requireContext(), feedPostList, followingIds, new HomeFeedAdapter.OnPostAction() {
            @Override
            public void onPostClick(PostModel post) {
                Intent intent = new Intent(getContext(), PostDetailActivity.class);
                intent.putExtra("postId", post.getId());
                intent.putExtra("collection", "posts");
                startActivity(intent);
            }

            @Override
            public void onOptionsClick(PostModel post) {
                showPostOptionsDialog(post);
            }
        });
        if (rvHomeFeed != null) {
            rvHomeFeed.setLayoutManager(new LinearLayoutManager(getContext()));
            rvHomeFeed.setNestedScrollingEnabled(false);
            rvHomeFeed.setAdapter(feedAdapter);
        }

        userAdapter = new UserAdapter(requireContext(), userSearchList, followingIds);
        if (rvUserSearch != null) {
            rvUserSearch.setLayoutManager(new LinearLayoutManager(getContext()));
            rvUserSearch.setAdapter(userAdapter);
        }
    }

    private void showPostOptionsDialog(PostModel post) {
        String[] options = {"Edit Caption", "Delete Post"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Post Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditCaptionDialog(post);
                    } else if (which == 1) {
                        confirmDeletePost(post);
                    }
                })
                .show();
    }

    private void showEditCaptionDialog(PostModel post) {
        android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setText(post.getCaption());
        et.setPadding(50, 20, 50, 20);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit Caption")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCaption = et.getText().toString().trim();
                    db.collection("posts").document(post.getId())
                            .update("caption", newCaption)
                            .addOnSuccessListener(aVoid -> {
                                db.collection("threads").document(post.getId()).update("caption", newCaption);
                                Toast.makeText(getContext(), "Post updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOwnStoryOptions(StoryModel story) {
        String[] options = {"View Story", "Change Story", "Delete Story"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Your Story")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(getContext(), StoryViewActivity.class);
                        intent.putExtra("storyImageUrl", story.getStoryImageUrl());
                        intent.putExtra("username", "You");
                        intent.putExtra("profileImageUrl", story.getProfileImageUrl());
                        startActivity(intent);
                    } else if (which == 1) {
                        storyImagePicker.launch("image/*");
                    } else if (which == 2) {
                        confirmDeleteStory(story);
                    }
                })
                .show();
    }

    private void confirmDeleteStory(StoryModel story) {
        if (story.getId() == null) {
            Toast.makeText(getContext(), "Error: Story ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Story")
                .setMessage("Are you sure you want to delete your story?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("stories").document(story.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Story deleted", Toast.LENGTH_SHORT).show();
                                story.setStoryImageUrl(null);
                                story.setId(null);
                                storyAdapter.notifyItemChanged(0);
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeletePost(PostModel post) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("posts").document(post.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                db.collection("threads").document(post.getId()).delete();
                                Toast.makeText(getContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadCurrentUserThenData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        db.collection("users").document(uid).addSnapshotListener((doc, error) -> {
            if (error != null || doc == null || !doc.exists()) {
                Log.e(TAG, "Failed to load user doc", error);
                return;
            }

            String ownProfileUrl = doc.getString("profileImageUrl");
            String ownUsername   = doc.getString("username");

            List<String> following = (List<String>) doc.get("following");
            followingIds.clear();
            if (following != null) {
                followingIds.addAll(following);
            }

            StoryModel ownBubble = new StoryModel(
                    uid, ownUsername != null ? ownUsername : "You",
                    ownProfileUrl, null, 0, true);

            storyList.clear();
            storyList.add(ownBubble);         

            loadGlobalStoriesAndFeed(ownBubble);
            
            if (feedAdapter != null) {
                feedAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadGlobalStoriesAndFeed(StoryModel ownBubble) {
        String uid = auth.getCurrentUser().getUid();

        if (storyListener != null) storyListener.remove();

        long since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L;

        storyListener = db.collection("stories")
                .whereGreaterThan("createdAt", since)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;

                    storyList.clear();
                    storyList.add(ownBubble);

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        StoryModel s = doc.toObject(StoryModel.class);
                        if (s != null) {
                            s.setId(doc.getId());
                            if (s.getUserId().equals(uid)) {
                                ownBubble.setId(s.getId());
                                ownBubble.setStoryImageUrl(s.getStoryImageUrl());
                                ownBubble.setCreatedAt(s.getCreatedAt());
                            } else {
                                if (followingIds.contains(s.getUserId())) {
                                    storyList.add(s);
                                }
                            }
                        }
                    }
                    storyAdapter.notifyDataSetChanged();
                });

        if (feedListener != null) {
            feedListener.remove();
        }

        List<String> queryUids = new ArrayList<>(followingIds);
        queryUids.add(uid);

        List<String> limitedUids = queryUids.size() > 30 ? queryUids.subList(0, 30) : queryUids;

        feedListener = db.collection("posts")
                .whereIn("uid", limitedUids)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) {
                        Log.e(TAG, "Feed listener error", err);
                        loadGlobalFeedFallback();
                        return;
                    }
                    feedPostList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PostModel post = doc.toObject(PostModel.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            feedPostList.add(post);
                        }
                    }
                    feedAdapter.notifyDataSetChanged();
                    updateFeedVisibility();
                });
    }

    private void loadGlobalFeedFallback() {
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    feedPostList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        PostModel post = doc.toObject(PostModel.class);
                        if (post != null) {
                            post.setId(doc.getId());
                            feedPostList.add(post);
                        }
                    }
                    feedAdapter.notifyDataSetChanged();
                    updateFeedVisibility();
                });
    }

    private void uploadStory(Uri imageUri) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String uid  = user.getUid();
        String fileName = "story_" + System.currentTimeMillis() + ".jpg";

        Toast.makeText(getContext(), "Uploading story…", Toast.LENGTH_SHORT).show();

        SupabaseHelper.uploadFile(requireContext(), "stories", fileName, imageUri, new SupabaseHelper.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                saveStoryToFirestore(uid, publicUrl);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Story upload failed", e);
                Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStoryToFirestore(String uid, String imageUrl) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String username   = doc.getString("username");
                    if (username == null || username.isEmpty()) {
                        username = "user" + (uid != null && uid.length() >= 5 ? uid.substring(0, 5) : "XXXXX");
                    }
                    String profileUrl = doc.getString("profileImageUrl");

                    Map<String, Object> story = new HashMap<>();
                    story.put("userId",          uid);
                    story.put("username",         username);
                    story.put("profileImageUrl",  profileUrl);
                    story.put("storyImageUrl",    imageUrl);
                    story.put("createdAt",        System.currentTimeMillis());

                    db.collection("stories").add(story)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(getContext(),
                                        "Story posted!", Toast.LENGTH_SHORT).show();

                                if (!storyList.isEmpty() && storyList.get(0).isOwn()) {
                                    storyList.get(0).setStoryImageUrl(imageUrl);
                                    storyList.get(0).setId(ref.getId());
                                    storyAdapter.notifyItemChanged(0);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Failed to post story", Toast.LENGTH_SHORT).show());
                });
    }

    @Override
    public void onSearch(String query) {
        lastSearchQuery = query;
        if (query.isEmpty()) {
            if (rvUserSearch != null) rvUserSearch.setVisibility(View.GONE);
            if (rvStories != null) rvStories.setVisibility(View.VISIBLE);
            if (rvHomeFeed != null) rvHomeFeed.setVisibility(View.VISIBLE);
            loadCurrentUserThenData();
            return;
        }

        if (rvStories != null) rvStories.setVisibility(View.GONE);
        if (rvHomeFeed != null) rvHomeFeed.setVisibility(View.GONE);
        if (rvUserSearch != null) rvUserSearch.setVisibility(View.VISIBLE);
        if (emptyHomeFeed != null) emptyHomeFeed.setVisibility(View.GONE);

        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!query.equals(lastSearchQuery)) return;

                    userSearchList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        UserModel user = doc.toObject(UserModel.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            userSearchList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                });
    }

    private void updateFeedVisibility() {
        boolean hasPosts = !feedPostList.isEmpty();
        rvHomeFeed.setVisibility(hasPosts    ? View.VISIBLE : View.GONE);
        emptyHomeFeed.setVisibility(hasPosts ? View.GONE   : View.VISIBLE);
    }

    private List<String> limitTo20(List<String> list) {
        return list.size() > 20 ? list.subList(0, 20) : list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (feedListener  != null) feedListener.remove();
        if (storyListener != null) storyListener.remove();
    }
}