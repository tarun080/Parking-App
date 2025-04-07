// app/src/main/java/com/smartparking/fragments/BookingDetailsDialogFragment.java
package com.smartparking.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smartparking.R;
import com.smartparking.models.Booking;
import com.smartparking.models.ParkingSpace;
import com.smartparking.viewmodels.BookingViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingDetailsDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "BookingDetailsDialog";
    private static final String ARG_BOOKING_ID = "booking_id";

    private BookingViewModel bookingViewModel;
    private String bookingId;
    private FirebaseFirestore firestore;

    private TextView textViewParkingName;
    private TextView textViewAddress;
    private TextView textViewTime;
    private TextView textViewDuration;
    private TextView textViewStatus;
    private TextView textViewAmount;
    private TextView textViewPaymentStatus;
    private ImageView imageViewQRCode;
    private Button buttonAction;
    private ImageView imageViewClose;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public static BookingDetailsDialogFragment newInstance(String bookingId) {
        BookingDetailsDialogFragment fragment = new BookingDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            bookingId = getArguments().getString(ARG_BOOKING_ID);
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog);
        firestore = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "Dialog view created for booking: " + bookingId);

        // Initialize views
        textViewParkingName = view.findViewById(R.id.textViewParkingName);
        textViewAddress = view.findViewById(R.id.textViewAddress);
        textViewTime = view.findViewById(R.id.textViewTime);
        textViewDuration = view.findViewById(R.id.textViewDuration);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        textViewAmount = view.findViewById(R.id.textViewAmount);
        textViewPaymentStatus = view.findViewById(R.id.textViewPaymentStatus);
        imageViewQRCode = view.findViewById(R.id.imageViewQRCode);
        buttonAction = view.findViewById(R.id.buttonAction);
        imageViewClose = view.findViewById(R.id.imageViewClose);

        // Initialize ViewModel
        bookingViewModel = new ViewModelProvider(requireActivity()).get(BookingViewModel.class);

        // Load booking details
        loadBookingDetails();

        // Set click listeners
        imageViewClose.setOnClickListener(v -> dismiss());
    }

    private void loadBookingDetails() {
        Log.d(TAG, "Loading booking details for ID: " + bookingId);

        if (bookingId != null) {
            bookingViewModel.getBookingById(bookingId).observe(getViewLifecycleOwner(), booking -> {
                if (booking != null) {
                    Log.d(TAG, "Booking loaded: " + booking.getBookingId());
                    updateUI(booking);

                    // Load parking space details
                    loadParkingSpaceDetails(booking.getParkingSpaceId());
                } else {
                    Log.e(TAG, "Booking not found");
                    showErrorAndDismiss();
                }
            });
        } else {
            Log.e(TAG, "Booking ID is null");
            showErrorAndDismiss();
        }
    }

    private void loadParkingSpaceDetails(String parkingSpaceId) {
        Log.d(TAG, "Loading parking space details for ID: " + parkingSpaceId);

        firestore.collection("parkingSpaces")
                .document(parkingSpaceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ParkingSpace parkingSpace = documentSnapshot.toObject(ParkingSpace.class);
                        if (parkingSpace != null) {
                            textViewParkingName.setText(parkingSpace.getName());
                            textViewAddress.setText(parkingSpace.getAddress());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading parking space details", e));
    }

    private void updateUI(Booking booking) {
        // Time
        String timeStr = dateFormat.format(new Date(booking.getStartTime())) +
                " - " +
                dateFormat.format(new Date(booking.getEndTime()));
        textViewTime.setText(timeStr);

        // Duration
        String durationStr = String.format(Locale.getDefault(), "%.1f hours", booking.getDurationHours());
        textViewDuration.setText(durationStr);

        // Status
        textViewStatus.setText(booking.getBookingStatus());

        // Status color
        int colorRes;
        switch (booking.getBookingStatus()) {
            case "ACTIVE":
                colorRes = R.color.colorAvailable;
                break;
            case "COMPLETED":
                colorRes = R.color.colorTextLight;
                break;
            case "CANCELLED":
                colorRes = R.color.colorUnavailable;
                break;
            default: // RESERVED
                colorRes = R.color.colorAccent;
                break;
        }
        textViewStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes));

        // Amount
        String amountStr = "$" + String.format(Locale.getDefault(), "%.2f", booking.getTotalAmount());
        textViewAmount.setText(amountStr);

        // Payment status
        textViewPaymentStatus.setText(booking.getPaymentStatus());

        // Generate QR Code if booking is active or reserved
        if (booking.getBookingStatus().equals("ACTIVE") || booking.getBookingStatus().equals("RESERVED")) {
            generateQRCode(booking);
        } else {
            imageViewQRCode.setVisibility(View.GONE);
        }

        // Action button
        if (booking.getBookingStatus().equals("RESERVED") || booking.getBookingStatus().equals("ACTIVE")) {
            buttonAction.setVisibility(View.VISIBLE);
            buttonAction.setText("Cancel Booking");
            buttonAction.setOnClickListener(v -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                builder.setTitle("Cancel Booking");
                builder.setMessage("Are you sure you want to cancel this booking?");
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    bookingViewModel.cancelBooking(booking.getBookingId());
                    dismiss();
                });
                builder.setNegativeButton("No", null);
                builder.show();
            });
        } else {
            buttonAction.setVisibility(View.GONE);
        }
    }

    private void generateQRCode(Booking booking) {
        try {
            // Create QR code content with booking details
            String qrContent = "BOOKING:" + booking.getBookingId() +
                    "|SPACE:" + booking.getParkingSpaceId() +
                    "|USER:" + booking.getUserId() +
                    "|START:" + booking.getStartTime() +
                    "|END:" + booking.getEndTime();

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            imageViewQRCode.setImageBitmap(bitmap);
            imageViewQRCode.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            imageViewQRCode.setVisibility(View.GONE);
        }
    }

    private void showErrorAndDismiss() {
        Toast.makeText(getContext(), "Booking details not available", Toast.LENGTH_SHORT).show();
        dismiss();
    }
}