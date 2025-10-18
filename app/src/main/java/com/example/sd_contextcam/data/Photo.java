package com.example.sd_contextcam.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "photos")
public class Photo {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String filePath;
    public long timestamp;
    public Double latitude = null;  // Changed to Double (nullable) instead of double
    public Double longitude = null; // Changed to Double (nullable) instead of double
    public String wifiNetwork = "";
    public String calendarEvent = "";

    @Ignore
    public Photo(String filePath, long timestamp) {
        this.filePath = filePath;
        this.timestamp = timestamp;
    }

    // Default constructor for Room
    public Photo() {
    }

    // Getters and setters
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

    public Double getLatitude() {  // Changed return type to Double
        return latitude;
    }

    public void setLatitude(Double latitude) {  // Changed parameter type to Double
        this.latitude = latitude;
    }

    public Double getLongitude() {  // Changed return type to Double
        return longitude;
    }

    public void setLongitude(Double longitude) {  // Changed parameter type to Double
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
}