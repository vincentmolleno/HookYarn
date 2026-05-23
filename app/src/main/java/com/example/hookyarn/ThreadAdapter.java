package com.example.hookyarn;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ThreadAdapter extends RecyclerView.Adapter<ThreadAdapter.ViewHolder>{

    public interface OnThreadAction {
        void onEdit(ThreadPost post);
        void onDelete(ThreadPost post);
        void onComment(ThreadPost post);
        void onMessage(ThreadPost post);
    }

    private final Context context;
    private final List<ThreadPost> list;
    private final List<String> followingIds;
    private final OnThreadAction listener;
    private final String currentUid;
    private final FirebaseFirestore db;

    public ThreadAdapter(Context context, List<ThreadPost> list, List<String> followingIds, OnThreadAction listener){
        this.context = context;
        this.list = list;
        this.followingIds = followingIds;
        this.listener = listener;
        this.currentUid = FirebaseAuth.getInstance().getUid() != null 
                ? FirebaseAuth.getInstance().getUid() : "";
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_thread, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThreadPost post = list.get(position);

        String username = post.getUsername();
        if (username == null || username.isEmpty()) {
            String uid = post.getUserId();
            username = "user" + (uid != null && uid.length() >= 5 ? uid.substring(0, 5) : "XXXXX");
        }
        holder.txtUsername.setText(username);
        
        if (!TextUtils.isEmpty(post.getProfileImageUrl())) {
            Glide.with(context).load(post.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_account_circle)
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_account_circle);
        }

        if (!TextUtils.isEmpty(post.getText())) {
            holder.txtCaption.setVisibility(View.VISIBLE);
            holder.txtCaption.setText(post.getText());
        } else {
            holder.txtCaption.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(post.getImageUrl())) {
            holder.imgPost.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImageUrl())
                    .centerCrop()
                    .into(holder.imgPost);
        } else {
            holder.imgPost.setVisibility(View.GONE);
        }

        holder.txtLikes.setText(String.valueOf(post.getLikesCount()));
        holder.txtComments.setText(String.valueOf(post.getCommentCount()));

        boolean isLiked = post.getLikedBy().contains(currentUid);
        holder.btnLike.setImageResource(isLiked ? R.drawable.ic_favorites : R.drawable.ic_favorite_border);

        holder.btnLike.setOnClickListener(v -> toggleLike(post, holder.txtLikes, holder.btnLike));
        holder.btnComment.setOnClickListener(v -> {
            if (listener != null) listener.onComment(post);
        });
        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) listener.onMessage(post);
        });

        String postUid = post.getUserId();
        boolean isOwnPost = !currentUid.isEmpty() && postUid != null && currentUid.equals(postUid);
        
        holder.btnOptions.setVisibility(isOwnPost ? View.VISIBLE : View.GONE);
        
        if (isOwnPost || postUid == null) {
            holder.btnFollow.setVisibility(View.GONE);
            if (isOwnPost) {
                holder.btnOptions.setOnClickListener(v -> showOptions(post, holder.btnOptions));
            }
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            boolean isFollowing = followingIds != null && followingIds.contains(postUid);
            holder.btnFollow.setText(isFollowing ? "Following" : "Follow");
            holder.btnFollow.setOnClickListener(v -> toggleFollow(post, holder.btnFollow));
        }

        holder.txtUsername.setOnClickListener(v -> openProfile(postUid));
        holder.imgProfile.setOnClickListener(v -> openProfile(postUid));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onComment(post);
        });
    }

    private void openProfile(String uid) {
        if (uid == null) return;
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_UID, uid);
        context.startActivity(intent);
    }

    private void toggleFollow(ThreadPost post, MaterialButton btn) {
        if (TextUtils.isEmpty(currentUid)) return;
        String targetUid = post.getUserId();
        if (followingIds == null) return;

        boolean isFollowing = followingIds.contains(targetUid);

        if (isFollowing) {
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

    private void toggleLike(ThreadPost post, TextView tvCount, ImageView btn) {
        if (TextUtils.isEmpty(currentUid)) return;

        boolean isLiked = post.getLikedBy().contains(currentUid);
        if (isLiked) {
            post.getLikedBy().remove(currentUid);
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            btn.setImageResource(R.drawable.ic_favorite_border);
            
            db.collection("threads").document(post.getPostId())
                    .update("likeCount", FieldValue.increment(-1),
                            "likedBy", FieldValue.arrayRemove(currentUid));
            db.collection("posts").document(post.getPostId())
                    .update("likeCount", FieldValue.increment(-1),
                            "likedBy", FieldValue.arrayRemove(currentUid));
        } else {
            post.getLikedBy().add(currentUid);
            post.setLikesCount(post.getLikesCount() + 1);
            btn.setImageResource(R.drawable.ic_favorites);
            
            db.collection("threads").document(post.getPostId())
                    .update("likeCount", FieldValue.increment(1),
                            "likedBy", FieldValue.arrayUnion(currentUid));
            db.collection("posts").document(post.getPostId())
                    .update("likeCount", FieldValue.increment(1),
                            "likedBy", FieldValue.arrayUnion(currentUid));
        }
        tvCount.setText(String.valueOf(post.getLikesCount()));
    }

    private void showOptions(ThreadPost post, View anchor) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Edit")) {
                if (listener != null) listener.onEdit(post);
            } else {
                if (listener != null) listener.onDelete(post);
            }
            return true;
        });
        popup.show();
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUsername, txtLikes, txtComments, txtCaption;
        ImageView btnLike, btnComment, btnMessage, btnOptions, imgProfile, imgPost;
        MaterialButton btnFollow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtLikes    = itemView.findViewById(R.id.txtLikes);
            txtComments = itemView.findViewById(R.id.txtComments);
            txtCaption  = itemView.findViewById(R.id.txtCaption);
            btnLike     = itemView.findViewById(R.id.btnLike);
            btnComment  = itemView.findViewById(R.id.btnComment);
            btnMessage  = itemView.findViewById(R.id.btnMessage);
            btnOptions  = itemView.findViewById(R.id.btnOptions);
            imgProfile  = itemView.findViewById(R.id.imgProfile);
            imgPost     = itemView.findViewById(R.id.imgPost);
            btnFollow   = itemView.findViewById(R.id.btnFollowThread);
        }
    }
}