package com.smartparking.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.smartparking.R;
import com.smartparking.activities.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "parking_notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        String notificationType = intent.getStringExtra("notificationType");
        String bookingId = intent.getStringExtra("bookingId");
        String parkingName = intent.getStringExtra("parkingName");

        if (notificationType != null && bookingId != null && parkingName != null) {
            if (notificationType.equals("reminder")) {
                showBookingReminderNotification(context, bookingId, parkingName);
            } else if (notificationType.equals("expiry")) {
                showBookingExpiryNotification(context, bookingId, parkingName);
            }
        }
    }

    private void showBookingReminderNotification(Context context, String bookingId, String parkingName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("bookingId", bookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Booking Reminder")
                .setContentText("Your booking at " + parkingName + " starts in 15 minutes")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(bookingId.hashCode(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void showBookingExpiryNotification(Context context, String bookingId, String parkingName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("bookingId", bookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Booking Expiring Soon")
                .setContentText("Your booking at " + parkingName + " expires in 15 minutes")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify((bookingId + "_expiry").hashCode(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}