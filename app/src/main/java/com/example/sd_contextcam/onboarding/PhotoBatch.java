package com.example.sd_contextcam.onboarding;

import java.util.List;
import java.util.Objects;

public class PhotoBatch {
    private String title;
    private String description;
    private List<String> photoPaths;
    private String suggestedTag;
    
    public PhotoBatch(String title, String description, List<String> photoPaths, String suggestedTag) {
        this.title = title;
        this.description = description;
        this.photoPaths = photoPaths;
        this.suggestedTag = suggestedTag;
    }
    
    // Getters and setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getPhotoPaths() {
        return photoPaths;
    }
    
    public void setPhotoPaths(List<String> photoPaths) {
        this.photoPaths = photoPaths;
    }
    
    public String getSuggestedTag() {
        return suggestedTag;
    }
    
    public void setSuggestedTag(String suggestedTag) {
        this.suggestedTag = suggestedTag;
    }
    
    public int getPhotoCount() {
        return photoPaths != null ? photoPaths.size() : 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoBatch that = (PhotoBatch) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(photoPaths, that.photoPaths) &&
                Objects.equals(suggestedTag, that.suggestedTag);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(title, description, photoPaths, suggestedTag);
    }
}