// Create this new file: C:/Users/dhruv/SD_Contextcam/app/src/main/java/com/example/sd_contextcam/MediaSaver.java
package com.example.sd_contextcam;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;

public class MediaSaver {

    private static final String TAG = "MediaSaver";

    /**
     * Saves a photo to the public gallery. Handles both encrypted (vault) and normal photos.
     *
     * @param context          The context.
     * @param photoPath        The file path of the photo to save.
     * @param isVaultPhoto     True if the photo is encrypted and needs decryption first.
     * @return                 True if the photo was saved successfully, false otherwise.
     */
    public static boolean savePhotoToGallery(Context context, String photoPath, boolean isVaultPhoto) {
        // First, get the photo as a Bitmap. Decrypt it if it's from the vault.
        Bitmap bitmapToSave;
        if (isVaultPhoto) {
            // Decrypt the photo from the vault to get the Bitmap
            bitmapToSave = CryptoUtils.decryptPhoto(photoPath, context);
        } else {
            // For non-vault photos, just load the Bitmap from the path
            // This assumes you have a way to load normal photos if they exist.
            // If all photos are encrypted, you can simplify this.
            bitmapToSave = CryptoUtils.loadNormalPhoto(photoPath);
        }

        if (bitmapToSave == null) {
            Log.e(TAG, "Failed to get Bitmap. Photo not saved.");
            return false;
        }

        // Now, save the Bitmap to the MediaStore (public gallery)
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // Specify the "Pictures" directory for organization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        }

        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (imageUri == null) {
            Log.e(TAG, "Failed to create new MediaStore record.");
            return false;
        }

        try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
            if (outputStream == null) {
                Log.e(TAG, "Failed to get output stream.");
                return false;
            }
            // Compress the Bitmap to the output stream as a JPEG
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            Log.d(TAG, "Photo saved successfully to gallery: " + imageUri.toString());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving photo to gallery", e);
            // Clean up if the save fails
            resolver.delete(imageUri, null, null);
            return false;
        }
    }
}
