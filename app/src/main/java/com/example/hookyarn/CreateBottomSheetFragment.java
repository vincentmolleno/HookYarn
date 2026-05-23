package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class CreateBottomSheetFragment extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_create_options, container, false);

        LinearLayout btnProject = view.findViewById(R.id.btnProject);
        LinearLayout btnFolder = view.findViewById(R.id.btnFolder);
        LinearLayout btnPost = view.findViewById(R.id.btnPost);

        btnProject.setOnClickListener(v -> {
            openCreateActivity("project");
            dismiss();
        });

        btnFolder.setOnClickListener(v -> {
            openCreateActivity("folder");
            dismiss();
        });

        btnPost.setOnClickListener(v -> {
            openCreateActivity("post");
            dismiss();
        });


        return view;
    }

    private void openCreateActivity(String type) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), CreateActivity.class);
            intent.putExtra("create_type", type);
            startActivity(intent);
        }
    }
}