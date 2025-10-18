package com.example.sd_contextcam.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PhotoTagJoinDao {
    @Query("SELECT * FROM photo_tag_join WHERE photoId = :photoId")
    List<PhotoTagJoin> getTagJoinsForPhoto(int photoId);

    @Query("SELECT * FROM photo_tag_join WHERE tagId = :tagId")
    List<PhotoTagJoin> getPhotoJoinsForTag(int tagId);

    @Insert
    void insertPhotoTagJoin(PhotoTagJoin photoTagJoin);

    @Query("DELETE FROM photo_tag_join WHERE photoId = :photoId AND tagId = :tagId")
    void deletePhotoTagJoin(int photoId, int tagId);

    @Query("DELETE FROM photo_tag_join WHERE photoId = :photoId")
    void deleteAllTagJoinsForPhoto(int photoId);

    @Query("DELETE FROM photo_tag_join WHERE tagId = :tagId")
    void deleteAllPhotoJoinsForTag(int tagId);
}