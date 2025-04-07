// app/src/main/java/com/smartparking/activities/SplashActivity.java
package com.smartparking.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.smartparking.R;
import com.smartparking.viewmodels.AuthViewModel;

import java.util.HashMap;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 2 seconds
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("test")
                .document("test")
                .set(new HashMap<String, Object>() {{
                    put("test", "test");
                }})
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Connection successful"))
                .addOnFailureListener(e -> Log.e("Firebase", "Connection failed", e));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is already logged in
            if (authViewModel.getUserLiveData().getValue() != null) {
                // User is logged in, go to MainActivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                // User is not logged in, go to LoginActivity
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DELAY);
    }
}