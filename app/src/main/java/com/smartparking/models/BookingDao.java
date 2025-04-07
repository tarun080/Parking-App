// app/src/main/java/com/smartparking/models/BookingDao.java
package com.smartparking.models;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Booking booking);

    @Update
    void update(Booking booking);

    @Delete
    void delete(Booking booking);

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY startTime DESC")
    LiveData<List<Booking>> getBookingsByUserId(String userId);

    @Query("SELECT * FROM bookings WHERE bookingId = :bookingId")
    LiveData<Booking> getBookingById(String bookingId);

    @Query("SELECT * FROM bookings WHERE parkingSpaceId = :parkingSpaceId AND endTime > :currentTime")
    LiveData<List<Booking>> getActiveBookingsForParkingSpace(String parkingSpaceId, long currentTime);

    @Query("SELECT * FROM bookings WHERE userId = :userId AND bookingStatus IN ('RESERVED', 'ACTIVE') ORDER BY startTime ASC")
    LiveData<List<Booking>> getActiveBookingsForUser(String userId);

    @Query("SELECT * FROM bookings WHERE userId = :userId AND bookingStatus = 'COMPLETED' ORDER BY endTime DESC")
    LiveData<List<Booking>> getPastBookingsForUser(String userId);

    @Query("DELETE FROM bookings")
    void deleteAllBookings();
}