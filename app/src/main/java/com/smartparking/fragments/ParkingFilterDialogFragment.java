// app/src/main/java/com/smartparking/fragments/ParkingFilterDialogFragment.java
package com.smartparking.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.smartparking.R;
import com.smartparking.viewmodels.MapViewModel;

public class ParkingFilterDialogFragment extends DialogFragment {

    private MapViewModel mapViewModel;

    private SeekBar seekBarRadius;
    private TextView textViewRadius;
    private Button buttonApply;
    private Button buttonReset;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_AppCompat_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parking_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        seekBarRadius = view.findViewById(R.id.seekBarRadius);
        textViewRadius = view.findViewById(R.id.textViewRadius);
        buttonApply = view.findViewById(R.id.buttonApply);
        buttonReset = view.findViewById(R.id.buttonReset);

        // Initialize ViewModel
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        // Set initial values
        Double currentRadius = mapViewModel.getSearchRadius().getValue();
        if (currentRadius != null) {
            int progress = (int) (currentRadius * 10);  // Convert km to seekbar value (0-100)
            seekBarRadius.setProgress(progress);
            updateRadiusText(progress);
        }

        // Set up seekbar listener
        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateRadiusText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });

        // Set click listeners
        buttonApply.setOnClickListener(v -> {
            double radius = seekBarRadius.getProgress() / 10.0;  // Convert to km
            mapViewModel.setSearchRadius(radius);
            dismiss();
        });

        buttonReset.setOnClickListener(v -> {
            seekBarRadius.setProgress(50);  // 5 km default
            updateRadiusText(50);
        });
    }

    private void updateRadiusText(int progress) {
        double radius = progress / 10.0;  // Convert to km
        textViewRadius.setText(getString(R.string.radius_km, radius));
    }
}