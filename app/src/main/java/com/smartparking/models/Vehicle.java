// app/src/main/java/com/smartparking/models/Vehicle.java
package com.smartparking.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

@Entity(tableName = "vehicles",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("userId")}) // Add index on userId to fix the warning
public class Vehicle {
    @PrimaryKey
    @NonNull
    private String vehicleId;
    private String userId;
    private String licensePlate;
    private String make;
    private String model;
    private String color;
    private String vehicleType;

    // Keep this constructor for Room to use
    public Vehicle() {
        // Required empty constructor for Firebase and Room
    }

    // Add @Ignore to the parameterized constructor
    @Ignore
    public Vehicle(@NonNull String vehicleId, String userId, String licensePlate,
                   String make, String model, String color, String vehicleType) {
        this.vehicleId = vehicleId;
        this.userId = userId;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.color = color;
        this.vehicleType = vehicleType;
    }

    @NonNull
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(@NonNull String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}