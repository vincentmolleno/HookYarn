package com.example.hookyarn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PostGridAdapter extends RecyclerView.Adapter<PostGridAdapter.GridViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(PostModel post);
    }

    private final Context context;
    private final List<PostModel> posts;
    private final OnPostClickListener listener;

    public PostGridAdapter(Context context, List<PostModel> posts, OnPostClickListener listener) {
        this.context  = context;
        this.posts    = posts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_post_grid, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        PostModel post = posts.get(position);

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(post.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.imgGridPost);
        } else {
            holder.imgGridPost.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            } else {
                android.content.Intent intent = new android.content.Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", post.getId());
                intent.putExtra("collection", "posts");
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() { return posts.size(); }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView imgGridPost;

        GridViewHolder(@NonNull View itemView) {
            super(itemView);
            imgGridPost = itemView.findViewById(R.id.imgGridPost);
        }
    }
}