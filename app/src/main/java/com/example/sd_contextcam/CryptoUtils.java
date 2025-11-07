// Create this new file: C:/Users/dhruv/SD_Contextcam/app/src/main/java/com/example/sd_contextcam/CryptoUtils.java
package com.example.sd_contextcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public class CryptoUtils {

    private static final String TAG = "CryptoUtils";

    /**
     * Creates and returns an EncryptedFile object for reading or writing.
     * This is the core of the security library.
     */
    private static EncryptedFile getEncryptedFile(String filePath, Context context) throws GeneralSecurityException, IOException {
        // 1. Create or retrieve the master key for encryption.
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        // 2. Create an EncryptedFile object for the given file path.
        return new EncryptedFile.Builder(
                new File(filePath),
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();
    }

    /**
     * Decrypts an encrypted photo file and returns it as a Bitmap.
     *
     * @param encryptedPath The path to the encrypted file.
     * @param context       The application context.
     * @return A Bitmap of the decrypted photo, or null if decryption fails.
     */
    public static Bitmap decryptPhoto(String encryptedPath, Context context) {
        try {
            EncryptedFile encryptedFile = getEncryptedFile(encryptedPath, context);

            // Use the encrypted file's InputStream to read decrypted bytes.
            InputStream inputStream = encryptedFile.openFileInput();
            return BitmapFactory.decodeStream(inputStream);

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to decrypt photo", e);
            return null;
        }
    }

    /**
     * Loads a normal, unencrypted photo from a given path.
     * This is used as a fallback or for non-vault images.
     *
     * @param photoPath The path to the photo file.
     * @return A Bitmap of the photo, or null if loading fails.
     */
    public static Bitmap loadNormalPhoto(String photoPath) {
        try {
            return BitmapFactory.decodeFile(photoPath);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load normal photo", e);
            return null;
        }
    }

    /**
     * Encrypts a Bitmap and saves it to a specified file path.
     *
     * @param bitmapToEncrypt The Bitmap image to encrypt.
     * @param outputPath      The path where the encrypted file will be saved.
     * @param context         The application context.
     * @return True if encryption was successful, false otherwise.
     */
    public static boolean encryptAndSavePhoto(Bitmap bitmapToEncrypt, String outputPath, Context context) {
        try {
            EncryptedFile encryptedFile = getEncryptedFile(outputPath, context);

            // Convert Bitmap to a byte array
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmapToEncrypt.compress(Bitmap.CompressFormat.JPEG, 95, byteStream);
            byte[] byteArray = byteStream.toByteArray();

            // Use the encrypted file's OutputStream to write encrypted bytes.
            FileOutputStream outputStream = encryptedFile.openFileOutput();
            outputStream.write(byteArray);
            outputStream.flush();
            outputStream.close();

            Log.d(TAG, "Photo encrypted and saved successfully to: " + outputPath);
            return true;

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to encrypt and save photo", e);
            return false;
        }
    }
}
