// app/src/main/java/com/smartparking/utils/NotificationHelper.java
package com.smartparking.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.smartparking.R;
import com.smartparking.activities.MainActivity;
import com.smartparking.receivers.NotificationReceiver;

public class NotificationHelper {

    private static final String CHANNEL_ID = "parking_notifications";
    private static final String CHANNEL_NAME = "Parking Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications related to parking bookings";

    public static void createNotificationChannel(Context context) {
        // Create the notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void showBookingConfirmationNotification(Context context, String bookingId, String parkingName) {
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
                .setContentTitle("Booking Confirmed")
                .setContentText("Your booking at " + parkingName + " has been confirmed")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(1, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void scheduleBookingReminderNotification(Context context, String bookingId, String parkingName, long startTime) {
        // Schedule a notification 15 minutes before the booking start time
        long reminderTime = startTime - (15 * 60 * 1000); // 15 minutes in milliseconds

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notificationType", "reminder");
        intent.putExtra("bookingId", bookingId);
        intent.putExtra("parkingName", parkingName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                bookingId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            }
        }
    }

    public static void scheduleBookingExpiryNotification(Context context, String bookingId, String parkingName, long endTime) {
        // Schedule a notification 15 minutes before the booking end time
        long expiryTime = endTime - (15 * 60 * 1000); // 15 minutes in milliseconds

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notificationType", "expiry");
        intent.putExtra("bookingId", bookingId);
        intent.putExtra("parkingName", parkingName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (bookingId + "_expiry").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiryTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, expiryTime, pendingIntent);
            }
        }
    }
}