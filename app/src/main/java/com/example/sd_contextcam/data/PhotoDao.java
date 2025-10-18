package com.example.sd_contextcam.data;

import android.util.Log;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PhotoDao {
    @Query("SELECT * FROM photos")
    List<Photo> getAllPhotos();

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    Photo getPhotoById(int id);

    @Query("SELECT * FROM photos WHERE filePath = :filePath LIMIT 1")
    Photo getPhotoByFilePath(String filePath);

    @Insert
    long insertPhoto(Photo photo);

    @Update
    void updatePhoto(Photo photo);

    @Delete
    void deletePhoto(Photo photo);

    @Query("DELETE FROM photos")
    void deleteAllPhotos();
}