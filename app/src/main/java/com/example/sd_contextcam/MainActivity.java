package com.example.sd_contextcam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.sd_contextcam.viewmodel.PhotoViewModel;

public class MainActivity extends AppCompatActivity {
    private PhotoViewModel photoViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup ViewModel
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        
        // Find the buttons and set click listeners
        Button openCameraButton = findViewById(R.id.openCameraButton);
        openCameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });
        
        Button openGalleryButton = findViewById(R.id.openGalleryButton);
        openGalleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            startActivity(intent);
        });
        
        Button openVaultButton = findViewById(R.id.openVaultButton);
        openVaultButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VaultActivity.class);
            startActivity(intent);
        });
    }
}