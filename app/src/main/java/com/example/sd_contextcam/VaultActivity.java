package com.example.sd_contextcam;

import android.os.Bundle;
import android.view.LayoutInflater; // <-- ADD THIS IMPORT
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
import com.google.android.material.textfield.TextInputLayout; // <-- ADD THIS IMPORT

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
        vaultAdapter = new VaultAdapter(); // Assuming VaultAdapter exists
        vaultRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        vaultRecyclerView.setAdapter(vaultAdapter);

        // vaultAdapter.setOnVaultPhotoClickListener(...); // Implement click listener if needed
    }

    private void setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
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

    // checkBiometricAvailability() remains unchanged

    private void showAuthMethodDialog() {
        // Only show Biometric option if available
        BiometricManager biometricManager = BiometricManager.from(this);
        List<String> authOptionsList = new ArrayList<>();
        boolean canUseBiometrics = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;

        if (canUseBiometrics) {
            authOptionsList.add("Biometric Authentication");
        }
        authOptionsList.add("Enter PIN"); // Always offer PIN

        String[] authOptions = authOptionsList.toArray(new String[0]);

        // Dismiss previous dialog if showing
        if (authMethodDialog != null && authMethodDialog.isShowing()) {
            authMethodDialog.dismiss();
        }

        authMethodDialog = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme) // Apply custom theme
                .setTitle("Choose Authentication Method")
                .setItems(authOptions, (dialog, which) -> {
                    String selectedOption = authOptions[which];
                    if (selectedOption.equals("Biometric Authentication")) {
                        biometricPrompt.authenticate(promptInfo);
                    } else { // "Enter PIN"
                        showPinDialog();
                    }
                })
                .setOnCancelListener(dialog -> finish()) // Optional: Close activity if they cancel selection
                .show();
    }


    // --- MODIFIED showPinDialog ---
    private void showPinDialog() {
        // Dismiss previous dialog if showing
        if (pinDialog != null && pinDialog.isShowing()) {
            pinDialog.dismiss();
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_pin_entry, null);

        final TextInputEditText pinInput = dialogView.findViewById(R.id.pinInputEditText);
        Button confirmButton = dialogView.findViewById(R.id.confirmPinButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelPinButton);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                .setView(dialogView)
                .setCancelable(false); // User must interact with buttons

        pinDialog = builder.create();

        confirmButton.setOnClickListener(v -> {
            String pin = pinInput.getText() != null ? pinInput.getText().toString() : "";
            if (validatePin(pin)) {
                showVaultContent();
                pinDialog.dismiss();
            } else {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
                // pinInput.setText(""); // Optionally clear input on failure
            }
        });

        cancelButton.setOnClickListener(v -> {
            pinDialog.dismiss();
            // Optional: You might want to show the auth method choice again or finish
            showAuthMethodDialog(); // Go back to choosing auth method
            // finish(); // Or just close the vault
        });

        pinDialog.show();
    }
    // --- END MODIFICATION ---


    // --- MODIFIED showCreatePinDialog ---
    private void showCreatePinDialog() {
        // Dismiss previous dialog if showing
        if (createPinDialog != null && createPinDialog.isShowing()) {
            createPinDialog.dismiss();
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_pin_entry, null); // Reuse the same layout

        // Get references and customize
        TextView title = dialogView.findViewById(R.id.pinPromptTitle);
        final TextInputEditText pinInput = dialogView.findViewById(R.id.pinInputEditText);
        TextInputLayout pinInputContainer = dialogView.findViewById(R.id.pinInputContainer);
        Button confirmButton = dialogView.findViewById(R.id.confirmPinButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelPinButton);

        title.setText("Create Vault PIN");
        pinInputContainer.setHint("Create a 4-digit PIN");
        confirmButton.setText("Create");
        cancelButton.setVisibility(View.GONE); // No cancelling PIN creation

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                // Removed setTitle from here, using the one in the custom layout
                .setMessage("Create a 4-digit PIN for accessing your vault.") // Keep message for context
                .setView(dialogView)
                .setCancelable(false);

        createPinDialog = builder.create();

        confirmButton.setOnClickListener(v -> {
            String pin = pinInput.getText() != null ? pinInput.getText().toString() : "";
            if (pin.length() == 4 && pin.matches("\\d+")) {
                saveVaultPin(pin);
                Toast.makeText(this, "PIN created successfully", Toast.LENGTH_SHORT).show();
                showVaultContent();
                createPinDialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter a valid 4-digit PIN", Toast.LENGTH_SHORT).show();
            }
        });

        createPinDialog.show();
    }
    // --- END MODIFICATION ---


    private void showVaultContent() {
        // Hide authentication UI
        authPromptText.setVisibility(View.GONE);
        authenticateButton.setVisibility(View.GONE);

        // Show vault content
        vaultRecyclerView.setVisibility(View.VISIBLE);

        loadEncryptedPhotos();
    }

    private void loadEncryptedPhotos() {
        // Load actual encrypted photos from vault directory
        File vaultDir = new File(getFilesDir(), "vault"); // Use app's private directory
        List<String> encryptedPhotoPaths = new ArrayList<>();

        if (vaultDir.exists() && vaultDir.isDirectory()) {
            File[] files = vaultDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Assuming encrypted files might have a specific extension or pattern
                    // Or just list all files if only photos are stored there
                    // if (isImageFile(file)) { // Check might be needed if other files exist
                    encryptedPhotoPaths.add(file.getAbsolutePath());
                    // }
                }
            }
        }

        // Pass the list to the adapter
        if (vaultAdapter != null) {
            vaultAdapter.setEncryptedPhotoPaths(encryptedPhotoPaths); // Make sure adapter has this method
        }


        // Show empty state if no photos
        if (encryptedPhotoPaths.isEmpty()) {
            emptyVaultText.setVisibility(View.VISIBLE);
            vaultRecyclerView.setVisibility(View.GONE);
        } else {
            emptyVaultText.setVisibility(View.GONE);
            vaultRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // isImageFile() method can be removed if not strictly needed for filtering vault files

    // PIN management methods (remain unchanged)
    private boolean hasVaultPin() {
        return getPreferences(MODE_PRIVATE).contains(PIN_KEY);
    }

    private void saveVaultPin(String pin) {
        // Consider encrypting the PIN before saving for better security
        getPreferences(MODE_PRIVATE).edit()
                .putString(PIN_KEY, pin) // In a real app, hash/encrypt this PIN
                .apply();
    }

    private boolean validatePin(String pin) {
        String savedPin = getPreferences(MODE_PRIVATE).getString(PIN_KEY, "");
        // In a real app, compare hashes or use secure comparison
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