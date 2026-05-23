package com.example.hookyarn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(ProjectModel project);
    }

    private final Context context;
    private final List<ProjectModel> projects;
    private final OnProjectClickListener listener;

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public ProjectListAdapter(Context context, List<ProjectModel> projects,
                              OnProjectClickListener listener) {
        this.context  = context;
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectModel project = projects.get(position);

        holder.tvTitle.setText(
                project.getTitle() != null ? project.getTitle() : "Untitled Project"
        );

        // Last updated date
        if (project.getUpdatedAt() != null) {
            holder.tvUpdated.setText(
                    "Updated " + DATE_FMT.format(project.getUpdatedAt().toDate())
            );
        } else {
            holder.tvUpdated.setText("Just now");
        }

        // Status badge
        setStatusBadge(holder.tvStatus, project.getStatus());

        holder.itemView.setOnClickListener(v -> listener.onProjectClick(project));
    }

    private void setStatusBadge(TextView badge, String status) {
        if (status == null) status = "ongoing";

        switch (status.toLowerCase()) {
            case "done":
                badge.setText("Done");
                badge.setTextColor(Color.parseColor("#2E7D32"));
                setBadgeBackground(badge, "#E8F5E9");
                break;
            case "draft":
                badge.setText("Draft");
                badge.setTextColor(Color.parseColor("#7B1FA2"));
                setBadgeBackground(badge, "#F3E5F5");
                break;
            case "ongoing":
            default:
                badge.setText("Ongoing");
                badge.setTextColor(Color.parseColor("#E65100"));
                setBadgeBackground(badge, "#FFF3E0");
                break;
        }
    }

    private void setBadgeBackground(TextView view, String colorHex) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(colorHex));
        bg.setCornerRadius(50f);
        view.setBackground(bg);
    }

    @Override
    public int getItemCount() { return projects.size(); }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvUpdated, tvStatus;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tvProjectTitle);
            tvUpdated = itemView.findViewById(R.id.tvProjectUpdated);
            tvStatus  = itemView.findViewById(R.id.tvProjectStatus);
        }
    }
}