// app/src/main/java/com/smartparking/utils/NotificationHelper.java
package com.smartparking.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

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
        // Ensure context is not null
        if (context == null) {
            Log.e("NotificationHelper", "Context is null, cannot show notification");
            return;
        }

        // Create an intent that opens the MainActivity and passes the booking ID
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("bookingId", bookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Booking Confirmed")
                .setContentText("Your booking at " + parkingName + " has been confirmed")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Use HIGH priority for immediate display
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            // Get notification manager and show the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Use a unique ID for each notification to avoid overwriting
            int notificationId = (bookingId + "_" + System.currentTimeMillis()).hashCode();

            // Check for permission (required for Android 13+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                    Log.d("NotificationHelper", "Booking confirmation notification shown");
                } else {
                    Log.e("NotificationHelper", "Notification permission not granted");
                }
            } else {
                // For older Android versions
                notificationManager.notify(notificationId, builder.build());
                Log.d("NotificationHelper", "Booking confirmation notification shown");
            }
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "Security exception showing notification", e);
        } catch (Exception e) {
            Log.e("NotificationHelper", "Error showing notification", e);
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
            try {
                // Check for SCHEDULE_EXACT_ALARM permission on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                        Log.d("NotificationHelper", "Scheduled exact reminder alarm for " + bookingId);
                    } else {
                        Log.e("NotificationHelper", "Cannot schedule exact alarms, permission not granted");
                        // Use setWindow as a fallback (less precise)
                        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, reminderTime, 60 * 1000, pendingIntent);
                        Log.d("NotificationHelper", "Scheduled window reminder alarm for " + bookingId);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                    Log.d("NotificationHelper", "Scheduled exact idle reminder alarm for " + bookingId);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                    Log.d("NotificationHelper", "Scheduled exact reminder alarm for " + bookingId);
                }
            } catch (SecurityException e) {
                Log.e("NotificationHelper", "Security exception scheduling reminder alarm", e);
                // Use inexact alarm as fallback
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                Log.d("NotificationHelper", "Scheduled inexact reminder alarm for " + bookingId);
            }
        } else {
            Log.e("NotificationHelper", "AlarmManager is null, cannot schedule reminder");
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
            try {
                // Check for SCHEDULE_EXACT_ALARM permission on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiryTime, pendingIntent);
                        Log.d("NotificationHelper", "Scheduled exact expiry alarm for " + bookingId);
                    } else {
                        Log.e("NotificationHelper", "Cannot schedule exact alarms, permission not granted");
                        // Use setWindow as a fallback (less precise)
                        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, expiryTime, 60 * 1000, pendingIntent);
                        Log.d("NotificationHelper", "Scheduled window expiry alarm for " + bookingId);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiryTime, pendingIntent);
                    Log.d("NotificationHelper", "Scheduled exact idle expiry alarm for " + bookingId);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, expiryTime, pendingIntent);
                    Log.d("NotificationHelper", "Scheduled exact expiry alarm for " + bookingId);
                }
            } catch (SecurityException e) {
                Log.e("NotificationHelper", "Security exception scheduling expiry alarm", e);
                // Use inexact alarm as fallback
                alarmManager.set(AlarmManager.RTC_WAKEUP, expiryTime, pendingIntent);
                Log.d("NotificationHelper", "Scheduled inexact expiry alarm for " + bookingId);
            }
        } else {
            Log.e("NotificationHelper", "AlarmManager is null, cannot schedule expiry");
        }
    }
}