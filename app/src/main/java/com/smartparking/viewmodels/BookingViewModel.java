// app/src/main/java/com/smartparking/viewmodels/BookingViewModel.java
package com.smartparking.viewmodels;

import android.app.Application;

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

public class BookingViewModel extends AndroidViewModel {

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
            bookingRepository.fetchBookingsFromFirestore(userId);
        }
    }

    public LiveData<List<Booking>> getAllBookings() {
        return bookingRepository.getBookingsByUserId(userId);
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

        // Get parking space details
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

    public void cancelBooking(String bookingId) {
        bookingInProgress.setValue(true);

        bookingRepository.cancelBooking(bookingId).observeForever(success -> {
            bookingInProgress.setValue(false);
            if (!success) {
                bookingError.setValue("Failed to cancel booking");
            }
        });
    }

    // Generate mock bookings for testing
    public void generateMockBookings() {
        if (!userId.isEmpty()) {
            bookingRepository.generateMockBookings(userId);
        }
    }
}