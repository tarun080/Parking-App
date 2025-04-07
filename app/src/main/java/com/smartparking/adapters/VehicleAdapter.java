// app/src/main/java/com/smartparking/adapters/VehicleAdapter.java
package com.smartparking.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartparking.R;
import com.smartparking.models.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private Context context;
    private List<Vehicle> vehicles = new ArrayList<>();

    public VehicleAdapter(Context context) {
        this.context = context;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        holder.bind(vehicle);
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    class VehicleViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewVehicleInfo;
        private TextView textViewLicensePlate;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewVehicleInfo = itemView.findViewById(R.id.textViewVehicleInfo);
            textViewLicensePlate = itemView.findViewById(R.id.textViewLicensePlate);
        }

        public void bind(Vehicle vehicle) {
            String vehicleInfo = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getColor() + ")";
            textViewVehicleInfo.setText(vehicleInfo);
            textViewLicensePlate.setText(vehicle.getLicensePlate());
        }
    }
}