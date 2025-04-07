// app/src/main/java/com/smartparking/fragments/CreateBookingDialogFragment.java
package com.smartparking.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartparking.R;
import com.smartparking.models.ParkingSpace;
import com.smartparking.models.Vehicle;
import com.smartparking.repositories.BookingRepository;
import com.smartparking.utils.NotificationHelper;
import com.smartparking.viewmodels.BookingViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateBookingDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "CreateBookingDialog";
    private static final String ARG_PARKING_ID = "parking_id";

    private String parkingId;
    private ParkingSpace parkingSpace;
    private BookingViewModel bookingViewModel;
    private FirebaseFirestore firestore;
    private String userId;

    private TextView textViewParkingName;
    private TextView textViewAddress;
    private TextView textViewRate;
    private TextView textViewStartTime;
    private TextView textViewEndTime;
    private SeekBar seekBarDuration;
    private TextView textViewDuration;
    private Spinner spinnerVehicle;
    private TextView textViewTotalAmount;
    private Button buttonBook;

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private List<Vehicle> userVehicles = new ArrayList<>();
    private List<String> vehicleDisplayList = new ArrayList<>();
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private int durationHours = 2; // Default 2 hours

    // Reference to activity context
    private Context activityContext;

    // Flag to track if we're using mock data
    private boolean usingMockData = false;

    public static CreateBookingDialogFragment newInstance(String parkingId) {
        CreateBookingDialogFragment fragment = new CreateBookingDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARKING_ID, parkingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Store activity context
        activityContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            parkingId = getArguments().getString(ARG_PARKING_ID);
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog);
        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Set initial times (start: now, end: +2 hours)
        endCalendar.add(Calendar.HOUR_OF_DAY, durationHours);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "Dialog view created for parking space: " + parkingId);

        // Initialize views
        textViewParkingName = view.findViewById(R.id.textViewParkingName);
        textViewAddress = view.findViewById(R.id.textViewAddress);
        textViewRate = view.findViewById(R.id.textViewRate);
        textViewStartTime = view.findViewById(R.id.textViewStartTime);
        textViewEndTime = view.findViewById(R.id.textViewEndTime);
        seekBarDuration = view.findViewById(R.id.seekBarDuration);
        textViewDuration = view.findViewById(R.id.textViewDuration);
        spinnerVehicle = view.findViewById(R.id.spinnerVehicle);
        textViewTotalAmount = view.findViewById(R.id.textViewTotalAmount);
        buttonBook = view.findViewById(R.id.buttonBook);

        // Initialize ViewModel
        bookingViewModel = new ViewModelProvider(requireActivity()).get(BookingViewModel.class);

        // Set initial values
        seekBarDuration.setProgress(durationHours);
        updateDurationText();
        updateTimeDisplay();

        // Set up duration seek bar listener
        seekBarDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    seekBar.setProgress(1);
                    durationHours = 1;
                } else {
                    durationHours = progress;
                }
                updateDurationText();
                updateEndTime();
                updateTotalAmount();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });

        // Set click listeners
        textViewStartTime.setOnClickListener(v -> showDateTimePicker(true));
        textViewEndTime.setOnClickListener(v -> showDateTimePicker(false));
        buttonBook.setOnClickListener(v -> createBooking());

        // Check if we have mock data in arguments
        if (getArguments() != null
                && getArguments().getString("name") != null
                && !getArguments().getString("name").isEmpty()) {
            // Create parking space from arguments
            createParkingSpaceFromArguments();
        } else {
            // Load parking space details from Firestore
            loadParkingSpaceDetails();
        }

        // Load user vehicles
        loadUserVehicles();

        // Observe booking in progress
        bookingViewModel.getBookingInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            buttonBook.setEnabled(!inProgress);
            buttonBook.setText(inProgress ? "Processing..." : "Book Now");
        });

        // Observe booking errors
        bookingViewModel.getBookingError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && isAdded()) {
                safeShowToast(error);
            }
        });
    }

    private void createParkingSpaceFromArguments() {
        Bundle args = getArguments();
        if (args != null) {
            String name = args.getString("name", "");
            String address = args.getString("address", "");
            int availableSpots = args.getInt("availableSpots", 0);
            int totalSpots = args.getInt("totalSpots", 0);
            double hourlyRate = args.getDouble("hourlyRate", 0.0);
            double latitude = args.getDouble("latitude", 0.0);
            double longitude = args.getDouble("longitude", 0.0);

            // Create a mock parking space
            parkingSpace = new ParkingSpace(
                    parkingId,
                    name,
                    address,
                    latitude,
                    longitude,
                    totalSpots,
                    hourlyRate,
                    "mock_owner"
            );
            parkingSpace.setAvailableSpots(availableSpots);

            // Update UI
            textViewParkingName.setText(name);
            textViewAddress.setText(address);
            String rateStr = "$" + String.format(Locale.getDefault(), "%.2f", hourlyRate) + " / hour";
            textViewRate.setText(rateStr);

            updateTotalAmount();

            // Set flag for mock data
            usingMockData = true;

            Log.d(TAG, "Created parking space from arguments: " + name);
        }
    }

    private void loadParkingSpaceDetails() {
        Log.d(TAG, "Loading parking space details for ID: " + parkingId);

        if (parkingId == null || parkingId.isEmpty()) {
            Log.e(TAG, "Parking ID is null or empty");
            safeShowToast("Invalid parking space ID");
            dismiss();
            return;
        }

        firestore.collection("parkingSpaces")
                .document(parkingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded()) {
                        if (documentSnapshot.exists()) {
                            parkingSpace = documentSnapshot.toObject(ParkingSpace.class);
                            if (parkingSpace != null) {
                                textViewParkingName.setText(parkingSpace.getName());
                                textViewAddress.setText(parkingSpace.getAddress());

                                String rateStr = "$" + String.format(Locale.getDefault(), "%.2f", parkingSpace.getHourlyRate()) + " / hour";
                                textViewRate.setText(rateStr);

                                updateTotalAmount();
                            }
                        } else {
                            Log.e(TAG, "Parking space document doesn't exist");
                            safeShowToast("Parking space not found");
                            dismiss();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Log.e(TAG, "Error loading parking space details", e);
                        safeShowToast("Error loading parking space details");
                        dismiss();
                    }
                });
    }

    private void loadUserVehicles() {
        if (userId.isEmpty()) {
            safeShowToast("User not logged in");
            dismiss();
            return;
        }

        firestore.collection("vehicles")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isAdded()) {
                        userVehicles.clear();
                        vehicleDisplayList.clear();

                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Vehicle vehicle = document.toObject(Vehicle.class);
                            if (vehicle != null) {
                                userVehicles.add(vehicle);
                                vehicleDisplayList.add(vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getLicensePlate() + ")");
                            }
                        }

                        // If no vehicles, add a default option
                        if (userVehicles.isEmpty()) {
                            // Add a dummy vehicle for testing
                            Vehicle dummyVehicle = new Vehicle(
                                    "vehicle1",
                                    userId,
                                    "MH01AB1234",
                                    "Toyota",
                                    "Corolla",
                                    "White",
                                    "Sedan"
                            );
                            userVehicles.add(dummyVehicle);
                            vehicleDisplayList.add(dummyVehicle.getMake() + " " + dummyVehicle.getModel() + " (" + dummyVehicle.getLicensePlate() + ")");
                        }

                        // Setup spinner
                        if (getContext() != null) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    getContext(),
                                    android.R.layout.simple_spinner_item,
                                    vehicleDisplayList
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerVehicle.setAdapter(adapter);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user vehicles", e);

                    if (isAdded()) {
                        // Add a dummy vehicle for testing
                        Vehicle dummyVehicle = new Vehicle(
                                "vehicle1",
                                userId,
                                "MH01AB1234",
                                "Toyota",
                                "Corolla",
                                "White",
                                "Sedan"
                        );
                        userVehicles.add(dummyVehicle);
                        vehicleDisplayList.add(dummyVehicle.getMake() + " " + dummyVehicle.getModel() + " (" + dummyVehicle.getLicensePlate() + ")");

                        // Setup spinner
                        if (getContext() != null) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    getContext(),
                                    android.R.layout.simple_spinner_item,
                                    vehicleDisplayList
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerVehicle.setAdapter(adapter);
                        }
                    }
                });
    }

    private void showDateTimePicker(boolean isStartTime) {
        final Calendar calendar = isStartTime ? startCalendar : endCalendar;

        if (isAdded() && getContext() != null) {
            // Show date picker
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        if (isAdded() && getContext() != null) {
                            // Show time picker
                            TimePickerDialog timePickerDialog = new TimePickerDialog(
                                    getContext(),
                                    (view1, hourOfDay, minute) -> {
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        calendar.set(Calendar.MINUTE, minute);

                                        // If start time changed, update end time based on duration
                                        if (isStartTime) {
                                            updateEndTime();
                                        } else {
                                            // If end time was directly set, calculate new duration
                                            long diffMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
                                            durationHours = Math.max(1, (int) (diffMillis / (1000 * 60 * 60)));
                                            seekBarDuration.setProgress(durationHours);
                                            updateDurationText();
                                        }

                                        updateTimeDisplay();
                                        updateTotalAmount();
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                            );
                            timePickerDialog.show();
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        }
    }

    private void updateDurationText() {
        textViewDuration.setText(durationHours + " " + (durationHours == 1 ? "hour" : "hours"));
    }

    private void updateEndTime() {
        // Set end time based on start time + duration
        endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
        endCalendar.add(Calendar.HOUR_OF_DAY, durationHours);
        updateTimeDisplay();
    }

    private void updateTimeDisplay() {
        textViewStartTime.setText(dateTimeFormat.format(startCalendar.getTime()));
        textViewEndTime.setText(dateTimeFormat.format(endCalendar.getTime()));
    }

    private void updateTotalAmount() {
        if (parkingSpace != null) {
            // Calculate duration in hours
            long durationMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
            double durationHours = durationMillis / (1000.0 * 60 * 60);

            // Calculate total amount
            double totalAmount = parkingSpace.getHourlyRate() * durationHours;

            // Display total amount
            String totalAmountStr = "$" + String.format(Locale.getDefault(), "%.2f", totalAmount);
            textViewTotalAmount.setText(totalAmountStr);
        }
    }

    // In CreateBookingDialogFragment.java, update the createBooking method

    private void createBooking() {
        if (parkingSpace == null) {
            safeShowToast("Parking space details not available");
            return;
        }

        if (userVehicles.isEmpty() || spinnerVehicle.getSelectedItemPosition() < 0) {
            safeShowToast("Please select a vehicle");
            return;
        }

        // Show progress
        buttonBook.setEnabled(false);
        buttonBook.setText("Checking availability...");

        // Get selected vehicle
        Vehicle selectedVehicle = userVehicles.get(spinnerVehicle.getSelectedItemPosition());

        if (usingMockData) {
            // For mock data, directly create booking
            Log.d(TAG, "Creating booking for mock parking space: " + parkingSpace.getName());

            // Calculate booking amount
            long durationMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
            double durationHours = durationMillis / (1000.0 * 60 * 60);
            double amount = parkingSpace.getHourlyRate() * durationHours;

            // Create booking in view model
            bookingViewModel.createBooking(
                    parkingSpace.getSpaceId(),
                    selectedVehicle.getVehicleId(),
                    startCalendar.getTimeInMillis(),
                    endCalendar.getTimeInMillis()
            );

            // Manually show a notification since we're using mock data
            NotificationHelper.showBookingConfirmationNotification(
                    requireContext(),
                    "mock-booking-" + System.currentTimeMillis(),
                    parkingSpace.getName()
            );

            // Show success message and close dialog
            safeShowToast("Booking created successfully");
            dismiss();
        } else {
            // For real data, check if user already has a booking at this time
            BookingRepository bookingRepo = new BookingRepository(requireActivity().getApplication());
            bookingRepo.canCreateBooking(
                    userId,
                    parkingSpace.getSpaceId(),
                    startCalendar.getTimeInMillis(),
                    endCalendar.getTimeInMillis()
            ).observe(getViewLifecycleOwner(), canBook -> {
                if (canBook) {
                    // Create booking
                    bookingViewModel.createBooking(
                            parkingSpace.getSpaceId(),
                            selectedVehicle.getVehicleId(),
                            startCalendar.getTimeInMillis(),
                            endCalendar.getTimeInMillis()
                    );

                    // Manually show success message
                    safeShowToast("Booking created successfully");

                    // Close dialog on success
                    bookingViewModel.getBookingInProgress().observe(getViewLifecycleOwner(), inProgress -> {
                        if (!inProgress) {
                            dismiss();
                        }
                    });
                } else {
                    buttonBook.setEnabled(true);
                    buttonBook.setText(R.string.book_now);
                    safeShowToast("You already have an overlapping booking. Please choose a different time.");
                }
            });
        }
    }

    // Safe method to show a toast that checks if fragment is attached
    private void safeShowToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        } else if (activityContext != null) {
            // Use stored activity context if fragment context is not available
            Toast.makeText(activityContext, message, Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "Cannot show toast: " + message);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear the activity context reference
        activityContext = null;
    }
}