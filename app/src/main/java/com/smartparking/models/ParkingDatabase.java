// app/src/main/java/com/smartparking/models/ParkingDatabase.java
package com.smartparking.models;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, Vehicle.class, ParkingSpace.class, Booking.class}, version = 3, exportSchema = false)
public abstract class ParkingDatabase extends RoomDatabase {

    private static ParkingDatabase instance;

    public abstract UserDao userDao();
    public abstract VehicleDao vehicleDao();
    public abstract ParkingSpaceDao parkingSpaceDao();
    public abstract BookingDao bookingDao();


    public static synchronized ParkingDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ParkingDatabase.class,
                            "parking_database")
                    .fallbackToDestructiveMigration() // This will delete and recreate the database
                    .build();
        }
        return instance;
    }
}