// app/src/main/java/com/smartparking/fragments/AddVehicleDialogFragment.java
package com.smartparking.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartparking.R;
import com.smartparking.models.Vehicle;

import java.util.UUID;

public class AddVehicleDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "AddVehicleDialog";

    private EditText editTextLicensePlate;
    private EditText editTextMake;
    private EditText editTextModel;
    private EditText editTextColor;
    private Spinner spinnerVehicleType;
    private Button buttonSave;

    private FirebaseFirestore firestore;
    private String userId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog);

        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_vehicle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        editTextLicensePlate = view.findViewById(R.id.editTextLicensePlate);
        editTextMake = view.findViewById(R.id.editTextMake);
        editTextModel = view.findViewById(R.id.editTextModel);
        editTextColor = view.findViewById(R.id.editTextColor);
        spinnerVehicleType = view.findViewById(R.id.spinnerVehicleType);
        buttonSave = view.findViewById(R.id.buttonSave);

        // Set up vehicle type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.vehicle_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(adapter);

        // Set click listener for save button
        buttonSave.setOnClickListener(v -> saveVehicle());
    }

    private void saveVehicle() {
        String licensePlate = editTextLicensePlate.getText().toString().trim();
        String make = editTextMake.getText().toString().trim();
        String model = editTextModel.getText().toString().trim();
        String color = editTextColor.getText().toString().trim();
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();

        // Validate input
        if (licensePlate.isEmpty()) {
            editTextLicensePlate.setError("License plate is required");
            return;
        }

        if (make.isEmpty()) {
            editTextMake.setError("Make is required");
            return;
        }

        if (model.isEmpty()) {
            editTextModel.setError("Model is required");
            return;
        }

        if (color.isEmpty()) {
            editTextColor.setError("Color is required");
            return;
        }

        if (userId.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create vehicle object
        String vehicleId = UUID.randomUUID().toString();
        Vehicle vehicle = new Vehicle(
                vehicleId,
                userId,
                licensePlate,
                make,
                model,
                color,
                vehicleType
        );

        // Save to Firestore
        buttonSave.setEnabled(false);
        buttonSave.setText("Saving...");

        firestore.collection("vehicles")
                .document(vehicleId)
                .set(vehicle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Vehicle added successfully");
                    Toast.makeText(getContext(), "Vehicle added successfully", Toast.LENGTH_SHORT).show();
                    dismiss();

                    // Notify parent fragment to refresh
                    if (getParentFragment() instanceof ProfileFragment) {
                        ((ProfileFragment) getParentFragment()).refreshVehicles();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding vehicle", e);
                    Toast.makeText(getContext(), "Error adding vehicle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonSave.setEnabled(true);
                    buttonSave.setText("Save");
                });
    }
}