package com.example.hookyarn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentVH> {
    private List<CommentModel> list;

    public CommentAdapter(List<CommentModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new CommentVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentVH holder, int position) {
        CommentModel comment = list.get(position);
        holder.tvUser.setText(comment.getUsername());
        holder.tvText.setText(comment.getCommentText());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class CommentVH extends RecyclerView.ViewHolder {
        TextView tvUser, tvText;
        CommentVH(@NonNull View v) {
            super(v);
            tvUser = v.findViewById(android.R.id.text1);
            tvText = v.findViewById(android.R.id.text2);
        }
    }
}
