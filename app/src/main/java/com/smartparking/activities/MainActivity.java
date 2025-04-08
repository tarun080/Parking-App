package com.smartparking.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartparking.R;
import com.smartparking.fragments.BookingDetailsDialogFragment;
import com.smartparking.fragments.MapFragment;
import com.smartparking.models.User;
import com.smartparking.viewmodels.AuthViewModel;
import com.smartparking.viewmodels.BookingViewModel;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private AuthViewModel authViewModel;
    private BookingViewModel bookingViewModel;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    private void requestRequiredPermissions() {
        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }

        // Request alarm scheduling permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.e("MainActivity", "Cannot open alarm settings");
                    Toast.makeText(this, "Please grant permission to schedule alarms in system settings",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request notification permission
        requestNotificationPermission();
        requestRequiredPermissions();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Setup drawer navigation
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup navigation controller
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_map, R.id.nav_bookings, R.id.nav_profile)
                .setDrawerLayout(drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize ViewModels
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);

        // Set observers
        authViewModel.getUserData().observe(this, this::updateNavigationHeader);

        // Setup booking observers to refresh map when bookings change
        setupBookingObservers();

        // Handle notification deep links with a slight delay
        new Handler().postDelayed(this::handleNotificationDeepLink, 500);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }
    }

    private void handleNotificationDeepLink() {
        // Check if activity was started from a notification
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("bookingId")) {
            String bookingId = intent.getStringExtra("bookingId");
            String notificationType = intent.getStringExtra("notification_type");

            if (bookingId != null && !bookingId.isEmpty()) {
                Log.d("MainActivity", "Showing booking details for: " + bookingId);
                // Show booking details
                BookingDetailsDialogFragment dialogFragment =
                        BookingDetailsDialogFragment.newInstance(bookingId);
                dialogFragment.show(getSupportFragmentManager(), "booking_details");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Add a slight delay to ensure activity is fully ready
        new Handler().postDelayed(this::handleNotificationDeepLink, 300);
    }

    private void setupBookingObservers() {
        // Observe booking in progress
        bookingViewModel.getBookingInProgress().observe(this, inProgress -> {
            if (!inProgress) {
                // When booking process finishes, refresh map data if that fragment is visible
                MapFragment mapFragment = (MapFragment) getSupportFragmentManager()
                        .findFragmentByTag("nav_map");

                if (mapFragment != null && mapFragment.isVisible()) {
                    mapFragment.refreshMapData();
                }
            }
        });
    }

    private void updateNavigationHeader(User user) {
        if (user != null) {
            // Update navigation header with user information
            View headerView = navigationView.getHeaderView(0);
            TextView textViewName = headerView.findViewById(R.id.textViewName);
            TextView textViewEmail = headerView.findViewById(R.id.textViewEmail);

            textViewName.setText(user.getName());
            textViewEmail.setText(user.getEmail());
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // If User object is null but user is authenticated, fetch from Firestore
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User userData = documentSnapshot.toObject(User.class);
                            if (userData != null) {
                                View headerView = navigationView.getHeaderView(0);
                                TextView textViewName = headerView.findViewById(R.id.textViewName);
                                TextView textViewEmail = headerView.findViewById(R.id.textViewEmail);

                                textViewName.setText(userData.getName());
                                textViewEmail.setText(userData.getEmail());
                            }
                        }
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            navController.navigate(R.id.nav_map);
        } else if (id == R.id.nav_bookings) {
            navController.navigate(R.id.nav_bookings);
        } else if (id == R.id.nav_profile) {
            navController.navigate(R.id.nav_profile);
        } else if (id == R.id.nav_settings) {
            navController.navigate(R.id.nav_settings);
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to login screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Helper method to refresh map after a booking is made or canceled
    public void refreshMap() {
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager()
                .findFragmentByTag("nav_map");

        if (mapFragment != null) {
            mapFragment.refreshMapData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            // Handle notification permission result
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d("MainActivity", "Notification permission granted");
            } else {
                Log.e("MainActivity", "Notification permission denied");
                Toast.makeText(this, "Notification permission required for booking alerts", Toast.LENGTH_LONG).show();
            }
        }
    }
}