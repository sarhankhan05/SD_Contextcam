package com.example.sd_contextcam.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "tags")
public class Tag {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    
    // For nested tags, this references the parent tag id
    // -1 means no parent (root level tag)
    public int parentId = -1;

    @Ignore
    public Tag(String name) {
        this.name = name;
    }

    @Ignore
    public Tag(String name, int parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    // Default constructor for Room
    public Tag() {
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
}