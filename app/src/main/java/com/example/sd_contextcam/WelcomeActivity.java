package com.example.sd_contextcam;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build; // Import Build for version checks
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList; // Import ArrayList
import java.util.List;    // Import List

public class WelcomeActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String ONBOARDING_COMPLETE = "onboarding_complete";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding is already complete
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isOnboardingComplete = prefs.getBoolean(ONBOARDING_COMPLETE, false);

        if (isOnboardingComplete) {
            // Go directly to main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        Button grantPermissionButton = findViewById(R.id.grantPermissionButton);
        grantPermissionButton.setOnClickListener(v -> requestRequiredPermissions()); // Changed method name
    }

    // New method to handle multiple permissions and version-specific requests
    private void requestRequiredPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Handle Media/Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            // If the app needs to write media to shared storage (e.g., saving edited photos)
            // consider checking if the app is targeting API 33+ AND handling how to save files
            // WRITE_EXTERNAL_STORAGE is deprecated for general media access.
            // If you save photos taken by *this* app, you usually don't need WRITE_EXTERNAL_STORAGE for that.
        } else { // Android 12 (API 31) and below (including your minSdk 29)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            // For older APIs, WRITE_EXTERNAL_STORAGE might still be used if the app intends to
            // write files to *anywhere* on external storage. For merely reading/organizing
            // existing photos from the gallery, READ_EXTERNAL_STORAGE is usually sufficient
            // on these older versions. Given your targetSdk=36, it's less relevant.
            // if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //     permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            // }
        }

        // --- Handle Location Permissions (always request these AFTER storage if needed) ---
        // For ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // For ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        // NOTE: For background location (ACCESS_BACKGROUND_LOCATION), you'd need a separate request.

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            // All necessary permissions already granted
            // Now proceed with your location dialog as before,
            // or simply start onboarding if location isn't critical.
            requestLocationPermission(); // Or directly startOnboarding()
        }
    }


    // The rest of your code remains largely the same, but adjust onRequestPermissionsResult
    private void requestLocationPermission() {
        // Show location permission dialog
        LocationPermissionDialog dialog = new LocationPermissionDialog();
        dialog.setOnPermissionResultListener(granted -> {
            // No matter if granted or not, proceed as per your original logic
            startOnboarding();
        });
        dialog.show(getSupportFragmentManager(), "location_permission_dialog");
    }

    private void startOnboarding() {
        // Mark onboarding as complete
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ONBOARDING_COMPLETE, true);
        editor.apply();

        Intent intent = new Intent(this, OnboardingActivity.class);
        startActivity(intent);
        // Don't finish here so user can come back to main screen
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // All requested permissions (media and location) granted, proceed
                requestLocationPermission(); // This will show your custom dialog which then starts onboarding
            } else {
                Toast.makeText(this, "Required permissions are not fully granted. Some features may not work.",
                        Toast.LENGTH_LONG).show();
                // Optionally, you might want to guide the user to settings or explain why
                // permissions are needed, then give them an option to retry or proceed
                // with limited functionality. For now, proceeding to onboarding:
                startOnboarding(); // You might still want to proceed to onboarding even if denied
            }
        }
    }
}