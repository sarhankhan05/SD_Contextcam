package com.example.sd_contextcam; // Make sure this package name is correct

import android.content.Intent;
import android.content.SharedPreferences; // <--- ADD THIS IMPORT
import android.database.Cursor; // <--- ADD THIS IMPORT
import android.net.Uri; // <--- ADD THIS IMPORT
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore; // <--- ADD THIS IMPORT
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.viewmodel.PhotoViewModel;

import java.io.File; // <--- ADD THIS IMPORT
import java.util.ArrayList;
import java.util.HashMap; // <--- ADD THIS IMPORT
import java.util.List;
import java.util.Map; // <--- ADD THIS IMPORT
import java.util.concurrent.ExecutorService; // <--- ADD THIS IMPORT
import java.util.concurrent.Executors; // <--- ADD THIS IMPORT

public class BatchSorterActivity extends AppCompatActivity {

    private ProgressBar progressBarScanning;
    private TextView textScanningLabel;
    private Group groupSorterUi;
    private RecyclerView batchRecyclerView;
    private Button btnSkipSorting;

    private BatchSorterAdapter adapter;
    private PhotoViewModel photoViewModel;

    // --- MODIFICATION: Add background thread executor ---
    private ExecutorService scannerExecutor = Executors.newSingleThreadExecutor();

    // --- MODIFICATION: Add constants for onboarding ---
    public static final String PREFS_NAME = "AppPrefs";
    public static final String ONBOARDING_COMPLETE = "onboarding_complete";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_sorter);

        // 1. Initialize Views
        initViews();

        // 2. Setup ViewModel (to pass to the adapter)
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);

        // 3. Setup RecyclerView
        setupRecyclerView();

        // 4. Set Click Listeners
        // This button will now call the MODIFIED goToCameraActivity()
        btnSkipSorting.setOnClickListener(v -> goToCameraActivity());

        // 5. Start the photo scan
        startPhotoScan();
    }

    private void initViews() {
        progressBarScanning = findViewById(R.id.progress_bar_scanning);
        textScanningLabel = findViewById(R.id.text_scanning_label);
        groupSorterUi = findViewById(R.id.group_sorter_ui);
        batchRecyclerView = findViewById(R.id.batch_recycler_view);
        btnSkipSorting = findViewById(R.id.btn_skip_sorting);
    }

    private void setupRecyclerView() {
        // We pass the ViewModel to the adapter so it can get tags and apply changes
        adapter = new BatchSorterAdapter(new ArrayList<>(), this, photoViewModel);
        batchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        batchRecyclerView.setAdapter(adapter);
    }

    private void startPhotoScan() {
        // Show scanning UI
        progressBarScanning.setVisibility(View.VISIBLE);
        textScanningLabel.setVisibility(View.VISIBLE);
        groupSorterUi.setVisibility(View.GONE);

        // --- MODIFICATION: Replaced fake data with real scanner on a background thread ---
        scannerExecutor.execute(() -> {

            // This Map will hold: <"Screenshots", List<.../path/to/img.jpg>>
            Map<String, List<String>> folderMap = new HashMap<>();

            // Define what columns we want to get
            String[] projection = new String[]{
                    MediaStore.Images.Media.DATA // This is the file path
            };

            Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = getContentResolver().query(imagesUri, projection, null, null, null);

            if (cursor != null) {
                int pathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                while (cursor.moveToNext()) {
                    String photoPath = cursor.getString(pathColumnIndex);

                    File file = new File(photoPath);
                    String folderPath = file.getParent();
                    if (folderPath == null) continue; // Skip if path is invalid
                    String folderName = new File(folderPath).getName();

                    if (!folderMap.containsKey(folderName)) {
                        folderMap.put(folderName, new ArrayList<>());
                    }
                    folderMap.get(folderName).add(photoPath);
                }
                cursor.close();
            }

            // Now, convert the Map into your List<BatchItem>
            // IMPORTANT: You MUST modify BatchItem.java to accept the List<String>
            List<BatchItem> foundBatches = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : folderMap.entrySet()) {
                String folder = entry.getKey();
                List<String> paths = entry.getValue();
                String description = "All photos found in your '" + folder + "' folder";

                // Pass the photo paths to the BatchItem
                foundBatches.add(new BatchItem(folder, description, paths.size(), paths));
            }

            // --- End of Scan Logic ---

            // Post the results back to the Main UI Thread
            new Handler(Looper.getMainLooper()).post(() -> {
                // Hide scanning UI
                progressBarScanning.setVisibility(View.GONE);
                textScanningLabel.setVisibility(View.GONE);

                if (foundBatches.isEmpty()) {
                    // If no photos were found, just go to the main app
                    Toast.makeText(this, "No existing photos found to organize.", Toast.LENGTH_SHORT).show();
                    goToCameraActivity(); // This will now go to MainActivity
                } else {
                    // Photos were found! Show the sorter UI
                    groupSorterUi.setVisibility(View.VISIBLE);
                    adapter.updateBatches(foundBatches);
                }
            });
        });
    }

    // --- MODIFICATION: This method now completes onboarding and goes to MainActivity ---
    private void goToCameraActivity() {
        // --- THIS IS THE CRITICAL STEP ---
        // Mark onboarding as complete!
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ONBOARDING_COMPLETE, true);
        editor.apply();

        // Now go to the main app hub
        Intent intent = new Intent(BatchSorterActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish this activity so the user can't go back to it
    }
}