package com.example.hookyarn;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectDetailActivity extends AppCompatActivity {

    // Intent keys
    public static final String EXTRA_PROJECT_ID     = "project_id";
    public static final String EXTRA_PROJECT_TITLE  = "project_title";
    public static final String EXTRA_PROJECT_STATUS = "project_status";
    public static final String EXTRA_IS_NEW         = "is_new_project";
    public static final String EXTRA_FOLDER_NAME    = "folder_name";

    // UI refs
    private Toolbar      toolbar;
    private EditText     etProjectTitle;
    private ImageButton  btnTitleToggle;
    private TextView     tvStatus;
    private TextView     tvUpdated;
    private RecyclerView rvSteps;
    private LinearLayout emptySteps;
    private View         btnAddStep;

    // State
    private boolean isTitleEditing = false;
    private String  projectId      = null;
    private boolean isNewProject   = false;
    private String  folderName     = null;

    // Firebase
    private FirebaseFirestore    db;
    private FirebaseAuth         auth;
    private ListenerRegistration stepsListener;

    // Steps list
    private final List<StepModel> stepList = new ArrayList<>();
    private StepAdapter stepAdapter;

    private final ActivityResultLauncher<Intent> stepActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String returnedProjectId = result.getData().getStringExtra("projectId");
                    if (isNewProject && returnedProjectId != null) {
                        projectId = returnedProjectId;
                        isNewProject = false;
                        attachStepsListener();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        setupToolbar();
        readIntent();
        setupBackButton(); // Call after readIntent
        setupTitleToggle();
        setupStepsSection();

        if (projectId != null && !isNewProject) {
            loadProjectFromFirestore();
            attachStepsListener();
        }
    }

    private void bindViews() {
        toolbar        = findViewById(R.id.toolbar);
        etProjectTitle = findViewById(R.id.etProjectTitle);
        btnTitleToggle = findViewById(R.id.btnTitleToggle);
        tvStatus       = findViewById(R.id.tvProjectDetailStatus);
        tvUpdated      = findViewById(R.id.tvProjectDetailUpdated);
        rvSteps        = findViewById(R.id.rvSteps);
        emptySteps     = findViewById(R.id.emptySteps);
        btnAddStep     = findViewById(R.id.btnAddStep);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    private void setupBackButton() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle Toolbar navigation icon click
        toolbar.setNavigationOnClickListener(v -> handleBackPress());

        // Handle system back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_project_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Handle back button from action bar
        if (itemId == android.R.id.home) {
            handleBackPress();
            return true;
        }
        // Handle save button from menu
        else if (itemId == R.id.action_save) {
            handleSaveButton();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleBackPress() {
        // Force close keyboard and clear focus
        forceCloseKeyboard();

        // If title was being edited, commit changes
        if (isTitleEditing) {
            commitTitleEdit();
            setTitleViewMode();
        }

        // If it's a new project with added steps but NOT yet saved to Firestore
        if (isNewProject && projectId == null && !stepList.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have added steps. Do you want to save the project before leaving?")
                    .setPositiveButton("Save", (dialog, which) -> saveProjectAndExit())
                    .setNegativeButton("Discard", (dialog, which) -> finish())
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            finish();
        }
    }

    private void forceCloseKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        }
    }

    private void handleSaveButton() {
        // Close title editing mode if open
        if (isTitleEditing) {
            if (commitTitleEdit()) {
                setTitleViewMode();
            } else {
                return; // Title is empty, don't proceed
            }
        }

        // Save the project
        saveProjectAndExit();
    }

    private void saveProjectAndExit() {
        String title = etProjectTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a project title", Toast.LENGTH_SHORT).show();
            setTitleEditMode();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("status", tvStatus.getText() != null ? tvStatus.getText().toString() : "In Progress");
        data.put("updatedAt", new Date());
        data.put("uid", user.getUid());
        if (folderName != null) {
            data.put("folderName", folderName);
        }

        if (isNewProject || projectId == null) {
            data.put("createdAt", new Date());
            // Create new project
            db.collection("projects").add(data)
                    .addOnSuccessListener(ref -> {
                        projectId = ref.getId();
                        isNewProject = false;

                        // Save steps if any
                        if (!stepList.isEmpty()) {
                            saveAllStepsToFirestore();
                        } else {
                            Toast.makeText(this, "Project saved!", Toast.LENGTH_SHORT).show();
                            postProjectToFeed(title, projectId);
                            goToHomeActivity();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Update existing project
            db.collection("projects").document(projectId)
                    .update(data)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Project updated!", Toast.LENGTH_SHORT).show();
                        goToHomeActivity();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveAllStepsToFirestore() {
        if (projectId == null) {
            goToHomeActivity();
            return;
        }

        // Save steps to Firestore
        for (int i = 0; i < stepList.size(); i++) {
            StepModel step = stepList.get(i);
            Map<String, Object> stepData = new HashMap<>();
            stepData.put("title", step.getTitle());
            stepData.put("note", step.getNote());
            stepData.put("order", i);
            stepData.put("createdAt", new Date());

            final int index = i;
            db.collection("projects").document(projectId)
                    .collection("steps")
                    .add(stepData)
                    .addOnSuccessListener(docRef -> {
                        // Update the step with Firestore ID
                        StepModel updatedStep = new StepModel(docRef.getId(), step.getTitle(), step.getNote(), step.getOrder());
                        stepList.set(index, updatedStep);

                        // If this is the last step, go to home
                        if (index == stepList.size() - 1) {
                            Toast.makeText(this, "Project and " + stepList.size() + " steps saved!", Toast.LENGTH_SHORT).show();
                            postProjectToFeed(etProjectTitle.getText().toString().trim(), projectId);
                            goToHomeActivity();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save some steps", Toast.LENGTH_SHORT).show();
                        goToHomeActivity();
                    });
        }

        // If no steps, just go home
        if (stepList.isEmpty()) {
            goToHomeActivity();
        }
    }

    private void postProjectToFeed(String title, String pid) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            String username = doc.getString("username");
            String profileUrl = doc.getString("profileImageUrl");

            Map<String, Object> post = new HashMap<>();
            post.put("id", db.collection("posts").document().getId());
            post.put("uid", user.getUid());
            post.put("username", username);
            post.put("profileImageUrl", profileUrl);
            post.put("caption", "I just created a new project: " + title);
            post.put("imageUrl", ""); // Default or placeholder
            post.put("likeCount", 0);
            post.put("commentCount", 0);
            post.put("likedBy", new ArrayList<String>());
            post.put("createdAt", new com.google.firebase.Timestamp(new Date()));
            post.put("projectId", pid); // Link to the project

            db.collection("posts").add(post);
        });
    }

    private void goToHomeActivity() {
        if (getCallingActivity() != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("project_id", projectId);
            resultIntent.putExtra("project_name", etProjectTitle.getText().toString().trim());
            resultIntent.putExtra("project_description", ""); // Or add a desc field if needed
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void readIntent() {
        Intent i = getIntent();
        projectId    = i.getStringExtra(EXTRA_PROJECT_ID);
        isNewProject = i.getBooleanExtra(EXTRA_IS_NEW, false);
        folderName   = i.getStringExtra(EXTRA_FOLDER_NAME);

        String title  = i.getStringExtra(EXTRA_PROJECT_TITLE);
        String status = i.getStringExtra(EXTRA_PROJECT_STATUS);

        if (!TextUtils.isEmpty(title)) {
            etProjectTitle.setText(title);
            setToolbarTitle(title);
        }
        if (!TextUtils.isEmpty(status)) tvStatus.setText(status);
        tvUpdated.setText(formatDate(new Date()));

        if (!isNewProject && projectId != null) {
            loadProjectFromFirestore();
            attachStepsListener();
        }
    }

    private void setupTitleToggle() {
        setTitleViewMode();

        btnTitleToggle.setOnClickListener(v -> {
            if (!isTitleEditing) {
                setTitleEditMode();
            } else {
                if (commitTitleEdit()) {
                    setTitleViewMode();
                    saveTitleToFirestore();
                }
            }
        });

        etProjectTitle.setOnEditorActionListener((v, actionId, event) -> {
            if (commitTitleEdit()) {
                setTitleViewMode();
                saveTitleToFirestore();
            }
            return true;
        });
    }

    private void saveTitleToFirestore() {
        if (projectId != null && !isNewProject) {
            String title = etProjectTitle.getText().toString().trim();
            Map<String, Object> updates = new HashMap<>();
            updates.put("title", title);
            updates.put("updatedAt", new Date());

            db.collection("projects").document(projectId)
                    .update(updates)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update title", Toast.LENGTH_SHORT).show());
        }
    }

    private void setTitleEditMode() {
        isTitleEditing = true;
        etProjectTitle.setEnabled(true);
        etProjectTitle.requestFocus();
        etProjectTitle.setSelection(etProjectTitle.getText().length());
        etProjectTitle.setBackgroundResource(android.R.drawable.edit_text);
        btnTitleToggle.setImageResource(R.drawable.ic_check);
        showKeyboard(etProjectTitle);
    }

    private void setTitleViewMode() {
        isTitleEditing = false;
        etProjectTitle.setEnabled(false);
        etProjectTitle.setBackgroundResource(android.R.color.transparent);
        btnTitleToggle.setImageResource(R.drawable.ic_edit);
        hideKeyboard(etProjectTitle);
    }

    private boolean commitTitleEdit() {
        String title = etProjectTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            etProjectTitle.setError("Title cannot be empty");
            return false;
        }
        setToolbarTitle(title);
        return true;
    }

    private void setupStepsSection() {
        stepAdapter = new StepAdapter(stepList,
                (step, pos) -> {
                    Intent intent = new Intent(ProjectDetailActivity.this, ItemStepActivity.class);
                    intent.putExtra("projectId", projectId);
                    intent.putExtra("stepId", step.getId());
                    intent.putExtra("stepTitle", step.getTitle());
                    intent.putExtra("stepNote", step.getNote());
                    intent.putExtra("isNewProject", isNewProject);
                    intent.putExtra("projectTitle", etProjectTitle.getText().toString());
                    stepActivityLauncher.launch(intent);
                },
                (step, pos) -> confirmDeleteStep(step, pos)
        );
        rvSteps.setLayoutManager(new LinearLayoutManager(this));
        rvSteps.setAdapter(stepAdapter);

        btnAddStep.setOnClickListener(v -> {
            Intent intent = new Intent(ProjectDetailActivity.this, ItemStepActivity.class);
            intent.putExtra("projectId", projectId);
            intent.putExtra("isNewProject", isNewProject);
            intent.putExtra("projectTitle", etProjectTitle.getText().toString());
            stepActivityLauncher.launch(intent);
        });

        updateStepsVisibility();
    }

    private void updateStepsVisibility() {
        boolean empty = stepList.isEmpty();
        rvSteps.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptySteps.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void confirmDeleteStep(StepModel step, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Step")
                .setMessage("Delete \"" + step.getTitle() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (projectId != null && !isNewProject && step.getId() != null) {
                        deleteStepFromFirestore(step.getId());
                    } else {
                        // Local update only if not in Firestore yet
                        stepList.remove(position);
                        stepAdapter.notifyItemRemoved(position);
                        updateStepsVisibility();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProjectFromFirestore() {
        db.collection("projects").document(projectId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String title = doc.getString("title");
                    String status = doc.getString("status");
                    Date updated = doc.getDate("updatedAt");

                    if (title != null) {
                        etProjectTitle.setText(title);
                        setToolbarTitle(title);
                    }
                    if (status != null) tvStatus.setText(status);
                    if (updated != null) tvUpdated.setText(formatDate(updated));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load project", Toast.LENGTH_SHORT).show());
    }

    private void attachStepsListener() {
        if (projectId == null) return;
        if (stepsListener != null) stepsListener.remove();

        stepsListener = db.collection("projects")
                .document(projectId)
                .collection("steps")
                .orderBy("order", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    stepList.clear();
                    for (var doc : snapshots.getDocuments()) {
                        Long orderVal = doc.getLong("order");
                        stepList.add(new StepModel(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("note"),
                                orderVal != null ? orderVal.intValue() : 0
                        ));
                    }
                    stepAdapter.notifyDataSetChanged();
                    updateStepsVisibility();
                });
    }

    private void addStepToFirestore(String title, String note) {
        if (projectId == null) return;

        Map<String, Object> step = new HashMap<>();
        step.put("title", title);
        step.put("note", note);
        step.put("order", stepList.size());
        step.put("createdAt", new Date());

        db.collection("projects").document(projectId)
                .collection("steps")
                .add(step)
                .addOnSuccessListener(docRef -> {
                    for (int i = 0; i < stepList.size(); i++) {
                        StepModel s = stepList.get(i);
                        if (s.getTitle().equals(title) && s.getId() == null) {
                            StepModel updatedStep = new StepModel(docRef.getId(), s.getTitle(), s.getNote(), s.getOrder());
                            stepList.set(i, updatedStep);
                            stepAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    Toast.makeText(this, "Step saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add step", Toast.LENGTH_SHORT).show());
    }

    private void updateStepInFirestore(String stepId, String title, String note) {
        if (projectId == null || stepId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("note", note);
        updates.put("updatedAt", new Date());

        db.collection("projects").document(projectId)
                .collection("steps").document(stepId)
                .update(updates)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update step", Toast.LENGTH_SHORT).show());
    }

    private void deleteStepFromFirestore(String stepId) {
        if (projectId == null || stepId == null) return;

        db.collection("projects").document(projectId)
                .collection("steps").document(stepId)
                .delete()
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete step", Toast.LENGTH_SHORT).show());
    }

    private void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()).format(date);
    }

    private void showKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stepsListener != null) stepsListener.remove();
    }

    // Step Model Class
    public static class StepModel {
        private final String id;
        private final String title;
        private final String note;
        private final int order;

        public StepModel(String id, String title, String note, int order) {
            this.id = id;
            this.title = title != null ? title : "";
            this.note = note != null ? note : "";
            this.order = order;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getNote() { return note; }
        public int getOrder() { return order; }
    }

    // Step Adapter
    public interface OnStepAction {
        void invoke(StepModel step, int position);
    }

    public static class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {
        private final List<StepModel> stepList;
        private final OnStepAction onEdit;
        private final OnStepAction onDelete;

        public StepAdapter(List<StepModel> stepList, OnStepAction onEdit, OnStepAction onDelete) {
            this.stepList = stepList;
            this.onEdit = onEdit;
            this.onDelete = onDelete;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StepModel step = stepList.get(position);
            holder.tvStepNumber.setText(String.valueOf(position + 1));
            holder.tvStepTitle.setText(step.getTitle());

            if (!TextUtils.isEmpty(step.getNote())) {
                holder.tvStepNote.setVisibility(View.VISIBLE);
                holder.tvStepNote.setText(step.getNote());
            } else {
                holder.tvStepNote.setVisibility(View.GONE);
            }

            holder.btnStepEdit.setOnClickListener(v -> {
                if (onEdit != null) onEdit.invoke(step, holder.getAdapterPosition());
            });

            holder.btnStepDelete.setOnClickListener(v -> {
                if (onDelete != null) onDelete.invoke(step, holder.getAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            return stepList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStepNumber, tvStepTitle, tvStepNote;
            ImageButton btnStepEdit, btnStepDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStepNumber = itemView.findViewById(R.id.tvStepNumber);
                tvStepTitle = itemView.findViewById(R.id.tvStepTitle);
                tvStepNote = itemView.findViewById(R.id.tvStepNote);
                btnStepEdit = itemView.findViewById(R.id.btnStepEdit);
                btnStepDelete = itemView.findViewById(R.id.btnStepDelete);
            }
        }
    }
}