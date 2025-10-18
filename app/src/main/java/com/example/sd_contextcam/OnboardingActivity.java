package com.example.sd_contextcam;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.data.Tag;
import com.example.sd_contextcam.data.TagRepository;
import com.example.sd_contextcam.onboarding.BatchAdapter;
import com.example.sd_contextcam.onboarding.PhotoBatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnboardingActivity extends AppCompatActivity {
    private RecyclerView batchRecyclerView;
    private BatchAdapter batchAdapter;
    private Button doneButton;
    private ProgressBar progressBar;
    private TextView progressText;
    
    private TagRepository tagRepository;
    private List<PhotoBatch> photoBatches;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        initViews();
        setupRecyclerView();
        setupRepository();
        setupClickListeners();
        
        // Load existing tags for autocomplete
        loadExistingTags();
        
        // Scan for photo batches
        scanPhotoBatches();
    }
    
    private void initViews() {
        batchRecyclerView = findViewById(R.id.batchRecyclerView);
        doneButton = findViewById(R.id.startButton);
        doneButton.setText("Done for now");
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
    }
    
    private void setupRecyclerView() {
        batchAdapter = new BatchAdapter();
        batchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        batchRecyclerView.setAdapter(batchAdapter);
        
        batchAdapter.setOnTagApplyListener((batch, tag) -> {
            // Apply tag to all photos in the batch on a background thread
            new Thread(() -> {
                applyTagToBatchPhotos(batch, tag);
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    // Remove the batch from the list since it's been processed
                    photoBatches.remove(batch);
                    batchAdapter.setBatches(photoBatches);
                });
            }).start();
        });
    }
    
    private void setupRepository() {
        tagRepository = new TagRepository(this);
    }
    
    private void setupClickListeners() {
        doneButton.setOnClickListener(v -> {
            // Finish onboarding and go to main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void loadExistingTags() {
        // Load existing tags on a background thread
        new Thread(() -> {
            List<Tag> tags = tagRepository.getAllTags();
            List<String> tagNames = new ArrayList<>();
            for (Tag tag : tags) {
                tagNames.add(tag.getName());
            }
            
            // Update UI on main thread
            runOnUiThread(() -> {
                batchAdapter.setAllTags(tagNames);
            });
        }).start();
    }
    
    private void scanPhotoBatches() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressText.setText("Scanning your photo library...");
        doneButton.setEnabled(false);
        
        // Scan for photos in a background thread
        new Thread(() -> {
            photoBatches = new ArrayList<>();
            
            // Scan for photos in all common directories
            List<Photo> allPhotos = new ArrayList<>();
            scanAllPhotoDirectories(allPhotos);
            
            // Group photos into batches
            groupPhotosIntoBatches(allPhotos);
            
            // Run on UI thread to update UI
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                progressText.setVisibility(View.GONE);
                doneButton.setEnabled(true);
                
                batchAdapter.setBatches(photoBatches);
            });
        }).start();
    }
    
    private void scanDirectoryForPhotos(File directory, List<Photo> photos) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursively scan subdirectories
                        scanDirectoryForPhotos(file, photos);
                    } else if (isImageFile(file)) {
                        // Create a Photo object for this image file
                        Photo photo = new Photo();
                        photo.setFilePath(file.getAbsolutePath());
                        photo.setTimestamp(file.lastModified());
                        // For now, we're not setting location data
                        photos.add(photo);
                    }
                }
            }
        }
    }
    
    // Add a method to scan all common photo directories
    private void scanAllPhotoDirectories(List<Photo> photos) {
        // Common photo directories
        File[] photoDirs = {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            new File(Environment.getExternalStorageDirectory(), "Camera"),
            new File(Environment.getExternalStorageDirectory(), "Photos"),
            new File(Environment.getExternalStorageDirectory(), "Images")
        };
        
        for (File dir : photoDirs) {
            scanDirectoryForPhotos(dir, photos);
        }
    }
    
    private boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || fileName.endsWith(".gif") ||
               fileName.endsWith(".bmp") || fileName.endsWith(".webp");
    }
    
    private void groupPhotosIntoBatches(List<Photo> allPhotos) {
        // Group photos by directory
        Map<String, List<Photo>> directoryGroups = new HashMap<>();
        for (Photo photo : allPhotos) {
            File file = new File(photo.getFilePath());
            String parentDir = file.getParentFile().getName();
            directoryGroups.computeIfAbsent(parentDir, k -> new ArrayList<>()).add(photo);
        }
        
        // Create batches for directories with many photos
        for (Map.Entry<String, List<Photo>> entry : directoryGroups.entrySet()) {
            String dirName = entry.getKey();
            List<Photo> photos = entry.getValue();
            
            // Only create batches for directories with more than 10 photos
            if (photos.size() > 10) {
                List<String> photoPaths = new ArrayList<>();
                for (Photo photo : photos) {
                    photoPaths.add(photo.getFilePath());
                }
                
                PhotoBatch batch = new PhotoBatch(
                        dirName,
                        photos.size() + " photos",
                        photoPaths,
                        dirName
                );
                photoBatches.add(batch);
            }
        }
        
        // Create batches for date ranges (last 7 days, last 30 days, etc.)
        // This is a simplified implementation
        long now = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60 * 1000;
        long oneWeek = 7 * oneDay;
        long oneMonth = 30 * oneDay;
        
        // Photos from last week
        List<Photo> lastWeekPhotos = new ArrayList<>();
        for (Photo photo : allPhotos) {
            if (now - photo.getTimestamp() <= oneWeek) {
                lastWeekPhotos.add(photo);
            }
        }
        
        if (lastWeekPhotos.size() > 5) {
            List<String> photoPaths = new ArrayList<>();
            for (Photo photo : lastWeekPhotos) {
                photoPaths.add(photo.getFilePath());
            }
            
            PhotoBatch batch = new PhotoBatch(
                    "Recent Photos",
                    lastWeekPhotos.size() + " photos from last week",
                    photoPaths,
                    "Recent"
            );
            photoBatches.add(batch);
        }
        
        // If we don't have enough batches, create some sample ones for demonstration
        if (photoBatches.isEmpty()) {
            createSampleBatches();
        }
    }
    
    private void createSampleBatches() {
        // Create some sample batches for demonstration
        List<String> samplePaths1 = Arrays.asList("path1.jpg", "path2.jpg", "path3.jpg");
        PhotoBatch batch1 = new PhotoBatch(
                "Screenshots",
                "1,800 photos",
                samplePaths1,
                "Screenshots"
        );
        
        List<String> samplePaths2 = Arrays.asList("path4.jpg", "path5.jpg");
        PhotoBatch batch2 = new PhotoBatch(
                "WhatsApp Images",
                "2,500 photos",
                samplePaths2,
                "WhatsApp Images"
        );
        
        List<String> samplePaths3 = Arrays.asList("path6.jpg", "path7.jpg", "path8.jpg");
        PhotoBatch batch3 = new PhotoBatch(
                "Photos from Oct 14-16, 2025",
                "72 photos",
                samplePaths3,
                "Navratri 2025"
        );
        
        List<String> samplePaths4 = Arrays.asList("path9.jpg", "path10.jpg");
        PhotoBatch batch4 = new PhotoBatch(
                "Photos from MSU Faculty of Tech",
                "115 photos",
                samplePaths4,
                "College"
        );
        
        photoBatches.add(batch1);
        photoBatches.add(batch2);
        photoBatches.add(batch3);
        photoBatches.add(batch4);
    }
    
    private void applyTagToBatchPhotos(PhotoBatch batch, String tagName) {
        // Get or create the tag
        Tag tag = tagRepository.getTagByName(tagName);
        if (tag == null) {
            tag = new Tag();
            tag.setName(tagName);
            tagRepository.insertTag(tag);
            // Get the updated tag with its ID
            tag = tagRepository.getTagByName(tagName);
        }
        
        // Apply tag to all photos in the batch
        for (String photoPath : batch.getPhotoPaths()) {
            // Get or create photo in database
            Photo photo = tagRepository.getPhotoByFilePath(photoPath);
            if (photo == null) {
                photo = new Photo();
                photo.setFilePath(photoPath);
                photo.setTimestamp(System.currentTimeMillis()); // Use current time if not available
                long photoId = tagRepository.insertPhoto(photo);
                // Get the inserted photo with its ID
                photo = tagRepository.getPhotoById((int) photoId);
            }
            
            // Add tag to photo
            if (photo != null) {
                tagRepository.addTagToPhoto(photo.getId(), tag.getId());
            }
        }
        
        // Show toast on main thread
        runOnUiThread(() -> {
            Toast.makeText(this, "Tag '" + tagName + "' applied to " + batch.getPhotoCount() + " photos", Toast.LENGTH_SHORT).show();
        });
    }
}