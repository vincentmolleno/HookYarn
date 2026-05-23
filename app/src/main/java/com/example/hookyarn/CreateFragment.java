package com.example.hookyarn;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CreateFragment extends BottomSheetDialogFragment {

    private Button btnCreate;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_create_options, container, false);

        btnCreate = view.findViewById(R.id.btn_create);

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                dismiss();
            });
        }

        return view;
    }
}