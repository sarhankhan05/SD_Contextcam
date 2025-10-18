package com.example.sd_contextcam.security;

import android.content.Context;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public class EncryptionUtil {
    private static final String TAG = "EncryptionUtil";
    private static final String VAULT_DIRECTORY = "vault";
    
    private MasterKey masterKey;
    private Context context;
    
    public EncryptionUtil(Context context) {
        this.context = context;
        try {
            masterKey = new MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error creating master key", e);
        }
    }
    
    /**
     * Encrypts a photo file and stores it in the vault
     * @param inputFile The original photo file to encrypt
     * @param fileName The name to give the encrypted file
     * @return The path to the encrypted file, or null if encryption failed
     */
    public String encryptPhoto(File inputFile, String fileName) {
        if (masterKey == null) {
            Log.e(TAG, "Master key not initialized");
            return null;
        }
        
        try {
            // Create vault directory if it doesn't exist
            File vaultDir = new File(context.getFilesDir(), VAULT_DIRECTORY);
            if (!vaultDir.exists()) {
                vaultDir.mkdirs();
            }
            
            // Create encrypted file
            File encryptedFile = new File(vaultDir, fileName);
            EncryptedFile encryptedVaultFile = new EncryptedFile.Builder(
                    context,
                    encryptedFile,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
            
            // Copy and encrypt the file
            try (InputStream inputStream = new FileInputStream(inputFile);
                 OutputStream outputStream = encryptedVaultFile.openFileOutput()) {
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            
            return encryptedFile.getAbsolutePath();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error encrypting photo", e);
            return null;
        }
    }
    
    /**
     * Decrypts a photo file from the vault
     * @param encryptedFilePath The path to the encrypted file
     * @param outputFilePath The path where the decrypted file should be saved
     * @return True if decryption was successful, false otherwise
     */
    public boolean decryptPhoto(String encryptedFilePath, String outputFilePath) {
        if (masterKey == null) {
            Log.e(TAG, "Master key not initialized");
            return false;
        }
        
        try {
            File encryptedFile = new File(encryptedFilePath);
            File outputFile = new File(outputFilePath);
            
            EncryptedFile encryptedVaultFile = new EncryptedFile.Builder(
                    context,
                    encryptedFile,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
            
            // Copy and decrypt the file
            try (InputStream inputStream = encryptedVaultFile.openFileInput();
                 OutputStream outputStream = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            
            return true;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error decrypting photo", e);
            return false;
        }
    }
    
    /**
     * Deletes an encrypted photo from the vault
     * @param encryptedFilePath The path to the encrypted file
     * @return True if deletion was successful, false otherwise
     */
    public boolean deleteEncryptedPhoto(String encryptedFilePath) {
        try {
            File encryptedFile = new File(encryptedFilePath);
            return encryptedFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting encrypted photo", e);
            return false;
        }
    }
}