package com.example.sd_contextcam;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.data.Tag;
import com.example.sd_contextcam.viewmodel.PhotoViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class GalleryActivity extends AppCompatActivity {
    private static final String TAG = "GalleryActivity";
    private RecyclerView photosRecyclerView;
    private RecyclerView tagsRecyclerView;  // New RecyclerView for tags
    private GalleryAdapter galleryAdapter;
    private TagsAdapter tagsAdapter;  // New adapter for tags
    private LinearLayout tagFilterLayout;
    private Button dateFilterButton;
    private Button clearFiltersButton;
    private TextView emptyStateText;
    private View tagsView;  // View to show tags list
    private View photosView;  // View to show photos grid
    
    private PhotoViewModel photoViewModel;
    private Tag currentTag;  // Keep track of currently selected tag
    
    // Biometric authentication
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_gallery);
        
        initViews();
        setupRecyclerViews();
        setupViewModel();
        setupBiometricAuthentication();  // Setup biometric authentication
        observeViewModel();
        setupFilters();
    }
    
    private void initViews() {
        Log.d(TAG, "Initializing views");
        photosRecyclerView = findViewById(R.id.photosRecyclerView);
        tagsRecyclerView = findViewById(R.id.tagsRecyclerView);  // New tags RecyclerView
        tagFilterLayout = findViewById(R.id.tagFilterLayout);
        dateFilterButton = findViewById(R.id.dateFilterButton);
        clearFiltersButton = findViewById(R.id.clearFiltersButton);
        emptyStateText = findViewById(R.id.emptyStateText);
        tagsView = findViewById(R.id.tagsView);  // New tags view
        photosView = findViewById(R.id.photosView);  // New photos view
    }
    
    private void setupRecyclerViews() {
        Log.d(TAG, "Setting up RecyclerViews");
        // Setup photos RecyclerView
        galleryAdapter = new GalleryAdapter();
        photosRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        photosRecyclerView.setAdapter(galleryAdapter);
        
        // Setup tags RecyclerView
        tagsAdapter = new TagsAdapter();
        tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tagsRecyclerView.setAdapter(tagsAdapter);
        
        tagsAdapter.setOnTagClickListener(tag -> {
            // Show photos for this tag
            showPhotosForTag(tag);
        });
    }
    
    private void setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Authentication successful, show vault content
                showVaultPhotos();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(GalleryActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(GalleryActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
        
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Vault Authentication")
                .setSubtitle("Authenticate to access Vault")
                .setNegativeButtonText("Cancel")
                .build();
    }
    
    private void authenticateForVault() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Biometric features are available
                biometricPrompt.authenticate(promptInfo);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "No biometric features available on this device", Toast.LENGTH_SHORT).show();
                // TODO: Fall back to PIN authentication
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric features are currently unavailable", Toast.LENGTH_SHORT).show();
                // TODO: Fall back to PIN authentication
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No biometric credentials enrolled. Please enroll in Settings", Toast.LENGTH_SHORT).show();
                // TODO: Fall back to PIN authentication
                break;
        }
    }
    
    private void showVaultPhotos() {
        // TODO: Load and show vault photos
        Toast.makeText(this, "Vault access granted", Toast.LENGTH_SHORT).show();
        // For now, just show an empty vault view
        showPhotosForTag(null); // Pass null for vault
    }
    
    private void showPhotosForTag(Tag tag) {
        currentTag = tag;
        
        // Show filter buttons when viewing photos by tag
        findViewById(R.id.tagFilterContainer).setVisibility(View.VISIBLE);
        dateFilterButton.setVisibility(View.VISIBLE);
        clearFiltersButton.setVisibility(View.VISIBLE);
        
        // Hide tags view and show photos view
        tagsView.setVisibility(View.GONE);
        photosView.setVisibility(View.VISIBLE);
        
        if (tag != null) {
            // Load photos for this tag on a background thread
            photoViewModel.getPhotosByTag(tag.id).observe(this, photos -> {
                if (photos != null) {
                    Log.d(TAG, "Photos loaded for tag " + tag.name + ": " + photos.size());
                    galleryAdapter.setPhotos(photos);
                    
                    // Show empty state if no photos
                    if (photos.isEmpty()) {
                        Log.d(TAG, "No photos found for tag, showing empty state");
                        emptyStateText.setVisibility(View.VISIBLE);
                        photosRecyclerView.setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "Photos found for tag, showing gallery");
                        emptyStateText.setVisibility(View.GONE);
                        photosRecyclerView.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else {
            // Show vault photos (empty for now)
            galleryAdapter.setPhotos(new ArrayList<>());
            emptyStateText.setVisibility(View.VISIBLE);
            photosRecyclerView.setVisibility(View.GONE);
        }
    }
    
    private void showTagsView() {
        // Show tags view and hide photos view
        tagsView.setVisibility(View.VISIBLE);
        photosView.setVisibility(View.GONE);
    }
    
    private void setupViewModel() {
        Log.d(TAG, "Setting up ViewModel");
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.loadPhotos();
        photoViewModel.loadTags();
    }
    
    private void observeViewModel() {
        Log.d(TAG, "Observing ViewModel");
        photoViewModel.getPhotos().observe(this, photos -> {
            if (photos != null) {
                Log.d(TAG, "Photos loaded: " + photos.size());
                galleryAdapter.setPhotos(photos);
                
                // Show empty state if no photos
                if (photos.isEmpty()) {
                    Log.d(TAG, "No photos found, showing empty state");
                    emptyStateText.setVisibility(View.VISIBLE);
                    photosRecyclerView.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Photos found, showing gallery");
                    emptyStateText.setVisibility(View.GONE);
                    photosRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
        
        photoViewModel.getTags().observe(this, tags -> {
            if (tags != null) {
                Log.d(TAG, "Tags loaded: " + tags.size());
                // Get photo counts for each tag on a background thread
                new Thread(() -> {
                    List<Integer> photoCounts = new ArrayList<>();
                    for (Tag tag : tags) {
                        int count = photoViewModel.getRepository().getPhotoCountForTag(tag.id);
                        photoCounts.add(count);
                    }
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        tagsAdapter.setTags(tags, photoCounts);
                        showTagsView();  // Show tags view when tags are loaded
                    });
                }).start();
            }
        });
        
        photoViewModel.getIsLoading().observe(this, isLoading -> {
            // TODO: Show/hide loading indicator
        });
    }
    
    // Add this method to refresh photos when returning to the gallery
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh photos and tags when returning to the gallery
        photoViewModel.loadPhotos();
        photoViewModel.loadTags();
    }
    
    private void setupFilters() {
        Log.d(TAG, "Setting up filters");
        dateFilterButton.setOnClickListener(v -> {
            // Show date filter dialog
            showDateFilterDialog();
        });
        
        clearFiltersButton.setOnClickListener(v -> {
            // Clear all filters and show all photos for current tag
            if (currentTag != null) {
                showPhotosForTag(currentTag);
            } else {
                photoViewModel.loadPhotos();
            }
        });
    }
    
    private void showDateFilterDialog() {
        // Create a simple month/year picker
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        
        // For simplicity, we'll just show a toast with the current date
        // In a real implementation, this would be a proper date picker dialog
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        
        Toast.makeText(this, "Filter by: " + months[month] + " " + year, Toast.LENGTH_SHORT).show();
        
        // TODO: Implement actual date filtering
        // This would filter the currently displayed photos by date
        // For now, we'll just show a message
        Toast.makeText(this, "Date filtering would filter photos by selected date range", Toast.LENGTH_LONG).show();
    }
}