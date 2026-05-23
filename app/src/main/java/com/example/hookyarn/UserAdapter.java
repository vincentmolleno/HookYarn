package com.example.hookyarn;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context context;
    private List<UserModel> userList;
    private List<String> followingIds;
    private String currentUid;
    private FirebaseFirestore db;

    public UserAdapter(Context context, List<UserModel> userList, List<String> followingIds) {
        this.context = context;
        this.userList = userList;
        this.followingIds = followingIds;
        this.currentUid = FirebaseAuth.getInstance().getUid();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.tvUsername.setText(user.getUsername());
        holder.tvFullName.setText(user.getFullName());

        Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_account_circle)
                .circleCrop()
                .into(holder.ivAvatar);

        if (user.getUid().equals(currentUid)) {
            holder.btnFollow.setVisibility(View.GONE);
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            boolean isFollowing = followingIds != null && followingIds.contains(user.getUid());
            holder.btnFollow.setText(isFollowing ? "Following" : "Follow");
            holder.btnFollow.setOnClickListener(v -> toggleFollow(user, holder.btnFollow));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserProfileActivity.class);
            intent.putExtra("uid", user.getUid());
            context.startActivity(intent);
        });
    }

    private void toggleFollow(UserModel user, MaterialButton btn) {
        if (currentUid == null) return;
        boolean isFollowing = followingIds.contains(user.getUid());

        if (isFollowing) {
            followingIds.remove(user.getUid());
            btn.setText("Follow");
            db.collection("users").document(currentUid).update("following", FieldValue.arrayRemove(user.getUid()));
        } else {
            followingIds.add(user.getUid());
            btn.setText("Following");
            db.collection("users").document(currentUid).update("following", FieldValue.arrayUnion(user.getUid()));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername, tvFullName;
        MaterialButton btnFollow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUsername = itemView.findViewById(R.id.tvUserUsername);
            tvFullName = itemView.findViewById(R.id.tvUserFullName);
            btnFollow = itemView.findViewById(R.id.btnFollowUser);
        }
    }
}
