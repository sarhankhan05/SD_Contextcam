package com.example.sd_contextcam; // Make sure this package name is correct

import java.util.List; // <--- ADD THIS IMPORT

public class BatchItem {
    private String title;
    private String description;
    private int photoCount;
    private List<String> photoPaths; // <--- ADD THIS

    // --- MODIFIED CONSTRUCTOR ---
    public BatchItem(String title, String description, int photoCount, List<String> photoPaths) {
        this.title = title;
        this.description = description;
        this.photoCount = photoCount;
        this.photoPaths = photoPaths; // <--- ADD THIS
    }

    // --- Getter methods ---
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    // --- ADD THIS NEW GETTER ---
    public List<String> getPhotoPaths() {
        return photoPaths;
    }
}