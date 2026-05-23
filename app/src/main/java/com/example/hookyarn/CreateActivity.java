package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CreateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String createType = getIntent().getStringExtra("create_type");

        if (createType != null) {

            switch (createType) {

                case "project":
                    Intent projectIntent = new Intent(CreateActivity.this, ProjectDetailActivity.class);
                    projectIntent.putExtra(ProjectDetailActivity.EXTRA_IS_NEW, true);
                    projectIntent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_TITLE, "");
                    startActivity(projectIntent);
                    break;

                case "folder":
                    Intent folderIntent = new Intent(CreateActivity.this, CreateFolderActivity.class);
                    folderIntent.putExtra("mode", "create");
                    startActivity(folderIntent);
                    break;

                case "post":
                    Intent postIntent = new Intent(CreateActivity.this, CreatePostActivity.class);
                    postIntent.putExtra("mode", "create");
                    startActivity(postIntent);
                    break;
            }
        }

        finish();
    }
}