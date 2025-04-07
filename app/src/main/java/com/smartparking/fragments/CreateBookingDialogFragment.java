// app/src/main/java/com/smartparking/fragments/CreateBookingDialogFragment.java
package com.smartparking.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

    public static CreateBookingDialogFragment newInstance(String parkingId) {
        CreateBookingDialogFragment fragment = new CreateBookingDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARKING_ID, parkingId);
        fragment.setArguments(args);
        return fragment;
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

        // Load parking space details
        loadParkingSpaceDetails();

        // Load user vehicles
        loadUserVehicles();

        // Observe booking in progress
        bookingViewModel.getBookingInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            buttonBook.setEnabled(!inProgress);
            buttonBook.setText(inProgress ? "Processing..." : "Book Now");
        });

        // Observe booking errors
        bookingViewModel.getBookingError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadParkingSpaceDetails() {
        Log.d(TAG, "Loading parking space details for ID: " + parkingId);

        if (parkingId == null || parkingId.isEmpty()) {
            Log.e(TAG, "Parking ID is null or empty");
            Toast.makeText(getContext(), "Invalid parking space ID", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        firestore.collection("parkingSpaces")
                .document(parkingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        parkingSpace = documentSnapshot.toObject(ParkingSpace.class);
                        if (parkingSpace != null) {
                            textViewParkingName.setText(parkingSpace.getName());
                            textViewAddress.setText(parkingSpace.getAddress());

                            String rateStr = "$" + String.format(Locale.getDefault(), "%.2f", parkingSpace.getHourlyRate()) + " / hour";
                            textViewRate.setText(rateStr);

                            updateTotalAmount();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading parking space details", e);
                    Toast.makeText(getContext(), "Error loading parking space details", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private void loadUserVehicles() {
        if (userId.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        firestore.collection("vehicles")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            vehicleDisplayList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerVehicle.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user vehicles", e);

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
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            vehicleDisplayList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerVehicle.setAdapter(adapter);
                });
    }

    private void showDateTimePicker(boolean isStartTime) {
        final Calendar calendar = isStartTime ? startCalendar : endCalendar;

        // Show date picker
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Show time picker
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
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
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
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

    private void createBooking() {
        if (parkingSpace == null) {
            Toast.makeText(getContext(), "Parking space details not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userVehicles.isEmpty() || spinnerVehicle.getSelectedItemPosition() < 0) {
            Toast.makeText(getContext(), "Please select a vehicle", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        buttonBook.setEnabled(false);
        buttonBook.setText("Checking availability...");

        // Get selected vehicle
        Vehicle selectedVehicle = userVehicles.get(spinnerVehicle.getSelectedItemPosition());

        // Check if user already has a booking at this time
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

                // Close dialog on success
                bookingViewModel.getBookingInProgress().observe(getViewLifecycleOwner(), inProgress -> {
                    if (!inProgress) {
                        dismiss();
                    }
                });
            } else {
                buttonBook.setEnabled(true);
                buttonBook.setText(R.string.book_now);
                Toast.makeText(getContext(), "You already have an overlapping booking. Please choose a different time.", Toast.LENGTH_LONG).show();
            }
        });
    }
}