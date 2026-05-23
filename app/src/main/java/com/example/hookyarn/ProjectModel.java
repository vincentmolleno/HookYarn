package com.example.hookyarn;

import com.google.firebase.Timestamp;
import java.util.List;

public class ProjectModel {

    private String id;
    private String uid;
    private String title;
    private String description;
    private String status;
    private String folderName;
    private List<StepModel> steps;
    private Timestamp updatedAt;
    private Timestamp createdAt;

    public ProjectModel() {}

    public ProjectModel(String uid, String title, String description, String status) {
        this.uid         = uid;
        this.title       = title;
        this.description = description;
        this.status      = status;
        this.createdAt   = Timestamp.now();
        this.updatedAt   = Timestamp.now();
    }

    public String getId()          { return id; }
    public String getUid()         { return uid; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getStatus()      { return status; }
    public String getFolderName()  { return folderName; }
    public List<StepModel> getSteps() { return steps; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(String id)                   { this.id = id; }
    public void setUid(String uid)                 { this.uid = uid; }
    public void setTitle(String title)             { this.title = title; }
    public void setDescription(String desc)        { this.description = desc; }
    public void setStatus(String status)           { this.status = status; }
    public void setFolderName(String folderName)   { this.folderName = folderName; }
    public void setSteps(List<StepModel> steps)    { this.steps = steps; }
    public void setUpdatedAt(Timestamp ts)         { this.updatedAt = ts; }
    public void setCreatedAt(Timestamp ts)         { this.createdAt = ts; }

    public static class StepModel {
        private String label;
        private boolean done;

        public StepModel() {}
        public StepModel(String label, boolean done) {
            this.label = label;
            this.done  = done;
        }
        public String  getLabel() { return label; }
        public boolean isDone()   { return done; }
        public void setLabel(String label) { this.label = label; }
        public void setDone(boolean done)  { this.done = done; }
    }
}