// app/src/main/java/com/smartparking/fragments/ParkingDetailsDialogFragment.java
package com.smartparking.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartparking.R;
import com.smartparking.models.ParkingSpace;

public class ParkingDetailsDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ParkingDetailsDialog";
    private static final String ARG_PARKING_ID = "parking_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_AVAILABLE_SPOTS = "availableSpots";
    private static final String ARG_TOTAL_SPOTS = "totalSpots";
    private static final String ARG_HOURLY_RATE = "hourlyRate";

    private String parkingId;
    private String name;
    private String address;
    private int availableSpots;
    private int totalSpots;
    private double hourlyRate;

    private TextView textViewName;
    private TextView textViewAddress;
    private TextView textViewAvailability;
    private TextView textViewRate;
    private Button buttonBook;
    private ImageView imageViewClose;
    private FirebaseFirestore firestore;

    public static ParkingDetailsDialogFragment newInstance(String parkingId) {
        ParkingDetailsDialogFragment fragment = new ParkingDetailsDialogFragment();
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
            name = getArguments().getString(ARG_NAME);
            address = getArguments().getString(ARG_ADDRESS);
            availableSpots = getArguments().getInt(ARG_AVAILABLE_SPOTS, 0);
            totalSpots = getArguments().getInt(ARG_TOTAL_SPOTS, 0);
            hourlyRate = getArguments().getDouble(ARG_HOURLY_RATE, 0.0);
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog);
        firestore = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parking_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "Dialog view created for parking space: " + parkingId);

        // Initialize views
        textViewName = view.findViewById(R.id.textViewName);
        textViewAddress = view.findViewById(R.id.textViewAddress);
        textViewAvailability = view.findViewById(R.id.textViewAvailability);
        textViewRate = view.findViewById(R.id.textViewRate);
        buttonBook = view.findViewById(R.id.buttonBook);
        imageViewClose = view.findViewById(R.id.imageViewClose);

        // If we have arguments with parking details, use those directly
        if (name != null && !name.isEmpty()) {
            updateUIFromArguments();
        } else {
            // Otherwise try to fetch from Firestore
            loadParkingDetails();
        }

        // Set click listeners
        buttonBook.setOnClickListener(v -> bookParking());
        imageViewClose.setOnClickListener(v -> dismiss());

    }

    private void updateUIFromArguments() {
        Log.d(TAG, "Updating UI from arguments");
        textViewName.setText(name);
        textViewAddress.setText(address);

        String availabilityText = availableSpots + " / " + totalSpots + " spots available";
        textViewAvailability.setText(availabilityText);

        String rateText = "$" + hourlyRate + " / hour";
        textViewRate.setText(rateText);

        // Disable book button if no spots available
        buttonBook.setEnabled(availableSpots > 0);
        if (availableSpots <= 0) {
            buttonBook.setText(R.string.no_spots_available);
        }
    }

    private void loadParkingDetails() {
        Log.d(TAG, "Loading parking details from Firestore for ID: " + parkingId);
        if (parkingId != null) {
            firestore.collection("parkingSpaces")
                    .document(parkingId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "Parking document exists in Firestore");
                            ParkingSpace parkingSpace = documentSnapshot.toObject(ParkingSpace.class);
                            if (parkingSpace != null) {
                                updateUI(parkingSpace);
                            } else {
                                Log.e(TAG, "Failed to convert document to ParkingSpace");
                                showErrorAndDismiss();
                            }
                        } else {
                            Log.d(TAG, "Parking document doesn't exist in Firestore");
                            showErrorAndDismiss();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading parking details", e);
                        showErrorAndDismiss();
                    });
        } else {
            Log.e(TAG, "ParkingId is null");
            showErrorAndDismiss();
        }
    }

    private void updateUI(ParkingSpace parkingSpace) {
        Log.d(TAG, "Updating UI from Firestore data");
        textViewName.setText(parkingSpace.getName());
        textViewAddress.setText(parkingSpace.getAddress());

        String availabilityText = parkingSpace.getAvailableSpots() + " / " + parkingSpace.getTotalSpots() + " spots available";
        textViewAvailability.setText(availabilityText);

        String rateText = "$" + parkingSpace.getHourlyRate() + " / hour";
        textViewRate.setText(rateText);

        // Disable book button if no spots available
        buttonBook.setEnabled(parkingSpace.getAvailableSpots() > 0);
        if (parkingSpace.getAvailableSpots() <= 0) {
            buttonBook.setText(R.string.no_spots_available);
        }
    }

    private void showErrorAndDismiss() {
        Toast.makeText(getContext(), "Parking space details not available", Toast.LENGTH_SHORT).show();
        dismiss();
    }



    private void bookParking() {
        Log.d(TAG, "Book parking: " + parkingId);

        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to book parking", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show create booking dialog
        CreateBookingDialogFragment dialogFragment = CreateBookingDialogFragment.newInstance(parkingId);
        dialogFragment.show(getParentFragmentManager(), "create_booking");

        // Close this dialog
        dismiss();
    }
}