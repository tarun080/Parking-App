package com.smartparking.repositories;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smartparking.models.Booking;
import com.smartparking.models.BookingDao;
import com.smartparking.models.ParkingDatabase;
import com.smartparking.models.ParkingSpace;
import com.smartparking.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingRepository {
    private static final String TAG = "BookingRepository";

    private final BookingDao bookingDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;
    private final ParkingRepository parkingRepository;
    private final Application application; // Store the application reference

    public BookingRepository(Application application) {
        this.application = application; // Save the application reference
        ParkingDatabase database = ParkingDatabase.getInstance(application);
        bookingDao = database.bookingDao();
        firestore = FirebaseFirestore.getInstance();
        executorService = Executors.newFixedThreadPool(4);
        parkingRepository = new ParkingRepository(application);
    }

    // Room database operations
    public void insertBooking(Booking booking) {
        executorService.execute(() -> bookingDao.insert(booking));
    }

    public void updateBooking(Booking booking) {
        executorService.execute(() -> bookingDao.update(booking));
    }

    public void deleteBooking(Booking booking) {
        executorService.execute(() -> bookingDao.delete(booking));
    }

    public LiveData<List<Booking>> getBookingsByUserId(String userId) {
        return bookingDao.getBookingsByUserId(userId);
    }

    public LiveData<Booking> getBookingById(String bookingId) {
        return bookingDao.getBookingById(bookingId);
    }

    public LiveData<List<Booking>> getActiveBookingsForUser(String userId) {
        return bookingDao.getActiveBookingsForUser(userId);
    }

    public LiveData<List<Booking>> getPastBookingsForUser(String userId) {
        return bookingDao.getPastBookingsForUser(userId);
    }

    // Firestore operations
    public void fetchBookingsFromFirestore(String userId) {
        firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Booking> bookings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        bookings.add(booking);

                        // Insert into Room database
                        insertBooking(booking);
                    }
                    Log.d(TAG, "Fetched " + bookings.size() + " bookings for user " + userId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching bookings", e));
    }

    public MutableLiveData<Boolean> createBooking(Booking booking, ParkingSpace parkingSpace) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        // Check if parking space is available
        if (parkingSpace.getAvailableSpots() <= 0) {
            Log.e(TAG, "Cannot create booking, no available spots");
            success.setValue(false);
            return success;
        }

        // Add to Firestore
        firestore.collection("bookings")
                .document(booking.getBookingId())
                .set(booking)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking added to Firestore");

                    // Insert into Room database
                    insertBooking(booking);

                    // Update available spots in parking space
                    parkingSpace.setAvailableSpots(parkingSpace.getAvailableSpots() - 1);
                    parkingRepository.updateParkingSpaceInFirestore(parkingSpace);

                    // Send booking confirmation notification
                    Context context = application.getApplicationContext();
                    NotificationHelper.showBookingConfirmationNotification(
                            context,
                            booking.getBookingId(),
                            parkingSpace.getName()
                    );

                    // Schedule reminder notifications
                    if (booking.getStartTime() > System.currentTimeMillis()) {
                        NotificationHelper.scheduleBookingReminderNotification(
                                context,
                                booking.getBookingId(),
                                parkingSpace.getName(),
                                booking.getStartTime()
                        );
                    }

                    NotificationHelper.scheduleBookingExpiryNotification(
                            context,
                            booking.getBookingId(),
                            parkingSpace.getName(),
                            booking.getEndTime()
                    );

                    success.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding booking", e);
                    success.setValue(false);
                });

        return success;
    }

    public MutableLiveData<Boolean> cancelBooking(String bookingId) {
        MutableLiveData<Boolean> success = new MutableLiveData<>();

        // Get the booking
        firestore.collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Booking booking = documentSnapshot.toObject(Booking.class);

                        if (booking != null) {
                            // Update booking status
                            booking.setBookingStatus("CANCELLED");

                            // Update in Firestore
                            firestore.collection("bookings")
                                    .document(bookingId)
                                    .update("bookingStatus", "CANCELLED")
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Booking cancelled in Firestore");

                                        // Update in Room database
                                        updateBooking(booking);

                                        // Restore available spot in parking space
                                        firestore.collection("parkingSpaces")
                                                .document(booking.getParkingSpaceId())
                                                .get()
                                                .addOnSuccessListener(parkingDoc -> {
                                                    if (parkingDoc.exists()) {
                                                        ParkingSpace parkingSpace = parkingDoc.toObject(ParkingSpace.class);
                                                        if (parkingSpace != null) {
                                                            int newAvailableSpots = parkingSpace.getAvailableSpots() + 1;

                                                            // Update in Firestore
                                                            firestore.collection("parkingSpaces")
                                                                    .document(booking.getParkingSpaceId())
                                                                    .update("availableSpots", newAvailableSpots)
                                                                    .addOnSuccessListener(aVoid2 -> {
                                                                        Log.d(TAG, "Parking space updated in Firestore, available spots: " + newAvailableSpots);
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.e(TAG, "Error updating parking space", e);
                                                                    });
                                                        }
                                                    }
                                                });

                                        success.setValue(true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error cancelling booking", e);
                                        success.setValue(false);
                                    });
                        } else {
                            Log.e(TAG, "Booking is null");
                            success.setValue(false);
                        }
                    } else {
                        Log.e(TAG, "Booking not found");
                        success.setValue(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting booking", e);
                    success.setValue(false);
                });

        return success;
    }

    public LiveData<Boolean> canCreateBooking(String userId, String parkingSpaceId, long startTime, long endTime) {
        MutableLiveData<Boolean> canBook = new MutableLiveData<>();

        // Check if the user has any overlapping bookings
        firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereIn("bookingStatus", Arrays.asList("RESERVED", "ACTIVE"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean hasOverlap = false;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking existingBooking = document.toObject(Booking.class);
                        if (existingBooking != null) {
                            // Check if the booking time overlaps
                            boolean overlaps = (startTime < existingBooking.getEndTime() &&
                                    endTime > existingBooking.getStartTime());

                            // If it's the same parking space or booking times overlap, can't book
                            if (existingBooking.getParkingSpaceId().equals(parkingSpaceId) || overlaps) {
                                hasOverlap = true;
                                break;
                            }
                        }
                    }

                    canBook.setValue(!hasOverlap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing bookings", e);
                    canBook.setValue(false);
                });

        return canBook;
    }

    // Helper method to create a booking object with a new ID
    public Booking createBookingObject(String userId, String parkingSpaceId, String vehicleId,
                                       long startTime, long endTime, double hourlyRate) {
        String bookingId = UUID.randomUUID().toString();
        double duration = (endTime - startTime) / (1000.0 * 60 * 60); // Duration in hours
        double totalAmount = hourlyRate * duration;

        return new Booking(
                bookingId,
                userId,
                parkingSpaceId,
                vehicleId,
                startTime,
                endTime,
                totalAmount
        );
    }

    // Helper method to generate mock bookings for testing
    public void generateMockBookings(String userId) {
        long now = System.currentTimeMillis();
        long oneHour = 60 * 60 * 1000;
        long oneDay = 24 * oneHour;

        // Past booking
        Booking pastBooking = createBookingObject(
                userId,
                "parking-college",
                "vehicle1",
                now - (2 * oneDay),
                now - (2 * oneDay) + (3 * oneHour),
                2.0
        );
        pastBooking.setBookingStatus("COMPLETED");
        pastBooking.setPaymentStatus("COMPLETED");

        // Active booking
        Booking activeBooking = createBookingObject(
                userId,
                "parking-mall",
                "vehicle1",
                now - oneHour,
                now + (2 * oneHour),
                3.5
        );
        activeBooking.setBookingStatus("ACTIVE");
        activeBooking.setPaymentStatus("COMPLETED");

        // Future booking
        Booking futureBooking = createBookingObject(
                userId,
                "parking-airport",
                "vehicle2",
                now + oneDay,
                now + oneDay + (4 * oneHour),
                5.0
        );

        // Add all bookings to Firestore
        firestore.collection("bookings").document(pastBooking.getBookingId()).set(pastBooking);
        firestore.collection("bookings").document(activeBooking.getBookingId()).set(activeBooking);
        firestore.collection("bookings").document(futureBooking.getBookingId()).set(futureBooking);

        // Add to Room database
        insertBooking(pastBooking);
        insertBooking(activeBooking);
        insertBooking(futureBooking);

        Log.d(TAG, "Generated mock bookings for user " + userId);
    }
}