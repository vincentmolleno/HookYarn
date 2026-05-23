package com.example.hookyarn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryVH> {

    public interface OnStoryClickListener {
        void onStoryClick(StoryModel story, int position);
        void onAddStoryClick();
    }

    private final Context             context;
    private final List<StoryModel>    stories;
    private final OnStoryClickListener listener;

    public StoryAdapter(Context context, List<StoryModel> stories,
                        OnStoryClickListener listener) {
        this.context  = context;
        this.stories  = stories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_story, parent, false);
        return new StoryVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryVH h, int position) {
        StoryModel story = stories.get(position);

        h.tvUsername.setText(story.isOwn() ? "Your Story" : story.getUsername());

        if (story.getProfileImageUrl() != null && !story.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(story.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_account_circle)
                    .into(h.ivProfile);
        } else {
            h.ivProfile.setImageResource(R.drawable.ic_account_circle);
        }

        h.ivAddBadge.setVisibility(story.isOwn() ? View.VISIBLE : View.GONE);

        boolean hasStory = story.getStoryImageUrl() != null
                && !story.getStoryImageUrl().isEmpty();
        h.storyRing.setBackgroundResource(
                hasStory ? R.drawable.bg_story_ring_active
                        : R.drawable.bg_story_ring_inactive);

        h.itemView.setOnClickListener(v -> {
            if (story.isOwn()) {
                listener.onAddStoryClick();
            } else {
                if (story.getStoryImageUrl() != null && !story.getStoryImageUrl().isEmpty()) {
                    listener.onStoryClick(story, h.getAdapterPosition());
                } else {
                    android.content.Intent intent = new android.content.Intent(context, UserProfileActivity.class);
                    intent.putExtra(UserProfileActivity.EXTRA_UID, story.getUserId());
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() { return stories.size(); }

    static class StoryVH extends RecyclerView.ViewHolder {
        View      storyRing;
        ImageView ivProfile, ivAddBadge;
        TextView  tvUsername;

        StoryVH(@NonNull View v) {
            super(v);
            storyRing   = v.findViewById(R.id.storyRing);
            ivProfile   = v.findViewById(R.id.ivStoryProfile);
            ivAddBadge  = v.findViewById(R.id.ivAddBadge);
            tvUsername  = v.findViewById(R.id.tvStoryUsername);
        }
    }
}