package com.example.sd_contextcam.viewmodel;

import android.app.Application;
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
    
    // LiveData for photos
    private MutableLiveData<List<Photo>> photosLiveData = new MutableLiveData<>();
    private MutableLiveData<List<Tag>> tagsLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Integer> lastInsertedPhotoId = new MutableLiveData<>();
    
    public interface TagCallback {
        void onTagReceived(Tag tag);
    }
    
    public PhotoViewModel(@NonNull Application application) {
        super(application);
        repository = new TagRepository(application);
        executorService = Executors.newFixedThreadPool(2);
        // Use postValue for initial value setting as well
        isLoading.postValue(false);
    }
    
    public LiveData<List<Photo>> getPhotos() {
        return photosLiveData;
    }
    
    public LiveData<List<Tag>> getTags() {
        return tagsLiveData;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<Integer> getLastInsertedPhotoId() {
        return lastInsertedPhotoId;
    }
    
    public TagRepository getRepository() {
        return repository;
    }
    
    public LiveData<List<Photo>> getPhotosByTag(int tagId) {
        MutableLiveData<List<Photo>> photosByTagLiveData = new MutableLiveData<>();
        // Use postValue instead of setValue when updating from any thread
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                List<Photo> photos = repository.getPhotosWithTag(tagId);
                // Use postValue instead of setValue when updating from background thread
                photosByTagLiveData.postValue(photos);
            } catch (Exception e) {
                Log.e(TAG, "Error loading photos for tag: " + tagId, e);
                photosByTagLiveData.postValue(new ArrayList<>());
            } finally {
                // Use postValue instead of setValue when updating from background thread
                isLoading.postValue(false);
            }
        });
        return photosByTagLiveData;
    }
    
    public void loadPhotos() {
        // Use postValue instead of setValue when updating from any thread
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                List<Photo> photos = repository.getAllPhotos();
                // Use postValue instead of setValue when updating from background thread
                photosLiveData.postValue(photos);
            } catch (Exception e) {
                Log.e(TAG, "Error loading photos", e);
            } finally {
                // Use postValue instead of setValue when updating from background thread
                isLoading.postValue(false);
            }
        });
    }
    
    public void loadTags() {
        // Use postValue instead of setValue when updating from any thread
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                List<Tag> tags = repository.getAllTags();
                // Use postValue instead of setValue when updating from background thread
                tagsLiveData.postValue(tags);
            } catch (Exception e) {
                Log.e(TAG, "Error loading tags", e);
            } finally {
                // Use postValue instead of setValue when updating from background thread
                isLoading.postValue(false);
            }
        });
    }
    
    public void addPhoto(Photo photo) {
        executorService.execute(() -> {
            try {
                long photoId = repository.insertPhoto(photo);
                // Use postValue instead of setValue when updating from background thread
                lastInsertedPhotoId.postValue((int) photoId);
                // Reload photos after adding
                loadPhotos();
            } catch (Exception e) {
                Log.e(TAG, "Error adding photo", e);
            }
        });
    }
    
    public void addTag(Tag tag) {
        executorService.execute(() -> {
            try {
                repository.insertTag(tag);
                // Reload tags after adding
                loadTags();
            } catch (Exception e) {
                Log.e(TAG, "Error adding tag", e);
            }
        });
    }
    
    public void addTagToPhoto(int photoId, int tagId) {
        executorService.execute(() -> {
            try {
                repository.addTagToPhoto(photoId, tagId);
            } catch (Exception e) {
                Log.e(TAG, "Error adding tag to photo", e);
            }
        });
    }
    
    public void getTagByName(String tagName, TagCallback callback) {
        executorService.execute(() -> {
            try {
                Tag tag = repository.getTagByName(tagName);
                // Call the callback on the main thread
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onTagReceived(tag);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting tag by name: " + tagName, e);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onTagReceived(null);
                    });
                }
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