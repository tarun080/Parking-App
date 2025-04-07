// app/src/main/java/com/smartparking/ParkingApp.java
package com.smartparking;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.smartparking.utils.NotificationHelper;

public class ParkingApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        NotificationHelper.createNotificationChannel(this);
    }
}