package com.example.sd_contextcam.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;

@Entity(
    tableName = "photo_tag_join",
    primaryKeys = {"photoId", "tagId"},
    foreignKeys = {
        @ForeignKey(
            entity = Photo.class,
            parentColumns = "id",
            childColumns = "photoId",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Tag.class,
            parentColumns = "id",
            childColumns = "tagId",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("photoId"),
        @Index("tagId")
    }
)
public class PhotoTagJoin {
    public int photoId;
    public int tagId;

    @Ignore
    public PhotoTagJoin(int photoId, int tagId) {
        this.photoId = photoId;
        this.tagId = tagId;
    }

    // Default constructor for Room
    public PhotoTagJoin() {
    }

    // Getters and setters
    public int getPhotoId() {
        return photoId;
    }

    public void setPhotoId(int photoId) {
        this.photoId = photoId;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }
}