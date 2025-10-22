package com.example.sd_contextcam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.data.Tag;
import com.example.sd_contextcam.security.EncryptionUtil;
import com.example.sd_contextcam.viewmodel.PhotoViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = getRequiredPermissions();

    private PreviewView viewFinder;
    private TextView tagSessionIndicator;
    private ImageButton captureButton;
    private ImageButton tagSessionButton;
    private ImageButton galleryButton;

    // New UI components for tag input
    private ConstraintLayout tagInputLayout;
    private AutoCompleteTextView tagAutoCompleteTextView;
    private Button confirmTagButton;
    private Button cancelTagButton;
    private ArrayAdapter<String> tagSuggestionsAdapter;
    private List<String> allTagNames = new ArrayList<>();

    private String currentTagSession = null;
    private boolean isVaultSession = false;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private PhotoViewModel photoViewModel;
    private EncryptionUtil encryptionUtil;

    private static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{Manifest.permission.CAMERA};
        } else {
            return new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        try {
            setContentView(R.layout.activity_camera);
            initViews();
            setupViewModel(); // Setup ViewModel before listeners that use it
            setupClickListeners();
            encryptionUtil = new EncryptionUtil(this);

            if (allPermissionsGranted()) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        Log.d(TAG, "Initializing views");
        viewFinder = findViewById(R.id.viewFinder);
        tagSessionIndicator = findViewById(R.id.tagSessionIndicator);
        captureButton = findViewById(R.id.captureButton);
        tagSessionButton = findViewById(R.id.tagSessionButton);
        galleryButton = findViewById(R.id.galleryButton);

        // Initialize new tag input views
        tagInputLayout = findViewById(R.id.tagInputLayout);
        tagAutoCompleteTextView = findViewById(R.id.tagAutoCompleteTextView);
        confirmTagButton = findViewById(R.id.confirmTagButton);
        cancelTagButton = findViewById(R.id.cancelTagButton);

        tagSessionButton.setImageResource(R.drawable.ic_tag_inactive);
        galleryButton.setImageResource(R.drawable.ic_gallery);
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        captureButton.setOnClickListener(v -> takePhoto());
        tagSessionButton.setOnClickListener(v -> showTagInputDialog()); // Changed from showTagSessionDialog
        galleryButton.setOnClickListener(v -> openGallery());

        // Listeners for the new tag input layout
        cancelTagButton.setOnClickListener(v -> tagInputLayout.setVisibility(View.GONE));
        confirmTagButton.setOnClickListener(v -> {
            String selectedTag = tagAutoCompleteTextView.getText().toString().trim();
            if ("Vault".equalsIgnoreCase(selectedTag)) {
                startVaultSession();
            } else if (!selectedTag.isEmpty()) {
                startTagSession(selectedTag);
            }
            tagInputLayout.setVisibility(View.GONE);
        });

        // Add a text watcher to update suggestions dynamically
        tagAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTagSuggestions(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupViewModel() {
        Log.d(TAG, "Setting up ViewModel");
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);

        photoViewModel.getLastInsertedPhotoId().observe(this, photoId -> {
            if (photoId != null && photoId > 0 && currentTagSession != null && !isVaultSession) {
                applyTagToPhoto(photoId, currentTagSession);
            }
        });

        // Observe all tags to populate suggestions
        photoViewModel.getTags().observe(this, tags -> {
            if (tags != null) {
                allTagNames = tags.stream().map(Tag::getName).collect(Collectors.toList());
            }
        });
    }

    private void applyTagToPhoto(int photoId, String tagName) {
        photoViewModel.getTagByName(tagName, tag -> {
            if (tag == null) {
                Tag newTag = new Tag();
                newTag.setName(tagName);
                photoViewModel.addTag(newTag);
                // The logic to link photo and tag will be triggered again
                // once the tag is created and getTagByName is called next time.
                // A more robust way is to get the new tag's ID and then link it.
                // For simplicity, we'll re-trigger photo saving or have a dedicated listener.
                photoViewModel.getLastInsertedPhotoId().observe(this, newPhotoId -> {
                    if (newPhotoId != null && newPhotoId == photoId) {
                        photoViewModel.getTagByName(tagName, newCreatedTag -> {
                            if(newCreatedTag != null) {
                                photoViewModel.addTagToPhoto(photoId, newCreatedTag.getId());
                            }
                        });
                    }
                });


            } else {
                photoViewModel.addTagToPhoto(photoId, tag.getId());
            }
        });
    }

    private void startCamera() {
        Log.d(TAG, "Starting camera");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                runOnUiThread(() -> Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "Binding preview");
        try {
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
            imageCapture = new ImageCapture.Builder().build();
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera", e);
            runOnUiThread(() -> Toast.makeText(this, "Error binding camera: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showTagInputDialog() {
        tagAutoCompleteTextView.setText(""); // Clear previous input
        updateTagSuggestions(""); // Populate initial list
        tagInputLayout.setVisibility(View.VISIBLE);
    }

    private void updateTagSuggestions(String query) {
        List<String> suggestions = new ArrayList<>(allTagNames);
        suggestions.add("Vault"); // Always include Vault option

        // Filter suggestions based on query, not strictly needed as AutoCompleteTextView does this
        List<String> filteredSuggestions = suggestions.stream()
                .filter(s -> s.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        // Check if the exact query exists (case-insensitive)
        boolean exactMatch = filteredSuggestions.stream()
                .anyMatch(s -> s.equalsIgnoreCase(query));

        // If query is not empty and no exact match, add the "Add new..." option
        if (!query.isEmpty() && !exactMatch) {
            filteredSuggestions.add(0, "+ Add new tag: '" + query + "'");
        }

        tagSuggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredSuggestions);
        tagAutoCompleteTextView.setAdapter(tagSuggestionsAdapter);
        tagSuggestionsAdapter.notifyDataSetChanged();

        // Handle item click to replace "+ Add new..." with the actual tag
        tagAutoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            if (selection.startsWith("+ Add new tag:")) {
                String newTag = selection.substring(selection.indexOf("'") + 1, selection.lastIndexOf("'"));
                tagAutoCompleteTextView.setText(newTag);
                tagAutoCompleteTextView.setSelection(newTag.length()); // Move cursor to end
            }
        });
    }

    private void startTagSession(String tag) {
        currentTagSession = tag;
        isVaultSession = false;
        tagSessionIndicator.setText("Tagging as: " + tag);
        tagSessionIndicator.setVisibility(View.VISIBLE);
        tagSessionIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.tag_session_background));
        tagSessionButton.setImageResource(R.drawable.ic_tag_active);
    }

    private void startVaultSession() {
        currentTagSession = "Vault";
        isVaultSession = true;
        tagSessionIndicator.setText("Saving to: Vault (Encrypted)");
        tagSessionIndicator.setVisibility(View.VISIBLE);
        tagSessionIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.vault_session_background));
        tagSessionButton.setImageResource(R.drawable.ic_lock);
    }

    private void endTagSession() {
        currentTagSession = null;
        isVaultSession = false;
        tagSessionIndicator.setVisibility(View.GONE);
        tagSessionButton.setImageResource(R.drawable.ic_tag_inactive);
    }

    private void takePhoto() {
        Log.d(TAG, "Taking photo");
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "JPEG_" + timeStamp + ".jpg";

        File outputDir = getExternalFilesDir(null);
        if (outputDir == null) {
            Toast.makeText(this, "Error accessing storage", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = new File(outputDir, fileName);
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Log.d(TAG, "Image saved successfully");

                        if (isVaultSession) {
                            String encryptedPath = encryptionUtil.encryptPhoto(photoFile, fileName);
                            if (encryptedPath != null) {
                                Photo photo = new Photo();
                                photo.setFilePath(encryptedPath);
                                photo.setTimestamp(System.currentTimeMillis());
                                photoViewModel.addPhoto(photo);
                                runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Photo captured and secured in vault", Toast.LENGTH_SHORT).show());
                            } else {
                                runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Error securing photo in vault", Toast.LENGTH_SHORT).show());
                            }
                            photoFile.delete();
                        } else {
                            Photo photo = new Photo();
                            photo.setFilePath(photoFile.getAbsolutePath());
                            photo.setTimestamp(System.currentTimeMillis());
                            photoViewModel.addPhoto(photo);

                            runOnUiThread(() -> {
                                String message = "Photo captured" + (currentTagSession != null ? " with tag: " + currentTagSession : "");
                                Toast.makeText(CameraActivity.this, message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed", exc);
                        runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Photo capture failed: " + exc.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(CameraActivity.this, GalleryActivity.class);
        startActivity(intent);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
