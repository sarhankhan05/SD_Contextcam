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
    private RecyclerView tagsRecyclerView;
    private GalleryAdapter galleryAdapter;
    private TagsAdapter tagsAdapter;
    private LinearLayout tagFilterLayout;
    private Button dateFilterButton;
    private Button clearFiltersButton;
    private TextView emptyStateText;
    private View tagsView;
    private View photosView;
    private TextView breadcrumbText;
    private PhotoViewModel photoViewModel;

    // --- MODIFIED: State management variables to align with GalleryAdapter ---
    private Tag currentTag = null;
    private String currentViewMode = "TAGS"; // Possible values: "TAGS", "VAULT", "DATE"
    private int currentTagId = -1; // -1 represents no specific tag (e.g., for Vault or All)
    // --- END MODIFICATION ---

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
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Initially show tags view
        showTagsView();
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
        breadcrumbText = findViewById(R.id.breadcrumbText);
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
            // When a tag is clicked, show photos for that specific tag
            currentViewMode = "TAG"; // Set view mode to "TAG"
            showPhotosForTag(tag);
        });
    }

    private void showVaultPhotos() {
        Toast.makeText(this, "Vault access granted", Toast.LENGTH_SHORT).show();
        showPhotosForTag(null);
    }

    // --- MODIFIED: This method now handles displaying photos for a specific tag ---
    private void showPhotosForTag(Tag tag) {
        currentTag = tag;
        currentTagId = (tag != null) ? tag.id : -1;

        breadcrumbText.setText("Gallery > " + tag.name);
        findViewById(R.id.tagFilterContainer).setVisibility(View.VISIBLE);
        dateFilterButton.setVisibility(View.VISIBLE);
        clearFiltersButton.setVisibility(View.VISIBLE);
        tagsView.setVisibility(View.GONE);
        photosView.setVisibility(View.VISIBLE);

        // Tell ViewModel to load the photos for this specific tag
        // --- THIS IS THE FIX ---
        photoViewModel.loadPhotosByTagId(currentTagId);
    }

    private void showTagsView() {
        currentTag = null;
        currentTagId = -1;
        currentViewMode = "TAGS"; // The main view is the list of tags

        breadcrumbText.setText("Gallery");
        findViewById(R.id.tagFilterContainer).setVisibility(View.GONE);
        dateFilterButton.setVisibility(View.GONE);
        clearFiltersButton.setVisibility(View.GONE);
        tagsView.setVisibility(View.VISIBLE);
        photosView.setVisibility(View.GONE);

        // Refresh the tags list to get updated photo counts
        photoViewModel.loadTags();
    }

    private void setupViewModel() {
        Log.d(TAG, "Setting up ViewModel");
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
    }

    private void observeViewModel() {
        Log.d(TAG, "Observing ViewModel");

        // --- MODIFIED: The photo observer now passes the required context to the adapter ---
        photoViewModel.getPhotos().observe(this, photos -> {
            if (photos != null) {
                Log.d(TAG, "Photo list updated with " + photos.size() + " photos for mode: " + currentViewMode + ", tagId: " + currentTagId);
                // Pass the current context to the adapter so it can forward it to the detail activity
                galleryAdapter.setPhotos(photos, currentTagId, currentViewMode);

                if (photos.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    photosRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    photosRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
        // --- END MODIFICATION ---

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
                    });
                }).start();
            }
        });

        photoViewModel.getIsLoading().observe(this, isLoading -> {
            // TODO: Show/hide a proper loading indicator
        });
    }

    // Other methods like setupBiometricAuthentication() and setupFilters() remain the same
    // ...
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
