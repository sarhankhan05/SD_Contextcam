package com.example.sd_contextcam.data;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagRepository {
    private static final String TAG = "TagRepository";
    private TagDao tagDao;
    private PhotoDao photoDao;
    private PhotoTagJoinDao photoTagJoinDao;
    private AppDatabase db;

    public TagRepository(Context context) {
        db = AppDatabase.getDatabase(context);
        tagDao = db.tagDao();
        photoDao = db.photoDao();
        photoTagJoinDao = db.photoTagJoinDao();
    }

    // Tag operations
    public List<Tag> getAllTags() {
        try {
            return tagDao.getAllTags();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all tags", e);
            return List.of();
        }
    }

    public Tag getTagById(int id) {
        try {
            return tagDao.getTagById(id);
        } catch (Exception e) {
            Log.e(TAG, "Error getting tag by id: " + id, e);
            return null;
        }
    }

    public Tag getTagByName(String name) {
        try {
            return tagDao.getTagByName(name);
        } catch (Exception e) {
            Log.e(TAG, "Error getting tag by name: " + name, e);
            return null;
        }
    }

    public List<Tag> getChildTags(int parentId) {
        try {
            return tagDao.getChildTags(parentId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting child tags for parent: " + parentId, e);
            return List.of();
        }
    }

    public long insertTag(Tag tag) {
        try {
            return tagDao.insertTag(tag);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting tag", e);
            return -1;
        }
    }

    public void updateTag(Tag tag) {
        try {
            tagDao.updateTag(tag);
        } catch (Exception e) {
            Log.e(TAG, "Error updating tag", e);
        }
    }

    public void deleteTag(Tag tag) {
        try {
            tagDao.deleteTag(tag);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting tag", e);
        }
    }

    // Photo operations
    public List<Photo> getAllPhotos() {
        try {
            return photoDao.getAllPhotos();
        } catch (Exception e) {
            Log.e(TAG, "Error getting all photos", e);
            return List.of();
        }
    }

    public Photo getPhotoById(int id) {
        try {
            return photoDao.getPhotoById(id);
        } catch (Exception e) {
            Log.e(TAG, "Error getting photo by id: " + id, e);
            return null;
        }
    }

    public Photo getPhotoByFilePath(String filePath) {
        try {
            return photoDao.getPhotoByFilePath(filePath);
        } catch (Exception e) {
            Log.e(TAG, "Error getting photo by file path: " + filePath, e);
            return null;
        }
    }

    public long insertPhoto(Photo photo) {
        try {
            return photoDao.insertPhoto(photo);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting photo", e);
            return -1;
        }
    }

    public void updatePhoto(Photo photo) {
        try {
            photoDao.updatePhoto(photo);
        } catch (Exception e) {
            Log.e(TAG, "Error updating photo", e);
        }
    }

    public void deletePhoto(Photo photo) {
        try {
            photoDao.deletePhoto(photo);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting photo", e);
        }
    }

    // Photo-Tag join operations
    public void addTagToPhoto(int photoId, int tagId) {
        try {
            PhotoTagJoin join = new PhotoTagJoin(photoId, tagId);
            photoTagJoinDao.insertPhotoTagJoin(join);
        } catch (Exception e) {
            Log.e(TAG, "Error adding tag to photo", e);
        }
    }

    public void removeTagFromPhoto(int photoId, int tagId) {
        try {
            photoTagJoinDao.deletePhotoTagJoin(photoId, tagId);
        } catch (Exception e) {
            Log.e(TAG, "Error removing tag from photo", e);
        }
    }

    // Helper method to get all photos with a specific tag
    public List<Photo> getPhotosWithTag(int tagId) {
        try {
            // Get all photo-tag joins for this tag
            List<PhotoTagJoin> joins = photoTagJoinDao.getPhotoJoinsForTag(tagId);
            
            // Get all photos
            List<Photo> allPhotos = getAllPhotos();
            
            // Create a map of photo id to photo for quick lookup
            Map<Integer, Photo> photoMap = new HashMap<>();
            for (Photo photo : allPhotos) {
                photoMap.put(photo.id, photo);
            }
            
            // Build result list with only photos that have this tag
            List<Photo> result = new ArrayList<>();
            for (PhotoTagJoin join : joins) {
                Photo photo = photoMap.get(join.photoId);
                if (photo != null) {
                    result.add(photo);
                }
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error getting photos with tag: " + tagId, e);
            return new ArrayList<>();
        }
    }
    
    // Helper method to get photo count for a specific tag
    public int getPhotoCountForTag(int tagId) {
        try {
            List<PhotoTagJoin> joins = photoTagJoinDao.getPhotoJoinsForTag(tagId);
            return joins.size();
        } catch (Exception e) {
            Log.e(TAG, "Error getting photo count for tag: " + tagId, e);
            return 0;
        }
    }
}