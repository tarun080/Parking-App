package com.smartparking.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartparking.R;
import com.smartparking.adapters.VehicleAdapter;
import com.smartparking.models.User;
import com.smartparking.models.Vehicle;
import com.smartparking.viewmodels.AuthViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView textViewName;
    private TextView textViewEmail;
    private TextView textViewPhone;
    private TextView textViewEditProfile;
    private Button buttonAddVehicle;
    private RecyclerView recyclerViewVehicles;
    private TextView textViewNoVehicles;

    private AuthViewModel authViewModel;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private VehicleAdapter vehicleAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "ProfileFragment view created");

        // Initialize views
        textViewName = view.findViewById(R.id.textViewName);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        textViewPhone = view.findViewById(R.id.textViewPhone);
        textViewEditProfile = view.findViewById(R.id.textViewEditProfile);
        buttonAddVehicle = view.findViewById(R.id.buttonAddVehicle);
        recyclerViewVehicles = view.findViewById(R.id.recyclerViewVehicles);
        textViewNoVehicles = view.findViewById(R.id.textViewNoVehicles);

        // Initialize Firebase components
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Set up RecyclerView for vehicles
        recyclerViewVehicles.setLayoutManager(new LinearLayoutManager(getContext()));
        vehicleAdapter = new VehicleAdapter(getContext());
        recyclerViewVehicles.setAdapter(vehicleAdapter);

        // Default values for text views
        if (currentUser != null) {
            textViewEmail.setText(currentUser.getEmail());
        } else {
            Log.e(TAG, "Current user is null");
        }

        // Load user data
        loadUserData();

        // Load user's vehicles
        loadUserVehicles();

        // Set click listeners
        textViewEditProfile.setOnClickListener(v -> {
            // TODO: Implement edit profile functionality
            Toast.makeText(getContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show();
        });

        buttonAddVehicle.setOnClickListener(v -> {
            AddVehicleDialogFragment dialogFragment = new AddVehicleDialogFragment();
            dialogFragment.show(getChildFragmentManager(), "add_vehicle");
        });

        Log.d(TAG, "ProfileFragment initialized");
    }

    private void loadUserData() {
        if (currentUser != null) {
            Log.d(TAG, "Loading user data for UID: " + currentUser.getUid());

            // First set the email from FirebaseAuth
            textViewEmail.setText(currentUser.getEmail());

            // Get additional user data from Firestore
            firestore.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "User document exists in Firestore");

                            String name = documentSnapshot.getString("name");
                            String phone = documentSnapshot.getString("phoneNumber");

                            if (name != null && !name.isEmpty()) {
                                textViewName.setText(name);
                                Log.d(TAG, "Name set: " + name);
                            } else {
                                textViewName.setText("Name not available");
                                Log.d(TAG, "Name not available in Firestore");
                            }

                            if (phone != null && !phone.isEmpty()) {
                                textViewPhone.setText(phone);
                                Log.d(TAG, "Phone set: " + phone);
                            } else {
                                textViewPhone.setText("Phone not available");
                                Log.d(TAG, "Phone not available in Firestore");
                            }
                        } else {
                            Log.d(TAG, "User document doesn't exist in Firestore");
                            textViewName.setText("Profile not found");
                            textViewPhone.setText("Phone not available");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load user data: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        textViewName.setText("Error loading profile");
                        textViewPhone.setText("Error loading phone");
                    });
        } else {
            Log.e(TAG, "Cannot load user data, user not logged in");
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserVehicles() {
        if (currentUser != null) {
            firestore.collection("vehicles")
                    .whereEqualTo("userId", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Vehicle> vehicles = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Vehicle vehicle = document.toObject(Vehicle.class);
                            if (vehicle != null) {
                                vehicles.add(vehicle);
                            }
                        }

                        if (vehicles.isEmpty()) {
                            textViewNoVehicles.setVisibility(View.VISIBLE);
                            recyclerViewVehicles.setVisibility(View.GONE);
                        } else {
                            textViewNoVehicles.setVisibility(View.GONE);
                            recyclerViewVehicles.setVisibility(View.VISIBLE);
                            vehicleAdapter.setVehicles(vehicles);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading vehicles", e);
                        Toast.makeText(getContext(), "Error loading vehicles", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Method to refresh vehicles after adding a new one
    public void refreshVehicles() {
        loadUserVehicles();
    }
}