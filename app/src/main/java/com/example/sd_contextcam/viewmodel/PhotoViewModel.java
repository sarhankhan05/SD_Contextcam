package com.example.sd_contextcam.viewmodel;

import android.app.Application;
import android.os.Handler; // <-- ADD THIS IMPORT
import android.os.Looper;  // <-- ADD THIS IMPORT
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.data.Tag;
import com.example.sd_contextcam.data.TagRepository; // Assuming this is your Repository class

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoViewModel extends AndroidViewModel {
    private static final String TAG = "PhotoViewModel";
    private TagRepository repository; // Renamed for clarity if it handles more than just tags
    private ExecutorService executorService;

    // LiveData (Keep these for observing lists)
    private MutableLiveData<List<Photo>> photosLiveData = new MutableLiveData<>();
    private MutableLiveData<List<Tag>> tagsLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    // private MutableLiveData<Integer> lastInsertedPhotoId = new MutableLiveData<>(); // REMOVED - Replaced by callbacks

    // --- ADD Callback Interface for Inserts ---
    public interface InsertCallback {
        void onInsertComplete(long id); // Use long for Room IDs
    }
    // --- END ---

    // Callback for fetching a single tag
    public interface TagCallback {
        void onTagReceived(Tag tag);
    }

    public PhotoViewModel(@NonNull Application application) {
        super(application);
        // Ensure TagRepository handles Photo and PhotoTagJoin operations too
        repository = new TagRepository(application);
        executorService = Executors.newFixedThreadPool(2);
        isLoading.postValue(false);
    }

    // --- LiveData Getters (Keep these) ---
    public LiveData<List<Photo>> getPhotos() {
        return photosLiveData;
    }

    public LiveData<List<Tag>> getTags() {
        return tagsLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /* // REMOVED getter for lastInsertedPhotoId
    public LiveData<Integer> getLastInsertedPhotoId() {
        return lastInsertedPhotoId;
    }
    */

    public TagRepository getRepository() { // Consider renaming if it handles Photos too
        return repository;
    }

    // --- Data Loading Methods (Keep these) ---

    public LiveData<List<Photo>> getPhotosByTag(int tagId) {
        MutableLiveData<List<Photo>> photosByTagLiveData = new MutableLiveData<>();
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                // Ensure repository has this method
                List<Photo> photos = repository.getPhotosWithTag(tagId);
                photosByTagLiveData.postValue(photos);
            } catch (Exception e) {
                Log.e(TAG, "Error loading photos for tag: " + tagId, e);
                photosByTagLiveData.postValue(new ArrayList<>());
            } finally {
                isLoading.postValue(false);
            }
        });
        return photosByTagLiveData;
    }

    public void loadPhotos() {
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                // Ensure repository has this method
                List<Photo> photos = repository.getAllPhotos();
                photosLiveData.postValue(photos);
            } catch (Exception e) {
                Log.e(TAG, "Error loading photos", e);
                photosLiveData.postValue(new ArrayList<>());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void loadTags() {
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                // Ensure repository has this method
                List<Tag> tags = repository.getAllTags();
                tagsLiveData.postValue(tags);
            } catch (Exception e) {
                Log.e(TAG, "Error loading tags", e);
                tagsLiveData.postValue(new ArrayList<>());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // --- Data Modification Methods ---

    // --- MODIFIED addPhoto to use Callback ---
    public void addPhoto(Photo photo, InsertCallback callback) {
        executorService.execute(() -> {
            long newId = -1; // Default error value
            try {
                // IMPORTANT: Ensure repository.insertPhoto returns long (the row ID)
                newId = repository.insertPhoto(photo);
                Log.d(TAG, "Inserted photo, got ID: " + newId);
                // Optionally reload all photos if needed elsewhere, but don't rely on it for the ID
                // loadPhotos();
            } catch (Exception e) {
                Log.e(TAG, "Error adding photo", e);
            } finally {
                final long finalId = newId;
                // Post result back to the main thread for the callback
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onInsertComplete(finalId);
                    }
                });
            }
        });
    }

    // --- MODIFIED addTag to use Callback ---
    public void addTag(Tag tag, InsertCallback callback) {
        executorService.execute(() -> {
            long newId = -1; // Default error value
            try {
                // IMPORTANT: Ensure repository.insertTag returns long (the row ID)
                newId = repository.insertTag(tag);
                Log.d(TAG, "Inserted tag, got ID: " + newId);
                // Reload tags immediately so they appear in suggestions
                loadTags();
            } catch (Exception e) {
                Log.e(TAG, "Error adding tag", e);
            } finally {
                final long finalId = newId;
                // Post result back to the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onInsertComplete(finalId);
                    }
                });
            }
        });
    }

    // --- MODIFIED addTagToPhoto to accept long photoId ---
    public void addTagToPhoto(long photoId, int tagId) { // Changed first parameter to long
        executorService.execute(() -> {
            try {
                // IMPORTANT: Ensure repository.addTagToPhoto accepts (long, int)
                repository.addTagToPhoto((int) photoId, tagId);
                Log.d(TAG, "Linked photo " + photoId + " to tag " + tagId);
            } catch (Exception e) {
                Log.e(TAG, "Error adding tag to photo", e);
            }
        });
    }
    // --- END MODIFICATION ---

    // getTagByName with callback (keep as is, looks correct)
    public void getTagByName(String tagName, TagCallback callback) {
        executorService.execute(() -> {
            Tag tag = null;
            try {
                tag = repository.getTagByName(tagName);
            } catch (Exception e) {
                Log.e(TAG, "Error getting tag by name: " + tagName, e);
            } finally {
                final Tag finalTag = tag;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onTagReceived(finalTag);
                    }
                });
            }
        });
    }

    // removeTagFromPhoto (keep as is, but consider changing photoId to long)
    public void removeTagFromPhoto(int photoId, int tagId) { // Consider changing photoId to long
        executorService.execute(() -> {
            try {
                repository.removeTagFromPhoto(photoId, tagId); // Ensure repository matches
            } catch (Exception e) {
                Log.e(TAG, "Error removing tag from photo", e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}