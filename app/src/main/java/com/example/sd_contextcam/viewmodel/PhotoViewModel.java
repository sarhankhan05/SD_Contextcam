package com.example.sd_contextcam.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.data.Tag;
import com.example.sd_contextcam.data.TagRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoViewModel extends AndroidViewModel {
    private static final String TAG = "PhotoViewModel";
    private TagRepository repository;
    private ExecutorService executorService;

    // The single source of truth for the list of photos being displayed.
    private MutableLiveData<List<Photo>> photosLiveData = new MutableLiveData<>();
    private MutableLiveData<List<Tag>> tagsLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public interface InsertCallback {
        void onInsertComplete(long id);
    }

    public interface TagCallback {
        void onTagReceived(Tag tag);
    }

    public PhotoViewModel(@NonNull Application application) {
        super(application);
        repository = new TagRepository(application);
        executorService = Executors.newFixedThreadPool(2);
        isLoading.postValue(false);
    }

    // --- LiveData Getters ---
    public LiveData<List<Photo>> getPhotos() {
        return photosLiveData; // The Activity observes this
    }

    public LiveData<List<Tag>> getTags() {
        return tagsLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public TagRepository getRepository() {
        return repository;
    }

    public void loadPhotosByTagId(int tagId) {
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                List<Photo> photos = repository.getPhotosWithTag(tagId);
                // Correctly post the new list to the main photosLiveData object
                photosLiveData.postValue(photos);
            } catch (Exception e) {
                Log.e(TAG, "Error loading photos for tag: " + tagId, e);
                photosLiveData.postValue(new ArrayList<>()); // Post an empty list on error
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    // ========================== END OF FIX ===========================

    public void loadPhotos() {
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
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

    // --- Data Modification Methods (Your existing code, looks good) ---

    public void addPhoto(Photo photo, InsertCallback callback) {
        executorService.execute(() -> {
            long newId = -1;
            try {
                newId = repository.insertPhoto(photo);
                Log.d(TAG, "Inserted photo, got ID: " + newId);
            } catch (Exception e) {
                Log.e(TAG, "Error adding photo", e);
            } finally {
                final long finalId = newId;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onInsertComplete(finalId);
                    }
                });
            }
        });
    }

    public void addTag(Tag tag, InsertCallback callback) {
        executorService.execute(() -> {
            long newId = -1;
            try {
                newId = repository.insertTag(tag);
                Log.d(TAG, "Inserted tag, got ID: " + newId);
                loadTags();
            } catch (Exception e) {
                Log.e(TAG, "Error adding tag", e);
            } finally {
                final long finalId = newId;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onInsertComplete(finalId);
                    }
                });
            }
        });
    }

    public void addTagToPhoto(long photoId, int tagId) {
        executorService.execute(() -> {
            try {
                repository.addTagToPhoto((int) photoId, tagId);
                Log.d(TAG, "Linked photo " + photoId + " to tag " + tagId);
            } catch (Exception e) {
                Log.e(TAG, "Error adding tag to photo", e);
            }
        });
    }

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

    public void removeTagFromPhoto(int photoId, int tagId) {
        executorService.execute(() -> {
            try {
                repository.removeTagFromPhoto(photoId, tagId);
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
