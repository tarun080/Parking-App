// app/src/main/java/com/smartparking/models/ParkingSpaceDao.java
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
public interface ParkingSpaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ParkingSpace parkingSpace);

    @Update
    void update(ParkingSpace parkingSpace);

    @Delete
    void delete(ParkingSpace parkingSpace);

    @Query("SELECT * FROM parking_spaces")
    LiveData<List<ParkingSpace>> getAllParkingSpaces();

    @Query("SELECT * FROM parking_spaces WHERE spaceId = :spaceId")
    LiveData<ParkingSpace> getParkingSpaceById(String spaceId);

    @Query("SELECT * FROM parking_spaces WHERE availableSpots > 0")
    LiveData<List<ParkingSpace>> getAvailableParkingSpaces();

    @Query("SELECT * FROM parking_spaces WHERE ownerId = :ownerId")
    LiveData<List<ParkingSpace>> getParkingSpacesByOwnerId(String ownerId);

    @Query("DELETE FROM parking_spaces")
    void deleteAllParkingSpaces();
}