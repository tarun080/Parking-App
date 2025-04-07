// app/src/main/java/com/smartparking/fragments/BookingsFragment.java
package com.smartparking.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.smartparking.R;
import com.smartparking.adapters.BookingAdapter;
import com.smartparking.models.Booking;
import com.smartparking.viewmodels.BookingViewModel;

import java.util.List;

public class BookingsFragment extends Fragment implements BookingAdapter.BookingClickListener {

    private static final String TAG = "BookingsFragment";

    private BookingViewModel bookingViewModel;
    private RecyclerView recyclerViewBookings;
    private TextView textViewNoBookings;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private BookingAdapter bookingAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "BookingsFragment view created");

        // Initialize views
        recyclerViewBookings = view.findViewById(R.id.recyclerViewBookings);
        textViewNoBookings = view.findViewById(R.id.textViewNoBookings);
        progressBar = view.findViewById(R.id.progressBar);
        tabLayout = view.findViewById(R.id.tabLayout);

        // Initialize ViewModel
        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);

        // Setup RecyclerView
        recyclerViewBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        bookingAdapter = new BookingAdapter(requireContext(), this);
        recyclerViewBookings.setAdapter(bookingAdapter);

        // Setup TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("Upcoming"));
        tabLayout.addTab(tabLayout.newTab().setText("Past"));

        boolean isDemo = false; // Set this based on your app's needs or a settings preference
        if (isDemo) {
            bookingViewModel.generateMockBookings();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadBookings(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });

        // Observe booking errors
        bookingViewModel.getBookingError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe booking progress
        bookingViewModel.getBookingInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        });

        // Load bookings for the selected tab
        loadBookings(tabLayout.getSelectedTabPosition());

        // Generate mock bookings for testing
        if (savedInstanceState == null) {
            bookingViewModel.generateMockBookings();
        }
    }

    private void loadBookings(int tabPosition) {
        Log.d(TAG, "Loading bookings for tab: " + tabPosition);

        if (tabPosition == 0) {
            // Upcoming bookings
            bookingViewModel.getActiveBookings().observe(getViewLifecycleOwner(), bookings -> {
                Log.d(TAG, "Active bookings: " + (bookings != null ? bookings.size() : 0));
                updateBookingsList(bookings);
            });
        } else {
            // Past bookings
            bookingViewModel.getPastBookings().observe(getViewLifecycleOwner(), bookings -> {
                Log.d(TAG, "Past bookings: " + (bookings != null ? bookings.size() : 0));
                updateBookingsList(bookings);
            });
        }
    }

    private void updateBookingsList(List<Booking> bookings) {
        if (bookings != null && !bookings.isEmpty()) {
            bookingAdapter.setBookings(bookings);
            recyclerViewBookings.setVisibility(View.VISIBLE);
            textViewNoBookings.setVisibility(View.GONE);
        } else {
            recyclerViewBookings.setVisibility(View.GONE);
            textViewNoBookings.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBookingClick(Booking booking) {
        Log.d(TAG, "Booking clicked: " + booking.getBookingId());

        // Show booking details
        BookingDetailsDialogFragment dialogFragment = BookingDetailsDialogFragment.newInstance(booking.getBookingId());
        dialogFragment.show(getParentFragmentManager(), "booking_details");
    }

    @Override
    public void onCancelBooking(Booking booking) {
        Log.d(TAG, "Cancel booking: " + booking.getBookingId());

        // Show confirmation dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Cancel Booking");
        builder.setMessage("Are you sure you want to cancel this booking?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            bookingViewModel.cancelBooking(booking.getBookingId());
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }
}