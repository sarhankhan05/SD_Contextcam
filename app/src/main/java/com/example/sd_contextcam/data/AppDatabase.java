package com.example.sd_contextcam.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = {Tag.class, Photo.class, PhotoTagJoin.class},
    version = 2,  // Increased version number from 1 to 2
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    public abstract TagDao tagDao();
    public abstract PhotoDao photoDao();
    public abstract PhotoTagJoinDao photoTagJoinDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Log.d(TAG, "Creating database instance");
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "sd_contextcam_database"
                    )
                    .fallbackToDestructiveMigration() // Add this to handle schema changes by recreating the database
                    .build();
                    Log.d(TAG, "Database instance created successfully");
                }
            }
        }
        return INSTANCE;
    }
}