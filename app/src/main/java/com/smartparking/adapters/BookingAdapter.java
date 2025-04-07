// app/src/main/java/com/smartparking/adapters/BookingAdapter.java
package com.smartparking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smartparking.R;
import com.smartparking.models.Booking;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private final Context context;
    private final BookingClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public interface BookingClickListener {
        void onBookingClick(Booking booking);
        void onCancelBooking(Booking booking);
    }

    public BookingAdapter(Context context, BookingClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        if (bookings != null) {
            Booking booking = bookings.get(position);
            holder.bind(booking);
        }
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewParkingName;
        private final TextView textViewTime;
        private final TextView textViewStatus;
        private final TextView textViewAmount;
        private final Button buttonCancel;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewParkingName = itemView.findViewById(R.id.textViewParkingName);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            textViewAmount = itemView.findViewById(R.id.textViewAmount);
            buttonCancel = itemView.findViewById(R.id.buttonCancel);
        }

        public void bind(Booking booking) {
            // Set parking name (this would normally come from a join with ParkingSpace)
            textViewParkingName.setText(booking.getParkingSpaceId());

            // Set booking time
            String timeStr = dateFormat.format(new Date(booking.getStartTime())) +
                    " - " +
                    dateFormat.format(new Date(booking.getEndTime()));
            textViewTime.setText(timeStr);

            // Set booking status
            textViewStatus.setText(booking.getBookingStatus());

            // Set color based on status
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
            textViewStatus.setTextColor(ContextCompat.getColor(context, colorRes));

            // Set amount
            String amountStr = "$" + String.format(Locale.getDefault(), "%.2f", booking.getTotalAmount());
            textViewAmount.setText(amountStr);

            // Show cancel button for active or reserved bookings
            if (booking.getBookingStatus().equals("ACTIVE") || booking.getBookingStatus().equals("RESERVED")) {
                buttonCancel.setVisibility(View.VISIBLE);
                buttonCancel.setOnClickListener(v -> listener.onCancelBooking(booking));
            } else {
                buttonCancel.setVisibility(View.GONE);
            }

            // Set click listener for the whole item
            itemView.setOnClickListener(v -> listener.onBookingClick(booking));
        }
    }
}