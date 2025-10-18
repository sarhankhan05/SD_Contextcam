package com.example.sd_contextcam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.data.Tag;
import com.example.sd_contextcam.security.EncryptionUtil;
import com.example.sd_contextcam.viewmodel.PhotoViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = getRequiredPermissions();

    private PreviewView viewFinder;
    private TextView tagSessionIndicator;
    private ImageButton captureButton;
    private ImageButton tagSessionButton;
    private ImageButton galleryButton;

    private String currentTagSession = null;
    private boolean isVaultSession = false;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private PhotoViewModel photoViewModel;
    private EncryptionUtil encryptionUtil;
    
    // Get required permissions based on Android version
    private static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, we don't need WRITE_EXTERNAL_STORAGE for app-specific directories
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
            Log.d(TAG, "Setting content view");
            setContentView(R.layout.activity_camera);
            Log.d(TAG, "Content view set");
            
            Log.d(TAG, "Initializing views");
            initViews();
            Log.d(TAG, "Views initialized");
            
            Log.d(TAG, "Setting up click listeners");
            setupClickListeners();
            Log.d(TAG, "Click listeners set up");
            
            Log.d(TAG, "Setting up ViewModel");
            setupViewModel();
            Log.d(TAG, "ViewModel set up");
            
            // Initialize encryption util
            encryptionUtil = new EncryptionUtil(this);
            
            // Request camera permissions
            Log.d(TAG, "Checking permissions");
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted, starting camera");
                startCamera();
            } else {
                Log.d(TAG, "Requesting permissions");
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
        
        // Set initial button icons
        tagSessionButton.setImageResource(R.drawable.ic_tag_inactive);
        galleryButton.setImageResource(R.drawable.ic_gallery);
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        captureButton.setOnClickListener(v -> takePhoto());
        tagSessionButton.setOnClickListener(v -> showTagSessionDialog());
        galleryButton.setOnClickListener(v -> openGallery());
    }

    private void setupViewModel() {
        Log.d(TAG, "Setting up ViewModel");
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        
        // Observe the last inserted photo ID to apply tags if needed
        photoViewModel.getLastInsertedPhotoId().observe(this, photoId -> {
            if (photoId != null && photoId > 0 && currentTagSession != null && !isVaultSession) {
                // Apply tag to the newly inserted photo
                applyTagToPhoto(photoId, currentTagSession);
            }
        });
    }
    
    private void applyTagToPhoto(int photoId, String tagName) {
        // First, try to get the tag by name
        photoViewModel.getTagByName(tagName, new PhotoViewModel.TagCallback() {
            @Override
            public void onTagReceived(Tag tag) {
                if (tag == null) {
                    // Tag doesn't exist, create it
                    Tag newTag = new Tag();
                    newTag.setName(tagName);
                    photoViewModel.addTag(newTag);
                    
                    // After creating the tag, we need to get its ID
                    // We'll observe the tags and apply the tag when it's available
                    photoViewModel.getTags().observe(CameraActivity.this, newTags -> {
                        for (Tag t : newTags) {
                            if (tagName.equals(t.getName())) {
                                photoViewModel.addTagToPhoto(photoId, t.getId());
                                // Remove observer to prevent multiple triggers
                                photoViewModel.getTags().removeObservers(CameraActivity.this);
                                break;
                            }
                        }
                    });
                } else {
                    // Tag exists, apply it to the photo
                    photoViewModel.addTagToPhoto(photoId, tag.getId());
                }
            }
        });
    }
    
    private void startCamera() {
        Log.d(TAG, "Starting camera");
        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            Log.d(TAG, "Camera provider future obtained");

            cameraProviderFuture.addListener(() -> {
                try {
                    Log.d(TAG, "Camera provider future ready");
                    cameraProvider = cameraProviderFuture.get();
                    Log.d(TAG, "Camera provider obtained, binding preview");
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // Handle any errors
                    Log.e(TAG, "Error starting camera", e);
                    runOnUiThread(() -> Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera process", e);
            Toast.makeText(this, "Error starting camera process: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "Binding preview");
        try {
            Preview preview = new Preview.Builder().build();
            Log.d(TAG, "Preview built");
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
            Log.d(TAG, "Surface provider set");

            imageCapture = new ImageCapture.Builder().build();
            Log.d(TAG, "ImageCapture built");

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            Log.d(TAG, "Camera selector created");

            Log.d(TAG, "Binding use cases to lifecycle");
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            Log.d(TAG, "Camera bound successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera", e);
            runOnUiThread(() -> Toast.makeText(this, "Error binding camera: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showTagSessionDialog() {
        // Create a list of recent tags and special tags
        String[] tagOptions = {"DS Notes - Graphs", "College", "Work", "Family", "Vacation", "Vault"};
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Start Tag Session")
                .setItems(tagOptions, (dialog, which) -> {
                    String selectedTag = tagOptions[which];
                    if ("Vault".equals(selectedTag)) {
                        startVaultSession();
                    } else {
                        startTagSession(selectedTag);
                    }
                })
                .show();
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
            Log.e(TAG, "ImageCapture is null");
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a timestamped file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "JPEG_" + timeStamp + ".jpg";
        
        // Create output file in app-specific directory
        File outputDir = getExternalFilesDir(null);
        if (outputDir == null) {
            Log.e(TAG, "Error accessing storage");
            Toast.makeText(this, "Error accessing storage", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File photoFile = new File(outputDir, fileName);
        Log.d(TAG, "Photo file path: " + photoFile.getAbsolutePath());

        // Setup image capture listener
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Log.d(TAG, "Image saved successfully");
                        
                        // If this is a vault session, encrypt the photo and store it in the vault
                        if (isVaultSession) {
                            // Encrypt the photo and store it in the vault
                            String encryptedPath = encryptionUtil.encryptPhoto(photoFile, fileName);
                            if (encryptedPath != null) {
                                // Save encrypted photo info to database
                                Photo photo = new Photo();
                                photo.setFilePath(encryptedPath); // Store the encrypted file path
                                photo.setTimestamp(System.currentTimeMillis());
                                photoViewModel.addPhoto(photo);
                                
                                runOnUiThread(() -> {
                                    Toast.makeText(CameraActivity.this, "Photo captured and secured in vault", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(CameraActivity.this, "Error securing photo in vault", Toast.LENGTH_SHORT).show();
                                });
                            }
                            
                            // Delete the original unencrypted file
                            photoFile.delete();
                        } else {
                            // Save photo to database with proper null values for latitude and longitude
                            Photo photo = new Photo();
                            photo.setFilePath(photoFile.getAbsolutePath());
                            photo.setTimestamp(System.currentTimeMillis());
                            // Latitude and longitude will be null by default, which is now allowed
                            photoViewModel.addPhoto(photo);
                            
                            // If we have an active tag session, apply the tag to the photo
                            if (currentTagSession != null) {
                                // We'll apply the tag after the photo is inserted
                                // by observing the last inserted photo ID
                            }
                            
                            runOnUiThread(() -> {
                                String message = "Photo captured" + (currentTagSession != null ? " with tag: " + currentTagSession : "");
                                Toast.makeText(CameraActivity.this, message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed", exc);
                        runOnUiThread(() -> {
                            Toast.makeText(CameraActivity.this, "Photo capture failed: " + exc.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void openGallery() {
        Log.d(TAG, "Opening gallery");
        Intent intent = new Intent(CameraActivity.this, GalleryActivity.class);
        startActivity(intent);
    }

    private boolean allPermissionsGranted() {
        Log.d(TAG, "Checking permissions");
        for (String permission : REQUIRED_PERMISSIONS) {
            Log.d(TAG, "Checking permission: " + permission);
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not granted: " + permission);
                return false;
            }
        }
        Log.d(TAG, "All permissions granted");
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult called, requestCode: " + requestCode);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted in result, starting camera");
                startCamera();
            } else {
                Log.e(TAG, "Permissions not granted by the user");
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}