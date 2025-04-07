// app/src/main/java/com/smartparking/repositories/ParkingRepository.java
package com.smartparking.repositories;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smartparking.models.ParkingDatabase;
import com.smartparking.models.ParkingSpace;
import com.smartparking.models.ParkingSpaceDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParkingRepository {
    private static final String TAG = "ParkingRepository";

    private final ParkingSpaceDao parkingSpaceDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;

    public ParkingRepository(Application application) {
        ParkingDatabase database = ParkingDatabase.getInstance(application);
        parkingSpaceDao = database.parkingSpaceDao();
        firestore = FirebaseFirestore.getInstance();
        executorService = Executors.newFixedThreadPool(4);
    }

    // Room database operations
    public void insertParkingSpace(ParkingSpace parkingSpace) {
        executorService.execute(() -> parkingSpaceDao.insert(parkingSpace));
    }

    public void updateParkingSpace(ParkingSpace parkingSpace) {
        executorService.execute(() -> parkingSpaceDao.update(parkingSpace));
    }

    public void deleteParkingSpace(ParkingSpace parkingSpace) {
        executorService.execute(() -> parkingSpaceDao.delete(parkingSpace));
    }

    public LiveData<List<ParkingSpace>> getAllParkingSpaces() {
        return parkingSpaceDao.getAllParkingSpaces();
    }

    public LiveData<ParkingSpace> getParkingSpaceById(String spaceId) {
        return parkingSpaceDao.getParkingSpaceById(spaceId);
    }

    public LiveData<List<ParkingSpace>> getAvailableParkingSpaces() {
        return parkingSpaceDao.getAvailableParkingSpaces();
    }

    public LiveData<List<ParkingSpace>> getParkingSpacesByOwnerId(String ownerId) {
        return parkingSpaceDao.getParkingSpacesByOwnerId(ownerId);
    }

    // Firestore operations
    public void fetchParkingSpacesFromFirestore() {
        firestore.collection("parkingSpaces")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ParkingSpace> parkingSpaces = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ParkingSpace parkingSpace = document.toObject(ParkingSpace.class);
                        // Handle GeoPoint conversion if needed
                        GeoPoint geoPoint = document.getGeoPoint("location");
                        if (geoPoint != null) {
                            parkingSpace.setLatitude(geoPoint.getLatitude());
                            parkingSpace.setLongitude(geoPoint.getLongitude());
                        }
                        parkingSpaces.add(parkingSpace);

                        // Insert into Room database
                        insertParkingSpace(parkingSpace);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching parking spaces", e));
    }

    public void addParkingSpaceToFirestore(ParkingSpace parkingSpace) {
        Map<String, Object> parkingData = new HashMap<>();
        parkingData.put("spaceId", parkingSpace.getSpaceId());
        parkingData.put("name", parkingSpace.getName());
        parkingData.put("address", parkingSpace.getAddress());
        parkingData.put("location", new GeoPoint(parkingSpace.getLatitude(), parkingSpace.getLongitude()));
        parkingData.put("totalSpots", parkingSpace.getTotalSpots());
        parkingData.put("availableSpots", parkingSpace.getAvailableSpots());
        parkingData.put("hourlyRate", parkingSpace.getHourlyRate());
        parkingData.put("isActive", parkingSpace.isActive());
        parkingData.put("ownerId", parkingSpace.getOwnerId());

        firestore.collection("parkingSpaces")
                .document(parkingSpace.getSpaceId())
                .set(parkingData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Parking space added to Firestore");
                    // Also add to local database
                    insertParkingSpace(parkingSpace);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error adding parking space", e));
    }

    public void updateParkingSpaceInFirestore(ParkingSpace parkingSpace) {
        Map<String, Object> parkingData = new HashMap<>();
        parkingData.put("name", parkingSpace.getName());
        parkingData.put("address", parkingSpace.getAddress());
        parkingData.put("location", new GeoPoint(parkingSpace.getLatitude(), parkingSpace.getLongitude()));
        parkingData.put("totalSpots", parkingSpace.getTotalSpots());
        parkingData.put("availableSpots", parkingSpace.getAvailableSpots());
        parkingData.put("hourlyRate", parkingSpace.getHourlyRate());
        parkingData.put("isActive", parkingSpace.isActive());

        firestore.collection("parkingSpaces")
                .document(parkingSpace.getSpaceId())
                .update(parkingData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Parking space updated in Firestore");
                    // Also update local database
                    updateParkingSpace(parkingSpace);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating parking space", e));
    }

    public void deleteParkingSpaceFromFirestore(String spaceId) {
        firestore.collection("parkingSpaces")
                .document(spaceId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Parking space deleted from Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting parking space", e));
    }

    public LiveData<List<ParkingSpace>> getNearbyParkingSpaces(double latitude, double longitude, double radiusInKm) {
        MutableLiveData<List<ParkingSpace>> nearbySpacesLiveData = new MutableLiveData<>();

        // In a production app, you would use Firestore GeoQuery or a similar solution
        // For simplicity, we'll fetch all parking spaces and filter them locally
        firestore.collection("parkingSpaces")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ParkingSpace> nearbySpaces = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ParkingSpace parkingSpace = document.toObject(ParkingSpace.class);

                        // Handle GeoPoint conversion if needed
                        GeoPoint geoPoint = document.getGeoPoint("location");
                        if (geoPoint != null) {
                            parkingSpace.setLatitude(geoPoint.getLatitude());
                            parkingSpace.setLongitude(geoPoint.getLongitude());
                        }

                        // Calculate distance
                        double distance = calculateDistance(
                                latitude, longitude,
                                parkingSpace.getLatitude(), parkingSpace.getLongitude());

                        if (distance <= radiusInKm) {
                            nearbySpaces.add(parkingSpace);
                        }
                    }
                    nearbySpacesLiveData.setValue(nearbySpaces);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching nearby parking spaces", e);
                    nearbySpacesLiveData.setValue(new ArrayList<>());
                });

        return nearbySpacesLiveData;
    }

    // Helper method to calculate distance between two coordinates using Haversine formula
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }

    public void addMockParkingSpaces() {
        // Only add mock data if there's no data in Firestore yet
        firestore.collection("parkingSpaces")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Add mock data
                        List<ParkingSpace> mockSpaces = createMockParkingSpaces();
                        for (ParkingSpace space : mockSpaces) {
                            addParkingSpaceToFirestore(space);
                        }
                        Log.d(TAG, "Added mock parking spaces");
                    }
                });
    }

    private List<ParkingSpace> createMockParkingSpaces() {
        List<ParkingSpace> mockSpaces = new ArrayList<>();

        // Create some mock parking spaces around a center point
        // These would normally come from your backend
        double centerLat = 37.7749; // San Francisco
        double centerLng = -122.4194;

        // Downtown Parking
        mockSpaces.add(new ParkingSpace(
                "parking1",
                "Downtown Parking",
                "123 Market St, San Francisco, CA",
                centerLat + 0.01,
                centerLng + 0.01,
                50,
                4.50,
                "owner1"
        ));

        // City Center Garage
        mockSpaces.add(new ParkingSpace(
                "parking2",
                "City Center Garage",
                "45 Main St, San Francisco, CA",
                centerLat - 0.015,
                centerLng + 0.005,
                100,
                3.75,
                "owner2"
        ));

        // Union Square Parking
        mockSpaces.add(new ParkingSpace(
                "parking3",
                "Union Square Parking",
                "333 Post St, San Francisco, CA",
                centerLat + 0.005,
                centerLng - 0.01,
                75,
                5.00,
                "owner1"
        ));

        double mumbaiLat = 19.1031; // Approximate latitude for Mukesh Patel College
        double mumbaiLng = 72.8517; // Approximate longitude for Mukesh Patel College

        mockSpaces.add(new ParkingSpace(
                "parking4",
                "Vile Parle College Parking",
                "Near Mukesh Patel School, Vile Parle West, Mumbai",
                19.1079172,  // Slightly offset from exact college location
                72.834547,
                40,                  // Total spots
                2.00,               // Hourly rate in USD (you could adjust this)
                "owner3"
        ));

        // Set some parking spaces as full (0 available spots)
        ParkingSpace fullSpace = mockSpaces.get(3);
        fullSpace.setAvailableSpots(0);

        return mockSpaces;
    }

    public void updateMockParkingSpaceAvailability(String spaceId, int newAvailableSpots) {
        // Update in the local map of mock spaces if we're using that approach
        for (ParkingSpace space : createMockParkingSpaces()) {
            if (space.getSpaceId().equals(spaceId)) {
                space.setAvailableSpots(newAvailableSpots);

                // Update in Firestore
                firestore.collection("parkingSpaces")
                        .document(spaceId)
                        .update("availableSpots", newAvailableSpots)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Mock parking space updated: " + spaceId + " now has " + newAvailableSpots + " spots");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating mock parking space", e);
                        });

                break;
            }
        }
    }
}