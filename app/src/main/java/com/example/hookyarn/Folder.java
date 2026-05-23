package com.example.hookyarn;

import java.util.ArrayList;

public class Folder {
    private String name;
    private String description;
    private int icon;
    private ArrayList<ProjectModel> projects;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getIcon() { return icon; }
    public void setIcon(int icon) { this.icon = icon; }

    public ArrayList<ProjectModel> getProjects() { return projects; }
    public void setProjects(ArrayList<ProjectModel> projects) { this.projects = projects; }
}