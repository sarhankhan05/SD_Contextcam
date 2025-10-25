package com.example.sd_contextcam; // Make sure this package name is correct

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout; // <-- ADD THIS IMPORT
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
// Use the ViewModel with callbacks
import com.example.sd_contextcam.viewmodel.PhotoViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else { // API 32 and below
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        REQUIRED_PERMISSIONS = perms.toArray(new String[0]);
    }

    // Session Persistence Constants
    public static final String PREFS_NAME = "AppContextPrefs";
    public static final String KEY_ACTIVE_TAG = "activeSessionTag";
    public static final String KEY_IS_VAULT = "isVaultSession";

    // UI Views
    private PreviewView viewFinder;
    // --- MODIFIED: Added container and end button ---
    private LinearLayout sessionIndicatorContainer;
    private TextView tagSessionIndicator; // This TextView is *inside* the container now
    private Button endSessionButton;      // The new red button
    // --- END MODIFICATION ---
    private ImageButton captureButton;
    private ImageButton tagSessionButton;
    private ImageButton galleryButton;

    private ConstraintLayout tagInputLayout;
    private AutoCompleteTextView tagAutoCompleteTextView;
    private Button confirmTagButton;
    private Button cancelTagButton;

    // Adapters & Data
    private ArrayAdapter<String> tagSuggestionsAdapter;
    private List<String> allTagNames = new ArrayList<>();

    // Logic Variables
    private String currentTagSession;
    private boolean isVaultSession = false;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private PhotoViewModel photoViewModel;
    private EncryptionUtil encryptionUtil;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        try {
            setContentView(R.layout.activity_camera);
            initViews();
            setupViewModel();
            photoViewModel.loadTags(); // Load tags early
            setupClickListeners();
            encryptionUtil = new EncryptionUtil(this);
            cameraExecutor = Executors.newSingleThreadExecutor();

            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Log.d(TAG, "Requesting permissions");
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // finish(); // Consider finishing if critical error
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        checkForInterruptedSession();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        if (isFinishing()) {
            Log.d(TAG, "Activity is finishing, clearing session.");
            clearSessionFromPrefs();
        } else {
            Log.d(TAG, "Activity is pausing, saving session.");
            saveSessionToPrefs();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdownNow(); // Use shutdownNow for quicker cleanup
        }
    }

    // --- MODIFIED: initViews finds new elements ---
    private void initViews() {
        Log.d(TAG, "Initializing views");
        viewFinder = findViewById(R.id.viewFinder);
        // Find the container and the views inside it
        sessionIndicatorContainer = findViewById(R.id.sessionIndicatorContainer);
        tagSessionIndicator = findViewById(R.id.tagSessionIndicator);
        endSessionButton = findViewById(R.id.endSessionButton);
        // Find other views
        captureButton = findViewById(R.id.captureButton);
        tagSessionButton = findViewById(R.id.tagSessionButton);
        galleryButton = findViewById(R.id.galleryButton);
        tagInputLayout = findViewById(R.id.tagInputLayout);
        tagAutoCompleteTextView = findViewById(R.id.tagAutoCompleteTextView);
        confirmTagButton = findViewById(R.id.confirmTagButton);
        cancelTagButton = findViewById(R.id.cancelTagButton);
    }
    // --- END MODIFICATION ---

    private void setupViewModel() {
        Log.d(TAG, "Setting up ViewModel");
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.getTags().observe(this, tags -> {
            Log.d(TAG, "Tags LiveData updated.");
            if (tags != null) {
                Log.d(TAG, "Received " + tags.size() + " tags from ViewModel.");
                allTagNames = tags.stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList());
            } else {
                Log.w(TAG, "Received null tag list from ViewModel.");
                allTagNames = new ArrayList<>();
            }
            if (tagSuggestionsAdapter != null) {
                updateTagSuggestions(tagAutoCompleteTextView.getText().toString());
            }
        });
    }

    private void applyTagToPhoto(long photoId, String tagName) {
        if (photoId <= 0 || tagName == null || tagName.isEmpty()) {
            Log.w(TAG, "applyTagToPhoto: Invalid photoId (" + photoId + ") or tagName (" + tagName + ")");
            return;
        }
        Log.d(TAG, "applyTagToPhoto: Applying tag '" + tagName + "' to photo ID: " + photoId);
        photoViewModel.getTagByName(tagName, tag -> {
            if (tag == null) {
                Log.d(TAG, "applyTagToPhoto: Tag '" + tagName + "' not found, creating...");
                Tag newTag = new Tag();
                newTag.setName(tagName);
                photoViewModel.addTag(newTag, newTagId -> {
                    if (newTagId > 0) {
                        Log.d(TAG, "applyTagToPhoto: New tag created with ID: " + newTagId + ". Linking photo ID: " + photoId);
                        photoViewModel.addTagToPhoto(photoId, (int) newTagId);
                    } else {
                        Log.e(TAG, "applyTagToPhoto: Failed to create new tag '" + tagName + "'");
                    }
                });
            } else {
                int existingTagId = tag.getId();
                Log.d(TAG, "applyTagToPhoto: Tag found ID: " + existingTagId + ". Linking photo ID: " + photoId);
                photoViewModel.addTagToPhoto(photoId, existingTagId);
            }
        });
    }

    // --- MODIFIED: setupClickListeners uses new end button ---
    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        captureButton.setOnClickListener(v -> takePhoto());
        tagSessionButton.setOnClickListener(v -> showTagInputDialog());
        galleryButton.setOnClickListener(v -> openGallery());
        // REMOVE listener from TextView: tagSessionIndicator.setOnClickListener(v -> endTagSession());
        // ADD listener to the new Button
        endSessionButton.setOnClickListener(v -> endTagSession());

        cancelTagButton.setOnClickListener(v -> tagInputLayout.setVisibility(View.GONE));
        confirmTagButton.setOnClickListener(v -> {
            String selectedTag = tagAutoCompleteTextView.getText().toString().trim();
            if (selectedTag.startsWith("+ Add new tag:")) {
                selectedTag = selectedTag.substring(selectedTag.indexOf("'") + 1, selectedTag.lastIndexOf("'"));
            }
            if (selectedTag.isEmpty()) {
                Toast.makeText(this, "Please enter or select a tag", Toast.LENGTH_SHORT).show();
                return;
            }
            if ("Vault".equalsIgnoreCase(selectedTag)) {
                startVaultSession();
            } else {
                startTagSession(selectedTag);
            }
            tagInputLayout.setVisibility(View.GONE);
        });

        tagAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTagSuggestions(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        tagAutoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "Item selected from dropdown: " + selection);
            if (selection != null) {
                if (selection.startsWith("+ Add new tag:")) {
                    String newTag = selection.substring(selection.indexOf("'") + 1, selection.lastIndexOf("'"));
                    tagAutoCompleteTextView.setText(newTag);
                    tagAutoCompleteTextView.setSelection(newTag.length());
                }
            }
        });
    }
    // --- END MODIFICATION ---

    private void startCamera() {
        Log.d(TAG, "Starting camera");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (cameraProvider == null) {
                    Log.e(TAG, "Camera provider is null");
                    runOnUiThread(() -> Toast.makeText(this, "Failed to get camera provider.", Toast.LENGTH_SHORT).show());
                    return;
                }
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera provider", e);
                runOnUiThread(() -> Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error starting camera", e);
                runOnUiThread(() -> Toast.makeText(this, "Unexpected error starting camera.", Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "Binding preview");
        try {
            cameraProvider.unbindAll();
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
            imageCapture = new ImageCapture.Builder().build();
            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            Log.d(TAG, "Camera preview bound successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera preview", e);
            runOnUiThread(() -> Toast.makeText(this, "Error binding camera: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showTagInputDialog() {
        Log.d(TAG, "Showing Tag Input Dialog. Current tags loaded: " + allTagNames.size());
        tagAutoCompleteTextView.setText("");
        List<String> currentSuggestions = new ArrayList<>(allTagNames);
        currentSuggestions.add("Vault");

        // --- TODO: Ensure R.layout.dropdown_item exists ---
        tagSuggestionsAdapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                currentSuggestions
        );
        tagAutoCompleteTextView.setAdapter(tagSuggestionsAdapter);

        tagInputLayout.setVisibility(View.VISIBLE);
        tagAutoCompleteTextView.requestFocus();
    }

    private void updateTagSuggestions(String query) {
        List<String> baseSuggestions = new ArrayList<>(allTagNames);
        baseSuggestions.add("Vault");
        boolean exactMatch = baseSuggestions.stream()
                .anyMatch(s -> s.equalsIgnoreCase(query.trim()));
        List<String> finalSuggestionsForAdapter = new ArrayList<>();
        if (!query.trim().isEmpty() && !exactMatch && !query.trim().startsWith("+")) {
            finalSuggestionsForAdapter.add("+ Add new tag: '" + query.trim() + "'");
        }
        baseSuggestions.stream()
                .filter(s -> s.toLowerCase().contains(query.trim().toLowerCase()))
                .forEach(finalSuggestionsForAdapter::add);

        if (tagSuggestionsAdapter != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                tagSuggestionsAdapter.clear();
                tagSuggestionsAdapter.addAll(finalSuggestionsForAdapter);
                tagSuggestionsAdapter.notifyDataSetChanged();
            });
        }
    }

    // --- MODIFIED: Session start/end updates container visibility and text ---
    private void startTagSession(String tag) {
        Log.d(TAG, "Starting tag session: " + tag);
        currentTagSession = tag;
        isVaultSession = false;
        // Update text format
        tagSessionIndicator.setText("Session Active - " + tag);
        // Show the container
        sessionIndicatorContainer.setVisibility(View.VISIBLE);
        // TODO: Update tag icon if needed
        // tagSessionButton.setImageResource(R.drawable.ic_tag_active);
        saveSessionToPrefs();
    }

    private void startVaultSession() {
        Log.d(TAG, "Starting vault session");
        currentTagSession = "Vault";
        isVaultSession = true;
        // Update text format
        tagSessionIndicator.setText("Vault Session Active");
        // Show the container
        sessionIndicatorContainer.setVisibility(View.VISIBLE);
        // TODO: Update tag icon if needed
        // tagSessionButton.setImageResource(R.drawable.ic_lock);
        saveSessionToPrefs();
    }

    private void endTagSession() {
        Log.d(TAG, "Ending tag session");
        currentTagSession = null;
        isVaultSession = false;
        // Hide the container
        sessionIndicatorContainer.setVisibility(View.GONE);
        // TODO: Update tag icon if needed
        // tagSessionButton.setImageResource(R.drawable.ic_tag_inactive);
        clearSessionFromPrefs();
        Toast.makeText(this, "Session ended", Toast.LENGTH_SHORT).show();
    }
    // --- END MODIFICATION ---

    private void takePhoto() {
        Log.d(TAG, "takePhoto: Initiating...");
        if (imageCapture == null) {
            Log.e(TAG, "takePhoto: ImageCapture is null.");
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "CTX_" + timeStamp + ".jpg";
        File outputDir = getFilesDir();
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "takePhoto: Failed to create directory: " + outputDir.getAbsolutePath());
            Toast.makeText(this, "Error creating photo directory", Toast.LENGTH_SHORT).show();
            return;
        }
        File photoFile = new File(outputDir, fileName);
        Log.d(TAG, "takePhoto: Saving to: " + photoFile.getAbsolutePath());

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        String savedPath = photoFile.getAbsolutePath();
                        long currentTimestamp = System.currentTimeMillis();
                        Log.d(TAG, "onImageSaved: Success. Path: " + savedPath);

                        if (isVaultSession) {
                            Log.d(TAG, "onImageSaved: Processing vault photo...");
                            cameraExecutor.execute(() -> {
                                String encryptedFileName = "VAULT_" + fileName;
                                String encryptedPath = encryptionUtil.encryptPhoto(photoFile, encryptedFileName);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (encryptedPath != null) {
                                        Log.d(TAG, "onImageSaved: Encryption successful, path: " + encryptedPath);
                                        Photo photo = new Photo();
                                        photo.setFilePath(encryptedPath);
                                        photo.setTimestamp(currentTimestamp);
                                        photo.setEncrypted(true);
                                        photoViewModel.addPhoto(photo, newPhotoId -> {
                                            if (newPhotoId > 0) Log.d(TAG, "Vault photo added to DB: " + newPhotoId);
                                            else Log.e(TAG, "Failed to add vault photo to DB");
                                        });
                                        Toast.makeText(CameraActivity.this, "Photo secured in vault", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.e(TAG, "onImageSaved: Encryption failed for: " + savedPath);
                                        Toast.makeText(CameraActivity.this, "Error securing photo", Toast.LENGTH_SHORT).show();
                                    }
                                    boolean deleted = photoFile.delete();
                                    Log.d(TAG, "onImageSaved: Original file deleted: " + deleted);
                                });
                            });
                        } else {
                            Log.d(TAG, "onImageSaved: Processing standard photo...");
                            Photo photo = new Photo();
                            photo.setFilePath(savedPath);
                            photo.setTimestamp(currentTimestamp);
                            photo.setEncrypted(false);
                            photoViewModel.addPhoto(photo, newPhotoId -> { // newPhotoId is long
                                if (newPhotoId > 0) {
                                    Log.d(TAG, "Standard photo added to DB: " + newPhotoId);
                                    if (currentTagSession != null && !currentTagSession.isEmpty()) {
                                        applyTagToPhoto(newPhotoId, currentTagSession);
                                    }
                                } else {
                                    Log.e(TAG, "Failed to add standard photo to DB");
                                }
                            });
                            String message = "Photo captured" + (currentTagSession != null ? " with tag: " + currentTagSession : "");
                            Toast.makeText(CameraActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "takePhoto onError: " + exc.getMessage(), exc);
                        Toast.makeText(CameraActivity.this, "Photo capture failed: " + exc.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permission not granted: " + permission);
                return false;
            }
        }
        Log.d(TAG, "All required permissions granted.");
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) { // Re-check all permissions
                Log.d(TAG, "Permissions granted after request.");
                startCamera();
            } else {
                Log.e(TAG, "Not all required permissions were granted after request.");
                Toast.makeText(this, "Essential permissions not granted. Camera cannot function.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // --- Session Helper Methods ---
    private void checkForInterruptedSession() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedTag = prefs.getString(KEY_ACTIVE_TAG, null);
        boolean isVault = prefs.getBoolean(KEY_IS_VAULT, false);

        if (savedTag != null) {
            Log.d(TAG, "Interrupted session found: " + savedTag + ", isVault: " + isVault);
            String sessionName = isVault ? "Vault" : savedTag;
            // --- TODO: Ensure R.style.AlertDialogTheme exists ---
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle("Session in Progress")
                    .setMessage("You didn't end your last session: '" + sessionName + "'. Continue?")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        Log.d(TAG, "Continuing interrupted session.");
                        if (isVault) startVaultSession();
                        else startTagSession(savedTag);
                    })
                    .setNegativeButton("End", (dialog, which) -> {
                        Log.d(TAG, "Ending interrupted session via dialog.");
                        clearSessionFromPrefs();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            Log.d(TAG, "No interrupted session found.");
        }
    }

    private void saveSessionToPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (currentTagSession != null) {
            editor.putString(KEY_ACTIVE_TAG, currentTagSession);
            editor.putBoolean(KEY_IS_VAULT, isVaultSession);
            Log.d(TAG, "Saving session state: Tag=" + currentTagSession + ", isVault=" + isVaultSession);
        } else {
            editor.remove(KEY_ACTIVE_TAG);
            editor.remove(KEY_IS_VAULT);
            Log.d(TAG, "Saving session state: No active session");
        }
        editor.apply();
    }

    private void clearSessionFromPrefs() {
        Log.d(TAG, "Clearing session state");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_ACTIVE_TAG)
                .remove(KEY_IS_VAULT)
                .apply();
    }
}