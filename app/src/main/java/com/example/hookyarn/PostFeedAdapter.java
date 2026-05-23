package com.example.hookyarn;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostFeedAdapter extends RecyclerView.Adapter<PostFeedAdapter.FeedViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(PostModel post);
        void onOptionsClick(PostModel post);
    }

    private final Context context;
    private final List<PostModel> posts;
    private final List<String> followingIds;
    private final OnPostClickListener listener;
    private final String currentUid;
    private final FirebaseFirestore db;

    public PostFeedAdapter(Context context, List<PostModel> posts, List<String> followingIds, OnPostClickListener listener) {
        this.context      = context;
        this.posts        = posts;
        this.followingIds = followingIds;
        this.listener     = listener;
        this.currentUid   = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_post_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        PostModel post = posts.get(position);

        String username = post.getUsername();
        if (username == null || username.isEmpty()) {
            String uid = post.getUid();
            username = "user" + (uid != null && uid.length() >= 5 ? uid.substring(0, 5) : "XXXXX");
        }
        holder.tvUsername.setText(username);
        holder.tvUsername.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_UID, post.getUid());
            context.startActivity(intent);
        });

        if (post.getCreatedAt() != null) {
            holder.tvTime.setText(formatTime(post.getCreatedAt().toDate()));
        }

        if (!TextUtils.isEmpty(post.getProfileImageUrl())) {
            Glide.with(context).load(post.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_account_circle)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_account_circle);
        }
        holder.ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_UID, post.getUid());
            context.startActivity(intent);
        });

        if (!TextUtils.isEmpty(post.getImageUrl())) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_yarn)
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(post.getCaption())) {
            holder.tvCaption.setVisibility(View.VISIBLE);
            holder.tvCaption.setText(post.getCaption());
        } else {
            holder.tvCaption.setVisibility(View.GONE);
        }

        int likes = post.getLikeCount();
        holder.tvLikeCount.setText(String.valueOf(likes));
        holder.btnLike.setImageResource(post.getLikedBy().contains(currentUid) 
                ? R.drawable.ic_favorites : R.drawable.ic_favorite_border);

        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        holder.btnLike.setOnClickListener(v -> toggleLike(post, holder.tvLikeCount, holder.btnLike));
        
        holder.btnComment.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(post);
        });

        String postUid = post.getUid();
        boolean isOwnPost = currentUid != null && postUid != null && currentUid.equals(postUid);
        
        holder.btnOptions.setVisibility(isOwnPost ? View.VISIBLE : View.GONE);
        
        if (isOwnPost || postUid == null) {
            holder.btnFollow.setVisibility(View.GONE);
            if (isOwnPost) {
                holder.btnOptions.setOnClickListener(v -> {
                    if (listener != null) listener.onOptionsClick(post);
                });
            }
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            boolean isFollowing = followingIds != null && followingIds.contains(postUid);
            holder.btnFollow.setText(isFollowing ? "Following" : "Follow");
            holder.btnFollow.setOnClickListener(v -> toggleFollow(post, holder.btnFollow));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(post);
        });
    }

    private void toggleFollow(PostModel post, MaterialButton btn) {
        if (TextUtils.isEmpty(currentUid)) return;
        String targetUid = post.getUid();
        if (followingIds == null) return;
        
        boolean currentlyFollowing = followingIds.contains(targetUid);

        if (currentlyFollowing) {
            followingIds.remove(targetUid);
            btn.setText("Follow");
            db.collection("users").document(currentUid)
                    .update("following", FieldValue.arrayRemove(targetUid));
            db.collection("users").document(targetUid)
                    .update("followers", FieldValue.arrayRemove(currentUid));
        } else {
            followingIds.add(targetUid);
            btn.setText("Following");
            db.collection("users").document(currentUid)
                    .update("following", FieldValue.arrayUnion(targetUid));
            db.collection("users").document(targetUid)
                    .update("followers", FieldValue.arrayUnion(currentUid));
        }
    }

    private void toggleLike(PostModel post, TextView tvCount, ImageButton btn) {
        if (TextUtils.isEmpty(currentUid)) return;

        boolean isLiked = post.getLikedBy().contains(currentUid);
        if (isLiked) {
            post.getLikedBy().remove(currentUid);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            btn.setImageResource(R.drawable.ic_favorite_border);
            
            db.collection("posts").document(post.getId())
                    .update("likeCount", FieldValue.increment(-1),
                            "likedBy", FieldValue.arrayRemove(currentUid));
            db.collection("threads").document(post.getId())
                    .update("likeCount", FieldValue.increment(-1),
                            "likedBy", FieldValue.arrayRemove(currentUid));
        } else {
            post.getLikedBy().add(currentUid);
            post.setLikeCount(post.getLikeCount() + 1);
            btn.setImageResource(R.drawable.ic_favorites);

            db.collection("posts").document(post.getId())
                    .update("likeCount", FieldValue.increment(1),
                            "likedBy", FieldValue.arrayUnion(currentUid));
            db.collection("threads").document(post.getId())
                    .update("likeCount", FieldValue.increment(1),
                            "likedBy", FieldValue.arrayUnion(currentUid));
        }
        tvCount.setText(String.valueOf(post.getLikeCount()));
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault()).format(date);
    }

    @Override
    public int getItemCount() { return posts.size(); }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivPostImage;
        TextView tvUsername, tvTime, tvCaption, tvLikeCount, tvCommentCount;
        ImageButton btnLike, btnComment, btnOptions;
        MaterialButton btnFollow;

        FeedViewHolder(@NonNull View v) {
            super(v);
            ivAvatar       = v.findViewById(R.id.ivPostAvatar);
            ivPostImage    = v.findViewById(R.id.ivPostImage);
            tvUsername     = v.findViewById(R.id.tvPostUsername);
            tvTime         = v.findViewById(R.id.tvPostTime);
            tvCaption      = v.findViewById(R.id.tvPostCaption);
            tvLikeCount    = v.findViewById(R.id.tvLikeCount);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
            btnLike        = v.findViewById(R.id.btnLike);
            btnComment     = v.findViewById(R.id.btnComment);
            btnOptions     = v.findViewById(R.id.btnPostOptions);
            btnFollow      = v.findViewById(R.id.btnFollowPost);
        }
    }
}