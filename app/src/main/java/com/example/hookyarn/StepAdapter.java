package com.example.hookyarn;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.StepViewHolder> {

    private final Context context;
    private final List<ProjectModel.StepModel> steps;

    public StepAdapter(Context context, List<ProjectModel.StepModel> steps) {
        this.context = context;
        this.steps   = steps;
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_step, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        ProjectModel.StepModel step = steps.get(position);

        holder.tvStepLabel.setText(step.getLabel());

        if (step.isDone()) {
            holder.tvStepDot.setText(context.getString(R.string.checkbox_checked));
            holder.tvStepLabel.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvStepLabel.setPaintFlags(
                    holder.tvStepLabel.getPaintFlags()
                            | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            holder.tvStepDot.setText(context.getString(R.string.checkbox_unchecked));
            holder.tvStepLabel.setTextColor(Color.parseColor("#2D1B12"));
            holder.tvStepLabel.setPaintFlags(
                    holder.tvStepLabel.getPaintFlags()
                            & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            );
        }
    }

    @Override
    public int getItemCount() { return steps.size(); }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        TextView tvStepDot, tvStepLabel;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepDot   = itemView.findViewById(R.id.tvStepDot);
            tvStepLabel = itemView.findViewById(R.id.tvStepLabel);
        }
    }
}