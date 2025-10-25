package com.example.sd_contextcam.data;

import androidx.room.ColumnInfo; // <-- ADD THIS IMPORT
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "photos")
public class Photo {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String filePath;
    public long timestamp;
    public Double latitude = null;
    public Double longitude = null;
    public String wifiNetwork = "";
    public String calendarEvent = "";

    // --- ADD THIS FIELD ---
    @ColumnInfo(name = "is_encrypted") // Optional: Defines the column name
    private boolean encrypted;
    // --- END OF ADDITION ---

    @Ignore
    public Photo(String filePath, long timestamp) {
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.encrypted = false; // Default to false
    }

    // Default constructor for Room
    public Photo() {
        this.encrypted = false; // Default to false
    }

    // --- Getters and setters ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getWifiNetwork() {
        return wifiNetwork;
    }

    public void setWifiNetwork(String wifiNetwork) {
        this.wifiNetwork = wifiNetwork;
    }

    public String getCalendarEvent() {
        return calendarEvent;
    }

    public void setCalendarEvent(String calendarEvent) {
        this.calendarEvent = calendarEvent;
    }

    // --- ADD GETTER AND SETTER FOR ENCRYPTED ---
    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
    // --- END OF ADDITION ---
}