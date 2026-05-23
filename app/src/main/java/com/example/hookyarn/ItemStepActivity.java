package com.example.hookyarn;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ItemStepActivity extends AppCompatActivity {

    private TextInputEditText etStepTitle, etStepNote;
    private Button btnSaveStep;
    private Toolbar toolbar;

    private String projectId;
    private String stepId;
    private boolean isNewProject;
    private String projectTitle;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_step);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        projectId = getIntent().getStringExtra("projectId");
        stepId = getIntent().getStringExtra("stepId");
        isNewProject = getIntent().getBooleanExtra("isNewProject", false);
        projectTitle = getIntent().getStringExtra("projectTitle");

        etStepTitle = findViewById(R.id.etStepTitle);
        etStepNote = findViewById(R.id.etStepNote);
        btnSaveStep = findViewById(R.id.btnSaveStep);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (stepId != null) {
                getSupportActionBar().setTitle("Edit Step");
                btnSaveStep.setText("Update Step");
                etStepTitle.setText(getIntent().getStringExtra("stepTitle"));
                etStepNote.setText(getIntent().getStringExtra("stepNote"));
            } else {
                getSupportActionBar().setTitle("Add New Step");
            }
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        btnSaveStep.setOnClickListener(v -> saveStep());
    }

    private void saveStep() {
        String title = etStepTitle.getText().toString().trim();
        String note = etStepNote.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etStepTitle.setError("Title is required");
            return;
        }

        if (stepId != null) {
            updateStepInFirestore(title, note);
        } else if (projectId == null) {
            autoSaveProjectAndThenStep(title, note);
        } else {
            addStepToFirestore(title, note);
        }
    }

    private void autoSaveProjectAndThenStep(String stepTitle, String stepNote) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String title = (projectTitle != null && !projectTitle.isEmpty()) ? projectTitle : "Untitled Project";

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("status", "In Progress");
        data.put("updatedAt", new Date());
        data.put("uid", user.getUid());
        data.put("createdAt", new Date());

        db.collection("projects").add(data)
                .addOnSuccessListener(ref -> {
                    projectId = ref.getId();
                    addStepToFirestore(stepTitle, stepNote);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create project", Toast.LENGTH_SHORT).show());
    }

    private void addStepToFirestore(String title, String note) {
        Map<String, Object> step = new HashMap<>();
        step.put("title", title);
        step.put("note", note);
        step.put("createdAt", new Date());

        db.collection("projects").document(projectId).collection("steps")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int nextOrder = queryDocumentSnapshots.size();
                    step.put("order", nextOrder);

                    db.collection("projects").document(projectId)
                            .collection("steps")
                            .add(step)
                            .addOnSuccessListener(docRef -> {
                                Toast.makeText(this, "Step added successfully", Toast.LENGTH_SHORT).show();
                                
                                android.content.Intent data = new android.content.Intent();
                                data.putExtra("projectId", projectId);
                                setResult(RESULT_OK, data);
                                
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to add step", Toast.LENGTH_SHORT).show());
                });
    }

    private void updateStepInFirestore(String title, String note) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("note", note);
        updates.put("updatedAt", new Date());

        db.collection("projects").document(projectId)
                .collection("steps").document(stepId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Step updated successfully", Toast.LENGTH_SHORT).show();
                    
                    android.content.Intent data = new android.content.Intent();
                    data.putExtra("projectId", projectId);
                    setResult(RESULT_OK, data);

                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update step", Toast.LENGTH_SHORT).show());
    }
}