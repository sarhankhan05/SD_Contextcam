package com.example.sd_contextcam;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
        grantPermissionButton.setOnClickListener(v -> requestStoragePermission());
    }
    
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed to location permission
            requestLocationPermission();
        }
    }
    
    private void requestLocationPermission() {
        // Show location permission dialog
        LocationPermissionDialog dialog = new LocationPermissionDialog();
        dialog.setOnPermissionResultListener(granted -> {
            if (granted) {
                // Start onboarding process
                startOnboarding();
            } else {
                // Proceed without location permission
                startOnboarding();
            }
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
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Storage permission granted, now request location permission
                requestLocationPermission();
            } else {
                Toast.makeText(this, "Storage permission is required to organize your photos", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}