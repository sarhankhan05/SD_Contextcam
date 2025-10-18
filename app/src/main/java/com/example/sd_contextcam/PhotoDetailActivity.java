package com.example.sd_contextcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sd_contextcam.security.EncryptionUtil;

import java.io.File;

public class PhotoDetailActivity extends AppCompatActivity {
    private static final String TAG = "PhotoDetailActivity";
    
    public static final String EXTRA_PHOTO_PATH = "photo_path";
    public static final String EXTRA_IS_VAULT_PHOTO = "is_vault_photo";
    
    private ImageView photoImageView;
    private ProgressBar loadingProgressBar;
    private EncryptionUtil encryptionUtil;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        
        initViews();
        setupEncryptionUtil();
        loadPhoto();
    }
    
    private void initViews() {
        photoImageView = findViewById(R.id.photoImageView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        
        // Set up click listener to toggle UI visibility
        photoImageView.setOnClickListener(v -> toggleUIVisibility());
    }
    
    private void setupEncryptionUtil() {
        encryptionUtil = new EncryptionUtil(this);
    }
    
    private void loadPhoto() {
        String photoPath = getIntent().getStringExtra(EXTRA_PHOTO_PATH);
        boolean isVaultPhoto = getIntent().getBooleanExtra(EXTRA_IS_VAULT_PHOTO, false);
        
        if (photoPath == null || photoPath.isEmpty()) {
            Toast.makeText(this, "Error: Photo path not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        loadingProgressBar.setVisibility(View.VISIBLE);
        
        // Load photo in background thread
        new Thread(() -> {
            try {
                Bitmap bitmap = null;
                
                if (isVaultPhoto) {
                    // For vault photos, we need to decrypt them first
                    File encryptedFile = new File(photoPath);
                    if (encryptedFile.exists()) {
                        // Create a temporary file for the decrypted photo
                        File tempFile = new File(getCacheDir(), "temp_decrypted_photo.jpg");
                        
                        // Decrypt the photo
                        boolean success = encryptionUtil.decryptPhoto(photoPath, tempFile.getAbsolutePath());
                        if (success && tempFile.exists()) {
                            // Load the decrypted photo
                            bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
                            
                            // Delete the temporary file
                            tempFile.delete();
                        }
                    }
                } else {
                    // For regular photos, load directly
                    File photoFile = new File(photoPath);
                    if (photoFile.exists()) {
                        bitmap = BitmapFactory.decodeFile(photoPath);
                    }
                }
                
                final Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    
                    if (finalBitmap != null) {
                        photoImageView.setImageBitmap(finalBitmap);
                    } else {
                        Toast.makeText(this, "Error loading photo", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading photo", e);
                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
    
    private void toggleUIVisibility() {
        // Hide status bar and action bar for full screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Toggle system UI visibility
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        if ((uiOptions & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.SYSTEM_UI_FLAG_FULLSCREEN) {
            // Currently in full screen, show UI
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            // Not in full screen, hide UI
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}