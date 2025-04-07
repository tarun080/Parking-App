// app/src/main/java/com/smartparking/viewmodels/BookingViewModel.java
package com.smartparking.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.smartparking.models.Booking;
import com.smartparking.models.ParkingSpace;
import com.smartparking.repositories.BookingRepository;
import com.smartparking.repositories.ParkingRepository;

import java.util.List;
import java.util.UUID;

public class BookingViewModel extends AndroidViewModel {

    private static final String TAG = "BookingViewModel";

    private final BookingRepository bookingRepository;
    private final ParkingRepository parkingRepository;
    private final String userId;

    private final MutableLiveData<Boolean> bookingInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<String> bookingError = new MutableLiveData<>();

    public BookingViewModel(@NonNull Application application) {
        super(application);
        bookingRepository = new BookingRepository(application);
        parkingRepository = new ParkingRepository(application);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser != null ? currentUser.getUid() : "";

        if (!userId.isEmpty()) {
            // Only fetch existing bookings from Firestore, don't generate mock ones automatically
            bookingRepository.fetchBookingsFromFirestore(userId);
        }
    }

    public LiveData<List<Booking>> getAllBookings() {
        return bookingRepository.getBookingsByUserId(userId);
    }

    private boolean mockDataGenerated = false;

    // Generate mock bookings for testing - only when explicitly called
    public void generateMockBookings() {
        if (!userId.isEmpty() && !mockDataGenerated) {
            bookingRepository.generateMockBookings(userId);
            mockDataGenerated = true;
        }
    }

    public LiveData<List<Booking>> getActiveBookings() {
        return bookingRepository.getActiveBookingsForUser(userId);
    }

    public LiveData<List<Booking>> getPastBookings() {
        return bookingRepository.getPastBookingsForUser(userId);
    }

    public LiveData<Booking> getBookingById(String bookingId) {
        return bookingRepository.getBookingById(bookingId);
    }

    public LiveData<Boolean> getBookingInProgress() {
        return bookingInProgress;
    }

    public LiveData<String> getBookingError() {
        return bookingError;
    }

    public void createBooking(String parkingSpaceId, String vehicleId, long startTime, long endTime) {
        if (userId.isEmpty()) {
            bookingError.setValue("User not logged in");
            return;
        }

        bookingInProgress.setValue(true);

        // Check if this is a mock parking space ID (usually starts with "parking-")
        if (parkingSpaceId.startsWith("parking-")) {
            Log.d(TAG, "Creating booking for mock parking space: " + parkingSpaceId);

            // For mock data, create a booking directly
            createMockBooking(parkingSpaceId, vehicleId, startTime, endTime);
        } else {
            // For real data, get parking space details first
            parkingRepository.getParkingSpaceById(parkingSpaceId).observeForever(parkingSpace -> {
                if (parkingSpace != null) {
                    // Create booking object
                    Booking booking = bookingRepository.createBookingObject(
                            userId,
                            parkingSpaceId,
                            vehicleId,
                            startTime,
                            endTime,
                            parkingSpace.getHourlyRate()
                    );

                    // Create booking in repository
                    bookingRepository.createBooking(booking, parkingSpace).observeForever(success -> {
                        bookingInProgress.setValue(false);
                        if (!success) {
                            bookingError.setValue("Failed to create booking");
                        }
                    });
                } else {
                    bookingInProgress.setValue(false);
                    bookingError.setValue("Parking space not found");
                }
            });
        }
    }

    private void createMockBooking(String parkingSpaceId, String vehicleId, long startTime, long endTime) {
        try {
            // Create a default mock space with reasonable values
            ParkingSpace mockSpace = new ParkingSpace(
                    parkingSpaceId,
                    "Mock Parking Space",
                    "Mock Address",
                    0, 0,
                    10,
                    2.0,
                    "mock_owner"
            );

            // Create a booking object
            String bookingId = UUID.randomUUID().toString();
            long durationMillis = endTime - startTime;
            double durationHours = durationMillis / (1000.0 * 60 * 60);
            double totalAmount = mockSpace.getHourlyRate() * durationHours;

            Booking booking = new Booking(
                    bookingId,
                    userId,
                    parkingSpaceId,
                    vehicleId,
                    startTime,
                    endTime,
                    totalAmount
            );

            // Add to local Room database
            bookingRepository.insertBooking(booking);

            // Add to Firestore
            bookingRepository.addMockBookingToFirestore(booking);

            // Success
            bookingInProgress.setValue(false);

            Log.d(TAG, "Mock booking created successfully: " + bookingId);
        } catch (Exception e) {
            Log.e(TAG, "Error creating mock booking", e);
            bookingInProgress.setValue(false);
            bookingError.setValue("Error creating booking: " + e.getMessage());
        }
    }

    public void cancelBooking(String bookingId) {
        bookingInProgress.setValue(true);

        bookingRepository.cancelBooking(bookingId).observeForever(success -> {
            bookingInProgress.setValue(false);
            if (!success) {
                bookingError.setValue("Failed to cancel booking");
            }
        });
    }
}