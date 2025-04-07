// app/src/main/java/com/smartparking/repositories/RoomRepository.java
package com.smartparking.repositories;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.smartparking.models.ParkingDatabase;
import com.smartparking.models.User;
import com.smartparking.models.UserDao;
import com.smartparking.models.Vehicle;
import com.smartparking.models.VehicleDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomRepository {
    private final UserDao userDao;
    private final VehicleDao vehicleDao;
    private final ExecutorService executorService;

    public RoomRepository(Application application) {
        ParkingDatabase database = ParkingDatabase.getInstance(application);
        userDao = database.userDao();
        vehicleDao = database.vehicleDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    // User operations
    public void insertUser(User user) {
        executorService.execute(() -> userDao.insert(user));
    }

    public void updateUser(User user) {
        executorService.execute(() -> userDao.update(user));
    }

    public void deleteUser(User user) {
        executorService.execute(() -> userDao.delete(user));
    }

    public LiveData<User> getUserById(String userId) {
        return userDao.getUserById(userId);
    }

    public void deleteAllUsers() {
        executorService.execute(() -> userDao.deleteAllUsers());
    }

    // Vehicle operations
    public void insertVehicle(Vehicle vehicle) {
        executorService.execute(() -> vehicleDao.insert(vehicle));
    }

    public void updateVehicle(Vehicle vehicle) {
        executorService.execute(() -> vehicleDao.update(vehicle));
    }

    public void deleteVehicle(Vehicle vehicle) {
        executorService.execute(() -> vehicleDao.delete(vehicle));
    }

    public LiveData<List<Vehicle>> getVehiclesByUserId(String userId) {
        return vehicleDao.getVehiclesByUserId(userId);
    }

    public LiveData<Vehicle> getVehicleById(String vehicleId) {
        return vehicleDao.getVehicleById(vehicleId);
    }

    public void deleteAllVehiclesByUserId(String userId) {
        executorService.execute(() -> vehicleDao.deleteAllVehiclesByUserId(userId));
    }
}