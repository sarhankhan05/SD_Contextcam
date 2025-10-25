package com.example.sd_contextcam; // <--- IMPORTANT: Change this to your actual package name

import android.content.Intent;
import android.content.SharedPreferences; // <--- ADD THIS IMPORT
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    // --- ADD THESE CONSTANTS ---
    // These MUST match the ones you use in PermissionsActivity/BatchSorterActivity
    public static final String PREFS_NAME = "AppPrefs";
    public static final String ONBOARDING_COMPLETE = "onboarding_complete";
    // --- END OF ADDITION ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        // Make sure you have an 'activity_splash_screen.xml' with your logo

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                // --- START OF MODIFIED LOGIC ---

                // Check SharedPreferences for the 'onboarding_complete' flag
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean isOnboardingComplete = prefs.getBoolean(ONBOARDING_COMPLETE, false);

                Intent intent;
                if (isOnboardingComplete) {
                    // Onboarding IS complete, go straight to the Main hub
                    intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                } else {
                    // Onboarding is NOT complete, start the welcome/permissions flow
                    intent = new Intent(SplashScreenActivity.this, WelcomeActivity.class);
                }

                SplashScreenActivity.this.startActivity(intent);
                SplashScreenActivity.this.finish(); // Close the splash activity

                // --- END OF MODIFIED LOGIC ---
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}