package com.example.sd_contextcam.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TagDao {
    @Query("SELECT * FROM tags")
    List<Tag> getAllTags();

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    Tag getTagById(int id);

    @Query("SELECT * FROM tags WHERE parentId = :parentId")
    List<Tag> getChildTags(int parentId);

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    Tag getTagByName(String name);

    @Insert
    long insertTag(Tag tag);

    @Update
    void updateTag(Tag tag);

    @Delete
    void deleteTag(Tag tag);

    @Query("DELETE FROM tags")
    void deleteAllTags();
}