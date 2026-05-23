package com.example.hookyarn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    private ArrayList<ProjectModel> projects;
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(ProjectModel project);
        void onProjectRemove(ProjectModel project, int position);
    }

    public ProjectAdapter(ArrayList<ProjectModel> projects, OnProjectClickListener listener) {
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectModel project = projects.get(position);
        holder.projectName.setText(project.getTitle());
        holder.projectStatus.setText(project.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProjectClick(project);
            } else {
                android.content.Context context = v.getContext();
                android.content.Intent intent = new android.content.Intent(context, ProjectDetailActivity.class);
                intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.getId());
                intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_TITLE, project.getTitle());
                intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_STATUS, project.getStatus());
                context.startActivity(intent);
            }
        });
        if (holder.btnRemove != null) {
            if (listener != null) {
                holder.btnRemove.setVisibility(View.VISIBLE);
                holder.btnRemove.setOnClickListener(v -> listener.onProjectRemove(project, position));
            } else {
                holder.btnRemove.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView projectName;
        TextView projectStatus;
        ImageView btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            projectName = itemView.findViewById(R.id.tvProjectTitle);
            projectStatus = itemView.findViewById(R.id.tvProjectStatus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}