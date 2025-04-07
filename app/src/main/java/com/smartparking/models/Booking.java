// app/src/main/java/com/smartparking/models/Booking.java
package com.smartparking.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "bookings")
//,
//        foreignKeys = {
//                @ForeignKey(entity = User.class,
//                        parentColumns = "userId",
//                        childColumns = "userId",
//                        onDelete = ForeignKey.CASCADE),
//                @ForeignKey(entity = ParkingSpace.class,
//                        parentColumns = "spaceId",
//                        childColumns = "parkingSpaceId",
//                        onDelete = ForeignKey.CASCADE)
//        })
public class Booking {

    @PrimaryKey
    @NonNull
    private String bookingId;
    private String userId;
    private String parkingSpaceId;
    private String vehicleId;
    private long startTime;
    private long endTime;
    private double totalAmount;
    private String paymentStatus; // PENDING, COMPLETED, FAILED
    private String bookingStatus; // RESERVED, ACTIVE, COMPLETED, CANCELLED
    private long createdAt;

    public Booking() {
        // Required empty constructor for Firebase
    }

    public Booking(@NonNull String bookingId, String userId, String parkingSpaceId,
                   String vehicleId, long startTime, long endTime, double totalAmount) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.parkingSpaceId = parkingSpaceId;
        this.vehicleId = vehicleId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;
        this.paymentStatus = "PENDING";
        this.bookingStatus = "RESERVED";
        this.createdAt = new Date().getTime();
    }

    @NonNull
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(@NonNull String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getParkingSpaceId() {
        return parkingSpaceId;
    }

    public void setParkingSpaceId(String parkingSpaceId) {
        this.parkingSpaceId = parkingSpaceId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Helper method to check if booking is active
    public boolean isActive() {
        long currentTime = new Date().getTime();
        return bookingStatus.equals("ACTIVE") &&
                startTime <= currentTime &&
                endTime >= currentTime;
    }

    // Helper method to calculate duration in hours
    public double getDurationHours() {
        return (endTime - startTime) / (1000.0 * 60 * 60);
    }
}