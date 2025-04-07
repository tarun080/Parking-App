// app/src/main/java/com/smartparking/viewmodels/MapViewModel.java
package com.smartparking.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.smartparking.models.ParkingSpace;
import com.smartparking.repositories.ParkingRepository;

import java.util.ArrayList;
import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private final ParkingRepository parkingRepository;
    private final MutableLiveData<Double> userLatitude = new MutableLiveData<>();
    private final MutableLiveData<Double> userLongitude = new MutableLiveData<>();
    private final MutableLiveData<Double> searchRadius = new MutableLiveData<>(50.0); // Default 5km radius

    public MapViewModel(@NonNull Application application) {
        super(application);
        parkingRepository = new ParkingRepository(application);
        parkingRepository.addMockParkingSpaces();
    }

    public void setUserLocation(double latitude, double longitude) {
        userLatitude.setValue(latitude);
        userLongitude.setValue(longitude);
    }

    public void addParkingSpace(ParkingSpace parkingSpace) {
        parkingRepository.addParkingSpaceToFirestore(parkingSpace);
    }

    public void setSearchRadius(double radius) {
        searchRadius.setValue(radius);
    }

    public LiveData<Double> getUserLatitude() {
        return userLatitude;
    }

    public LiveData<Double> getUserLongitude() {
        return userLongitude;
    }

    public LiveData<Double> getSearchRadius() {
        return searchRadius;
    }

    public LiveData<List<ParkingSpace>> getNearbyParkingSpaces() {
        Double lat = userLatitude.getValue();
        Double lng = userLongitude.getValue();
        Double radius = searchRadius.getValue();

        if (lat == null || lng == null || radius == null) {
            return new MutableLiveData<>();
        }

        return parkingRepository.getNearbyParkingSpaces(lat, lng, radius);
    }

    public LiveData<List<ParkingSpace>> getAllParkingSpaces() {
        return parkingRepository.getAllParkingSpaces();
    }

    public LiveData<List<ParkingSpace>> getAvailableParkingSpaces() {
        return parkingRepository.getAvailableParkingSpaces();
    }

    public void refreshParkingSpaces() {
        parkingRepository.fetchParkingSpacesFromFirestore();
    }

    public LiveData<ParkingSpace> getParkingSpaceById(String spaceId) {
        return parkingRepository.getParkingSpaceById(spaceId);
    }

    public LiveData<List<ParkingSpace>> searchParkingSpaces(String query) {
        MutableLiveData<List<ParkingSpace>> searchResultsLiveData = new MutableLiveData<>();

        getAllParkingSpaces().observeForever(parkingSpaces -> {
            if (parkingSpaces != null) {
                List<ParkingSpace> filteredSpaces = new ArrayList<>();

                for (ParkingSpace space : parkingSpaces) {
                    if (space.getName().toLowerCase().contains(query.toLowerCase()) ||
                            space.getAddress().toLowerCase().contains(query.toLowerCase())) {
                        filteredSpaces.add(space);
                    }
                }

                searchResultsLiveData.setValue(filteredSpaces);
            }
        });

        return searchResultsLiveData;
    }
}