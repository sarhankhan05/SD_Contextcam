package com.example.sd_contextcam;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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

    // ====================== MODIFICATION 1: DECLARE THE BREADCRUMB TEXTVIEW ======================
    private TextView breadcrumbText;
    // ===========================================================================================

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
        setupBiometricAuthentication();
        observeViewModel();
        setupFilters();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (photosView != null && photosView.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "Back pressed from photos view. Returning to tags view.");
                    showTagsView();

                    currentTag = null;
                    findViewById(R.id.tagFilterContainer).setVisibility(View.GONE);
                    dateFilterButton.setVisibility(View.GONE);
                    clearFiltersButton.setVisibility(View.GONE);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initViews() {
        Log.d(TAG, "Initializing views");
        photosRecyclerView = findViewById(R.id.photosRecyclerView);
        tagsRecyclerView = findViewById(R.id.tagsRecyclerView);
        tagFilterLayout = findViewById(R.id.tagFilterLayout);
        dateFilterButton = findViewById(R.id.dateFilterButton);
        clearFiltersButton = findViewById(R.id.clearFiltersButton);
        emptyStateText = findViewById(R.id.emptyStateText);
        tagsView = findViewById(R.id.tagsView);
        photosView = findViewById(R.id.photosView);

        // ====================== MODIFICATION 2: INITIALIZE THE BREADCRUMB TEXTVIEW ======================
        // Assumes you have a TextView with the id 'breadcrumbText' in your activity_gallery.xml
        breadcrumbText = findViewById(R.id.breadcrumbText);
        // ==============================================================================================
    }

    private void setupRecyclerViews() {
        Log.d(TAG, "Setting up RecyclerViews");
        galleryAdapter = new GalleryAdapter();
        photosRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        photosRecyclerView.setAdapter(galleryAdapter);

        tagsAdapter = new TagsAdapter();
        tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tagsRecyclerView.setAdapter(tagsAdapter);

        tagsAdapter.setOnTagClickListener(tag -> {
            showPhotosForTag(tag);
        });
    }

    private void setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
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
                biometricPrompt.authenticate(promptInfo);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "No biometric features available on this device", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric features are currently unavailable", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No biometric credentials enrolled. Please enroll in Settings", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showVaultPhotos() {
        Toast.makeText(this, "Vault access granted", Toast.LENGTH_SHORT).show();
        showPhotosForTag(null);
    }

    private void showPhotosForTag(Tag tag) {
        currentTag = tag;

        findViewById(R.id.tagFilterContainer).setVisibility(View.VISIBLE);
        dateFilterButton.setVisibility(View.VISIBLE);
        clearFiltersButton.setVisibility(View.VISIBLE);

        tagsView.setVisibility(View.GONE);
        photosView.setVisibility(View.VISIBLE);

        if (tag != null) {
            // ====================== MODIFICATION 3: SET BREADCRUMB TEXT FOR A TAG ======================
            breadcrumbText.setText("Gallery > " + tag.name);
            // =========================================================================================

            photoViewModel.getPhotosByTag(tag.id).observe(this, photos -> {
                if (photos != null) {
                    Log.d(TAG, "Photos loaded for tag " + tag.name + ": " + photos.size());
                    galleryAdapter.setPhotos(photos);

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
            // ====================== MODIFICATION 4: SET BREADCRUMB TEXT FOR THE VAULT ======================
            breadcrumbText.setText("Gallery > Vault");
            // ============================================================================================

            galleryAdapter.setPhotos(new ArrayList<>());
            emptyStateText.setVisibility(View.VISIBLE);
            photosRecyclerView.setVisibility(View.GONE);
        }
    }

    private void showTagsView() {
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
                new Thread(() -> {
                    List<Integer> photoCounts = new ArrayList<>();
                    for (Tag tagItem : tags) {
                        int count = photoViewModel.getRepository().getPhotoCountForTag(tagItem.id);
                        photoCounts.add(count);
                    }

                    runOnUiThread(() -> {
                        tagsAdapter.setTags(tags, photoCounts);
                        showTagsView();
                    });
                }).start();
            }
        });

        photoViewModel.getIsLoading().observe(this, isLoading -> {
            // TODO: Show/hide loading indicator
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");

        if (tagsView.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "Resuming to tags view, reloading tags.");
            photoViewModel.loadTags();
        } else if (currentTag != null) {
            Log.d(TAG, "Resuming to photos view for tag: " + currentTag.name + ", reloading its photos.");
            showPhotosForTag(currentTag);
        }
    }

    private void setupFilters() {
        Log.d(TAG, "Setting up filters");
        dateFilterButton.setOnClickListener(v -> {
            showDateFilterDialog();
        });

        clearFiltersButton.setOnClickListener(v -> {
            if (currentTag != null) {
                showPhotosForTag(currentTag);
            } else {
                photoViewModel.loadPhotos();
            }
        });
    }

    private void showDateFilterDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        Toast.makeText(this, "Filter by: " + months[month] + " " + year, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Date filtering would filter photos by selected date range", Toast.LENGTH_LONG).show();
    }
}
