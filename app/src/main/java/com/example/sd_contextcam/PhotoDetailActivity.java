// In PhotoDetailActivity.java
package com.example.sd_contextcam;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.io.Serializable;
import java.util.List;

public class PhotoDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_LIST = "com.example.sd_contextcam.EXTRA_PHOTO_LIST";
    public static final String EXTRA_CURRENT_POSITION = "com.example.sd_contextcam.EXTRA_CURRENT_POSITION";
    public static final String EXTRA_IS_VAULT_PHOTO = "com.example.sd_contextcam.EXTRA_IS_VAULT_PHOTO";

    public static final String EXTRA_CLICKED_PHOTO_ID = "com.example.sd_contextcam.EXTRA_CLICKED_PHOTO_ID";
    public static final String EXTRA_VIEW_MODE = "com.example.sd_contextcam.EXTRA_VIEW_MODE";
    public static final String EXTRA_TAG_ID = "com.example.sd_contextcam.EXTRA_TAG_ID";


    private ViewPager2 viewPager;
    private List<String> photoPaths;
    private boolean isVaultPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        viewPager = findViewById(R.id.photoViewPager);
        Button saveToGalleryButton = findViewById(R.id.saveButton);

        // Get the data from the intent
        photoPaths = (List<String>) getIntent().getSerializableExtra(EXTRA_PHOTO_LIST);
        int currentPosition = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);
        isVaultPhoto = getIntent().getBooleanExtra(EXTRA_IS_VAULT_PHOTO, false);

        if (photoPaths == null || photoPaths.isEmpty()) {
            Toast.makeText(this, "Error: Photo list not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up the adapter for the ViewPager
        PhotoPagerAdapter adapter = new PhotoPagerAdapter(this, photoPaths, isVaultPhoto);
        viewPager.setAdapter(adapter);

        // Go to the photo that was actually clicked
        viewPager.setCurrentItem(currentPosition, false);

        // Set up the "Save to Gallery" button listener
        saveToGalleryButton.setOnClickListener(v -> {
            saveCurrentPhotoToGallery();
        });
    }

    private void saveCurrentPhotoToGallery() {
        if (photoPaths == null || photoPaths.isEmpty()) return;

        // Get the path of the currently visible photo
        int currentItem = viewPager.getCurrentItem();
        String currentPhotoPath = photoPaths.get(currentItem);

        // You need to implement this saving logic.
        // It involves decrypting the photo and saving it to the public MediaStore.
        boolean success = MediaSaver.savePhotoToGallery(this, currentPhotoPath, isVaultPhoto);

        if (success) {
            Toast.makeText(this, "Photo saved to gallery!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show();
        }
    }
}
