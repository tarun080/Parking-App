// app/src/main/java/com/smartparking/models/VehicleDao.java
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
public interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Vehicle vehicle);

    @Update
    void update(Vehicle vehicle);

    @Delete
    void delete(Vehicle vehicle);

    @Query("SELECT * FROM vehicles WHERE userId = :userId")
    LiveData<List<Vehicle>> getVehiclesByUserId(String userId);

    @Query("SELECT * FROM vehicles WHERE vehicleId = :vehicleId")
    LiveData<Vehicle> getVehicleById(String vehicleId);

    @Query("DELETE FROM vehicles WHERE userId = :userId")
    void deleteAllVehiclesByUserId(String userId);
}