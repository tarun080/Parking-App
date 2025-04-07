// app/src/main/java/com/smartparking/models/ParkingSpace.java
package com.smartparking.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;

@Entity(tableName = "parking_spaces")
public class ParkingSpace {

    @PrimaryKey
    @NonNull
    private String spaceId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int totalSpots;
    private int availableSpots;
    private double hourlyRate;
    private boolean isActive;
    private String ownerId;

    // Keep this constructor for Room to use
    public ParkingSpace() {
        // Required empty constructor for Firebase and Room
    }

    // Add @Ignore to the parameterized constructor
    @Ignore
    public ParkingSpace(@NonNull String spaceId, String name, String address,
                        double latitude, double longitude, int totalSpots,
                        double hourlyRate, String ownerId) {
        this.spaceId = spaceId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalSpots = totalSpots;
        this.availableSpots = totalSpots; // Initially all spots are available
        this.hourlyRate = hourlyRate;
        this.isActive = true;
        this.ownerId = ownerId;
    }

    @NonNull
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(@NonNull String spaceId) {
        this.spaceId = spaceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getTotalSpots() {
        return totalSpots;
    }

    public void setTotalSpots(int totalSpots) {
        this.totalSpots = totalSpots;
    }

    public int getAvailableSpots() {
        return availableSpots;
    }

    public void setAvailableSpots(int availableSpots) {
        this.availableSpots = availableSpots;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    // Helper method to create GeoPoint for Firestore
    public GeoPoint getGeoPoint() {
        return new GeoPoint(latitude, longitude);
    }
}