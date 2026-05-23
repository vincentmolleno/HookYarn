package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class CreateFolderActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText folderNameInput;
    private TextInputEditText folderDescriptionInput;
    private RecyclerView projectsRecyclerView;
    private LinearLayout btnAddProject;
    private MaterialButton btnCreate;
    private ImageView folderIconPreview;
    private TextView btnChangeIcon;

    private ArrayList<ProjectModel> projectList;
    private ProjectAdapter projectAdapter;

    private final ActivityResultLauncher<Intent> addProjectLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String projectName = data.getStringExtra("project_name");
                    String projectDesc = data.getStringExtra("project_description");
                    String projectId = data.getStringExtra("project_id");

                    ProjectModel newProject = new ProjectModel();
                    newProject.setTitle(projectName != null ? projectName : "New Project");
                    newProject.setDescription(projectDesc != null ? projectDesc : "");
                    newProject.setId(projectId);

                    projectList.add(newProject);
                    projectAdapter.notifyItemInserted(projectList.size() - 1);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_folder);

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        folderNameInput = findViewById(R.id.folderNameInput);
        folderDescriptionInput = findViewById(R.id.folderDescriptionInput);
        projectsRecyclerView = findViewById(R.id.projectsRecyclerView);
        btnAddProject = findViewById(R.id.btnAddProject);
        btnCreate = findViewById(R.id.btnCreate);
        folderIconPreview = findViewById(R.id.folderIconPreview);
        btnChangeIcon = findViewById(R.id.btnChangeIcon);

        projectList = new ArrayList<>();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> handleBackNavigation());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        btnAddProject.setOnClickListener(v -> showAddProjectDialog());

        btnCreate.setOnClickListener(v -> createFolder());

        btnChangeIcon.setOnClickListener(v -> openIconPicker());
    }

    private void handleBackNavigation() {
        Intent intent = new Intent(CreateFolderActivity.this, HomeActivity.class);
        intent.putExtra("SHOW_CREATE_SHEET", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerView() {
        projectAdapter = new ProjectAdapter(projectList, new ProjectAdapter.OnProjectClickListener() {
            @Override
            public void onProjectClick(ProjectModel project) {
                openProjectDetails(project);
            }

            @Override
            public void onProjectRemove(ProjectModel project, int position) {
                projectList.remove(position);
                projectAdapter.notifyItemRemoved(position);
                Toast.makeText(CreateFolderActivity.this, "Project removed", Toast.LENGTH_SHORT).show();
            }
        });

        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        projectsRecyclerView.setAdapter(projectAdapter);
    }

    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Project");

        String[] options = {"Create new project", "Add existing project"};

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                String currentFolderName = folderNameInput.getText().toString().trim();
                Intent intent = new Intent(CreateFolderActivity.this, ProjectDetailActivity.class);
                intent.putExtra(ProjectDetailActivity.EXTRA_IS_NEW, true);
                if (!currentFolderName.isEmpty()) {
                    intent.putExtra(ProjectDetailActivity.EXTRA_FOLDER_NAME, currentFolderName);
                }
                addProjectLauncher.launch(intent);
            } else {
                showExistingProjectsDialog();
            }
        });

        builder.show();
    }

    private void showExistingProjectsDialog() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("projects")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No projects found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<ProjectModel> projects = querySnapshot.toObjects(ProjectModel.class);
                    String[] projectTitles = new String[projects.size()];
                    for (int i = 0; i < projects.size(); i++) {
                        projectTitles[i] = projects.get(i).getTitle();
                        if (projectTitles[i] == null) projectTitles[i] = "Untitled";
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Select Project");
                    builder.setItems(projectTitles, (dialog, which) -> {
                        ProjectModel selected = projects.get(which);
                        projectList.add(selected);
                        projectAdapter.notifyItemInserted(projectList.size() - 1);
                        Toast.makeText(CreateFolderActivity.this, "Project added", Toast.LENGTH_SHORT).show();
                    });
                    builder.show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openIconPicker() {
        int[] icons = {R.drawable.ic_folder_large, R.drawable.ic_favorites, R.drawable.ic_videos, R.drawable.ic_photo};
        String[] iconNames = {"Folder", "Favorites", "Videos", "Photos"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Folder Icon");

        builder.setItems(iconNames, (dialog, which) -> {
            folderIconPreview.setImageResource(icons[which]);
        });

        builder.show();
    }

    private void openProjectDetails(ProjectModel project) {
        Toast.makeText(this, "Opening: " + project.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void createFolder() {
        String folderName = folderNameInput.getText().toString().trim();
        String description = folderDescriptionInput.getText().toString().trim();

        if (folderName.isEmpty()) {
            folderNameInput.setError("Please enter a folder name");
            return;
        }

        Folder newFolder = new Folder();
        newFolder.setName(folderName);
        newFolder.setDescription(description);
        newFolder.setProjects(projectList);
        newFolder.setIcon(R.drawable.ic_folder_large);

        saveFolderToDatabase(newFolder);

        Toast.makeText(this, "Folder created successfully!", Toast.LENGTH_LONG).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("folder_name", folderName);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void saveFolderToDatabase(Folder folder) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        List<String> projectIds = new ArrayList<>();
        for (ProjectModel pm : folder.getProjects()) {
            if (pm.getId() != null) {
                projectIds.add(pm.getId());
            }
        }

        java.util.Map<String, Object> folderData = new java.util.HashMap<>();
        folderData.put("name", folder.getName());
        folderData.put("description", folder.getDescription());
        folderData.put("uid", uid);
        folderData.put("projectIds", projectIds);
        folderData.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("folders")
                .add(folderData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Folder saved to cloud", Toast.LENGTH_SHORT).show();
                    for (ProjectModel pm : folder.getProjects()) {
                        if (pm.getId() != null) {
                            db.collection("projects").document(pm.getId())
                                    .update("folderName", folder.getName());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
