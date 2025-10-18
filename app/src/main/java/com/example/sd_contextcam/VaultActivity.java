package com.example.sd_contextcam;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class VaultActivity extends AppCompatActivity {
    private TextView authPromptText;
    private Button authenticateButton;
    private RecyclerView vaultRecyclerView;
    private TextView emptyVaultText;
    
    private VaultAdapter vaultAdapter;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    
    // For PIN authentication
    private static final String PREFS_NAME = "VaultPrefs";
    private static final String PIN_KEY = "vault_pin";
    
    private AlertDialog authMethodDialog;
    private AlertDialog pinDialog;
    private AlertDialog createPinDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);
        
        initViews();
        setupRecyclerView();
        setupBiometricAuthentication();
        setupClickListeners();
        
        // Check if user has set a PIN
        if (hasVaultPin()) {
            // Show options for authentication method
            showAuthMethodDialog();
        } else {
            // First time setup - create PIN
            showCreatePinDialog();
        }
    }
    
    private void initViews() {
        authPromptText = findViewById(R.id.authPromptText);
        authenticateButton = findViewById(R.id.authenticateButton);
        vaultRecyclerView = findViewById(R.id.vaultRecyclerView);
        emptyVaultText = findViewById(R.id.emptyVaultText);
    }
    
    private void setupRecyclerView() {
        vaultAdapter = new VaultAdapter();
        vaultRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        vaultRecyclerView.setAdapter(vaultAdapter);
        
        // Remove the old click listener since we're handling clicks in the adapter
        // vaultAdapter.setOnVaultPhotoClickListener(encryptedPhotoPath -> {
        //     // TODO: Decrypt and display the photo
        //     Toast.makeText(this, "Photo clicked: " + encryptedPhotoPath, Toast.LENGTH_SHORT).show();
        // });
    }
    
    private void setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Authentication successful, show vault content
                showVaultContent();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(VaultActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(VaultActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
        
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Vault Authentication")
                .setSubtitle("Authenticate to access your private photos")
                .setNegativeButtonText("Cancel")
                .build();
    }
    
    private void setupClickListeners() {
        authenticateButton.setOnClickListener(v -> {
            if (hasVaultPin()) {
                showAuthMethodDialog();
            } else {
                showCreatePinDialog();
            }
        });
    }
    
    private void checkBiometricAvailability() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Biometric features are available
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "No biometric features available on this device", Toast.LENGTH_SHORT).show();
                // Fall back to PIN only
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric features are currently unavailable", Toast.LENGTH_SHORT).show();
                // Fall back to PIN only
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No biometric credentials enrolled. Please enroll in Settings", Toast.LENGTH_SHORT).show();
                // Fall back to PIN only
                break;
        }
    }
    
    private void showAuthMethodDialog() {
        String[] authOptions = {"Biometric Authentication", "Enter PIN"};
        
        authMethodDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Authentication Method")
                .setItems(authOptions, (dialog, which) -> {
                    if (which == 0) {
                        // Biometric authentication
                        biometricPrompt.authenticate(promptInfo);
                    } else {
                        // PIN authentication
                        showPinDialog();
                    }
                })
                .show();
    }
    
    private void showPinDialog() {
        TextInputEditText pinInput = new TextInputEditText(this);
        pinInput.setHint("Enter your 4-digit PIN");
        
        pinDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Enter PIN")
                .setView(pinInput)
                .setPositiveButton("OK", (dialog, which) -> {
                    String pin = pinInput.getText().toString();
                    if (validatePin(pin)) {
                        showVaultContent();
                    } else {
                        Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showCreatePinDialog() {
        TextInputEditText pinInput = new TextInputEditText(this);
        pinInput.setHint("Create a 4-digit PIN");
        
        createPinDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Create Vault PIN")
                .setMessage("Create a 4-digit PIN for accessing your vault. This is separate from your device's lock screen PIN.")
                .setView(pinInput)
                .setPositiveButton("Create", (dialog, which) -> {
                    String pin = pinInput.getText().toString();
                    if (pin.length() == 4 && pin.matches("\\d+")) {
                        saveVaultPin(pin);
                        Toast.makeText(this, "PIN created successfully", Toast.LENGTH_SHORT).show();
                        showVaultContent();
                    } else {
                        Toast.makeText(this, "Please enter a valid 4-digit PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }
    
    private void showVaultContent() {
        // Hide authentication UI
        authPromptText.setVisibility(View.GONE);
        authenticateButton.setVisibility(View.GONE);
        
        // Show vault content
        vaultRecyclerView.setVisibility(View.VISIBLE);
        
        // TODO: Load encrypted photos from vault
        // For now, we'll use placeholder data
        loadEncryptedPhotos();
    }
    
    private void loadEncryptedPhotos() {
        // Load actual encrypted photos from vault directory
        File vaultDir = new File(getFilesDir(), "vault");
        List<String> encryptedPhotoPaths = new ArrayList<>();
        
        if (vaultDir.exists() && vaultDir.isDirectory()) {
            File[] files = vaultDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (isImageFile(file)) {
                        encryptedPhotoPaths.add(file.getAbsolutePath());
                    }
                }
            }
        }
        
        vaultAdapter.setEncryptedPhotoPaths(encryptedPhotoPaths);
        
        // Show empty state if no photos
        if (encryptedPhotoPaths.isEmpty()) {
            emptyVaultText.setVisibility(View.VISIBLE);
            vaultRecyclerView.setVisibility(View.GONE);
        } else {
            emptyVaultText.setVisibility(View.GONE);
            vaultRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || fileName.endsWith(".gif") ||
               fileName.endsWith(".bmp") || fileName.endsWith(".webp");
    }
    
    // PIN management methods
    private boolean hasVaultPin() {
        return getPreferences(MODE_PRIVATE).contains(PIN_KEY);
    }
    
    private void saveVaultPin(String pin) {
        getPreferences(MODE_PRIVATE).edit()
                .putString(PIN_KEY, pin)
                .apply();
    }
    
    private boolean validatePin(String pin) {
        String savedPin = getPreferences(MODE_PRIVATE).getString(PIN_KEY, "");
        return savedPin.equals(pin);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dismiss dialogs to prevent window leaks
        if (authMethodDialog != null && authMethodDialog.isShowing()) {
            authMethodDialog.dismiss();
        }
        if (pinDialog != null && pinDialog.isShowing()) {
            pinDialog.dismiss();
        }
        if (createPinDialog != null && createPinDialog.isShowing()) {
            createPinDialog.dismiss();
        }
    }
}