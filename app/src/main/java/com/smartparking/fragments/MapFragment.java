// app/src/main/java/com/smartparking/fragments/MapFragment.java
package com.smartparking.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smartparking.R;
import com.smartparking.models.ParkingSpace;
import com.smartparking.viewmodels.MapViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private MapView mapView;
    private MapViewModel mapViewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FloatingActionButton fabMyLocation;
    private EditText editTextSearch;
    private ImageButton buttonFilter;

    private boolean locationPermissionGranted = false;
    private List<Marker> parkingMarkers = new ArrayList<>();

    // Mumbai coordinates for fixed location
    private final double MUMBAI_LAT = 19.1079172;
    private final double MUMBAI_LNG = 72.834547;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize OSMDroid configuration
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireActivity().getPackageName());

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "MapFragment view created");

        // Initialize views
        mapView = view.findViewById(R.id.mapView);
        fabMyLocation = view.findViewById(R.id.fab_my_location);
        editTextSearch = view.findViewById(R.id.editTextSearch);
        buttonFilter = view.findViewById(R.id.buttonFilter);

        // Configure map
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Set click listeners
        fabMyLocation.setOnClickListener(v -> moveToUserLocation());

        // Set up search functionality
        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchParkingSpaces(editTextSearch.getText().toString());
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editTextSearch.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Set up filter button
        buttonFilter.setOnClickListener(v -> {
            Log.d(TAG, "Filter button clicked");
            ParkingFilterDialogFragment dialogFragment = new ParkingFilterDialogFragment();
            dialogFragment.show(getParentFragmentManager(), "filter_dialog");
        });

        // Initialize ViewModel
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Request location permissions if not granted
        checkLocationPermission();

        // Use a fixed location for Mumbai
        updateUserLocation(MUMBAI_LAT, MUMBAI_LNG);
        mapViewModel.setUserLocation(MUMBAI_LAT, MUMBAI_LNG);

        // Add mock parking spaces directly
        addAndDisplayMockParkingSpaces();

        Log.d(TAG, "MapFragment initialized");
    }

    private void checkLocationPermission() {
        Log.d(TAG, "Checking location permissions");

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting location permissions");
                requestPermissions(REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // Permissions already granted
        Log.d(TAG, "Location permissions already granted");
        locationPermissionGranted = true;
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permissions granted");
                locationPermissionGranted = true;
                startLocationUpdates();
            } else {
                Log.d(TAG, "Location permissions denied");
                Toast.makeText(getContext(), "Location permission is required to show nearby parking", Toast.LENGTH_LONG).show();

                // Still use the fixed Mumbai location
                updateUserLocation(MUMBAI_LAT, MUMBAI_LNG);
            }
        }
    }

    private void startLocationUpdates() {
        if (!locationPermissionGranted) {
            Log.d(TAG, "Cannot start location updates, permission not granted");
            return;
        }

        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(10000)  // 10 seconds
                    .setFastestInterval(5000);  // 5 seconds

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Try to get last location but don't rely on it
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d(TAG, "Last location: " + location.getLatitude() + ", " + location.getLongitude());
                        updateUserLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d(TAG, "Last location is null, using Mumbai location");
                        updateUserLocation(MUMBAI_LAT, MUMBAI_LNG);
                    }
                });

                // Initialize locationCallback if needed
                if (locationCallback == null) {
                    locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            Location location = locationResult.getLastLocation();
                            if (location != null) {
                                Log.d(TAG, "User location updated: " + location.getLatitude() + ", " + location.getLongitude());
                                updateUserLocation(location.getLatitude(), location.getLongitude());
                            } else {
                                Log.e(TAG, "Location result was null");
                            }
                        }
                    };
                }

                // Try to request updates but catch any exceptions
                try {
                    Log.d(TAG, "Requesting location updates");
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting location updates", e);
                    // Continue using the fixed location
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates", e);
            // Continue using fixed location
            updateUserLocation(MUMBAI_LAT, MUMBAI_LNG);
        }
    }

    private void updateUserLocation(double latitude, double longitude) {
        Log.d(TAG, "Updating user location: " + latitude + ", " + longitude);
        mapViewModel.setUserLocation(latitude, longitude);

        // Center map on user's location
        GeoPoint userPoint = new GeoPoint(latitude, longitude);
        mapView.getController().animateTo(userPoint);

        // Add or update user marker
        updateUserMarker(userPoint);
    }

    private void updateUserMarker(GeoPoint userPoint) {
        // Find existing user marker
        Marker userMarker = null;
        for (Marker marker : parkingMarkers) {
            if (marker.getId().equals("user_location")) {
                userMarker = marker;
                break;
            }
        }

        // Create new marker if not found
        if (userMarker == null) {
            Log.d(TAG, "Creating new user marker");
            userMarker = new Marker(mapView);
            userMarker.setId("user_location");
            userMarker.setTitle("My Location");
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            Drawable userIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_my_location);
            if (userIcon != null) {
                userMarker.setIcon(userIcon);
            } else {
                Log.e(TAG, "User location icon drawable not found");
            }

            parkingMarkers.add(userMarker);
            mapView.getOverlays().add(userMarker);
        }

        userMarker.setPosition(userPoint);
        mapView.invalidate();
    }

    private void moveToUserLocation() {
        Log.d(TAG, "Move to user location requested");
        Double latitude = mapViewModel.getUserLatitude().getValue();
        Double longitude = mapViewModel.getUserLongitude().getValue();

        if (latitude != null && longitude != null) {
            Log.d(TAG, "Moving to user location: " + latitude + ", " + longitude);
            GeoPoint userPoint = new GeoPoint(latitude, longitude);
            mapView.getController().animateTo(userPoint);
            mapView.getController().setZoom(17.0);
        } else {
            Log.d(TAG, "Moving to default Mumbai location");
            GeoPoint userPoint = new GeoPoint(MUMBAI_LAT, MUMBAI_LNG);
            mapView.getController().animateTo(userPoint);
            mapView.getController().setZoom(17.0);
        }
    }

    private void displayParkingSpaces(List<ParkingSpace> parkingSpaces) {
        Log.d(TAG, "Displaying " + parkingSpaces.size() + " parking spaces on map");

        // Remove old parking markers
        List<Marker> markersToKeep = new ArrayList<>();
        for (Marker marker : parkingMarkers) {
            if (marker.getId().equals("user_location")) {
                markersToKeep.add(marker);
            } else {
                mapView.getOverlays().remove(marker);
            }
        }
        parkingMarkers.clear();
        parkingMarkers.addAll(markersToKeep);

        // Create new markers for parking spaces
        for (ParkingSpace parkingSpace : parkingSpaces) {
            Log.d(TAG, "Creating marker for " + parkingSpace.getName() + " at " +
                    parkingSpace.getLatitude() + ", " + parkingSpace.getLongitude());

            Marker marker = new Marker(mapView);
            marker.setId(parkingSpace.getSpaceId());
            marker.setTitle(parkingSpace.getName());
            marker.setSnippet("Available: " + parkingSpace.getAvailableSpots() +
                    " / " + parkingSpace.getTotalSpots() +
                    "\nRate: $" + parkingSpace.getHourlyRate() + "/hr");
            marker.setPosition(new GeoPoint(parkingSpace.getLatitude(), parkingSpace.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            // Set different icons based on availability
            int iconRes = parkingSpace.getAvailableSpots() > 0 ?
                    R.drawable.ic_parking_available : R.drawable.ic_parking_full;
            Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
            if (icon != null) {
                marker.setIcon(icon);
            } else {
                Log.e(TAG, "Parking icon drawable not found for resource: " + iconRes);
            }

            // Handle marker click
            marker.setOnMarkerClickListener((m, mapView) -> {
                Log.d(TAG, "Marker clicked: " + parkingSpace.getName());
                showParkingDetailsDialog(parkingSpace);
                return true;
            });

            parkingMarkers.add(marker);
            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    private void searchParkingSpaces(String query) {
        Log.d(TAG, "Searching for parking spaces with query: " + query);

        if (query == null || query.isEmpty()) {
            // Show all mock spaces if query is empty
            displayParkingSpaces(createMockParkingSpaces());
            return;
        }

        // Filter mock spaces by name/address
        List<ParkingSpace> allSpaces = createMockParkingSpaces();
        List<ParkingSpace> filteredSpaces = new ArrayList<>();

        for (ParkingSpace space : allSpaces) {
            if (space.getName().toLowerCase().contains(query.toLowerCase()) ||
                    space.getAddress().toLowerCase().contains(query.toLowerCase())) {
                filteredSpaces.add(space);
            }
        }

        Log.d(TAG, "Found " + filteredSpaces.size() + " spaces matching query: " + query);
        displayParkingSpaces(filteredSpaces);
    }

    private void showParkingDetailsDialog(ParkingSpace parkingSpace) {
        Log.d(TAG, "Showing details dialog for " + parkingSpace.getName());
        ParkingDetailsDialogFragment dialogFragment = ParkingDetailsDialogFragment.newInstance(parkingSpace.getSpaceId());
        // We need to pass the parking space to the dialog since it might not be in Firestore
        Bundle args = new Bundle();
        args.putString("name", parkingSpace.getName());
        args.putString("address", parkingSpace.getAddress());
        args.putInt("availableSpots", parkingSpace.getAvailableSpots());
        args.putInt("totalSpots", parkingSpace.getTotalSpots());
        args.putDouble("hourlyRate", parkingSpace.getHourlyRate());
        dialogFragment.setArguments(args);
        dialogFragment.show(getParentFragmentManager(), "parking_details");
    }

    // Method to directly create and show mock parking spaces
    private void addAndDisplayMockParkingSpaces() {
        List<ParkingSpace> mockSpaces = createMockParkingSpaces();
        Log.d(TAG, "Created " + mockSpaces.size() + " mock parking spaces");
        displayParkingSpaces(mockSpaces);
    }

    // Create mock parking spaces for testing
    private List<ParkingSpace> createMockParkingSpaces() {
        List<ParkingSpace> mockSpaces = new ArrayList<>();

        // Mumbai coordinates
        double mumbaiLat = MUMBAI_LAT;
        double mumbaiLng = MUMBAI_LNG;

        // Create parking spaces around that location
        mockSpaces.add(new ParkingSpace(
                "parking-college",
                "College Parking",
                "Mukesh Patel School, Vile Parle West, Mumbai",
                mumbaiLat,
                mumbaiLng,
                40,
                2.00,
                "owner1"
        ));

        mockSpaces.add(new ParkingSpace(
                "parking-mall",
                "Shopping Mall Parking",
                "Juhu Mall, Mumbai",
                mumbaiLat + 0.003,
                mumbaiLng - 0.002,
                100,
                3.50,
                "owner2"
        ));

        mockSpaces.add(new ParkingSpace(
                "parking-airport",
                "Airport Parking",
                "Mumbai International Airport",
                mumbaiLat - 0.002,
                mumbaiLng + 0.005,
                200,
                5.00,
                "owner3"
        ));

        mockSpaces.add(new ParkingSpace(
                "parking-station",
                "Railway Station Parking",
                "Vile Parle Station, Mumbai",
                mumbaiLat + 0.001,
                mumbaiLng - 0.001,
                60,
                1.50,
                "owner1"
        ));

        // Set one parking space as full
        ParkingSpace airportParking = mockSpaces.get(2);
        airportParking.setAvailableSpots(0);

        return mockSpaces;
    }

    // Add a method to manually add a test parking spot at current location
    public void addTestSpotAtCurrentLocation() {
        Double lat = mapViewModel.getUserLatitude().getValue();
        Double lng = mapViewModel.getUserLongitude().getValue();

        if (lat == null || lng == null) {
            lat = MUMBAI_LAT;
            lng = MUMBAI_LNG;
        }

        ParkingSpace testSpace = new ParkingSpace(
                "test-spot-" + UUID.randomUUID().toString(),
                "Test Parking Spot",
                "Current Location",
                lat,
                lng,
                10,
                2.0,
                "testowner"
        );

        // Add to displayed spaces
        List<ParkingSpace> currentSpaces = new ArrayList<>(createMockParkingSpaces());
        currentSpaces.add(testSpace);
        displayParkingSpaces(currentSpaces);

        Toast.makeText(requireContext(), "Added test parking spot at your location", Toast.LENGTH_SHORT).show();
    }

    public void refreshMapData() {
        Log.d(TAG, "Refreshing map data");

        // For Firebase implementation, get the user location
        Double lat = mapViewModel.getUserLatitude().getValue();
        Double lng = mapViewModel.getUserLongitude().getValue();

        if (lat != null && lng != null) {
            // This will trigger a refresh of the nearby parking spaces
            mapViewModel.setUserLocation(lat, lng);
        } else {
            // Use default location
            mapViewModel.setUserLocation(MUMBAI_LAT, MUMBAI_LNG);
        }

        // For mock data approach, recreate the spaces
        addAndDisplayMockParkingSpaces();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "MapFragment resumed");
        mapView.onResume();
        if (locationPermissionGranted) {
            try {
                startLocationUpdates();
            } catch (Exception e) {
                Log.e(TAG, "Error starting location updates in onResume", e);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "MapFragment paused");
        mapView.onPause();
        try {
            if (fusedLocationClient != null && locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing location updates", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MapFragment destroyed");
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}