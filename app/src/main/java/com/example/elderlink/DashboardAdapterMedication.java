package com.example.elderlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.view_medication.Model_medication;

import java.util.List;

public class DashboardAdapterMedication extends RecyclerView.Adapter<DashboardAdapterMedication.MedicationViewHolder> {

    private List<Model_medication> medicationList;

    public DashboardAdapterMedication(List<Model_medication> medicationList) {
        this.medicationList = medicationList;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Model_medication medication = medicationList.get(position);
        holder.tvMedName.setText(medication.getName());
        holder.tvDosageTime.setText(medication.getDosage() + " " + medication.getUnit() + " - " + medication.getTime());

        // Set status with color coding
        String status = medication.getStatus();
        holder.tvStatus.setText(status != null ? status : "Upcoming");

        // Set status color
        if (status != null) {
            switch (status) {
                case "Taken":
                    holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                    break;
                case "Pending":
                    holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_light));
                    break;
                case "Missed":
                    holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                    break;
                default:
                    holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return medicationList.size();
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedName, tvDosageTime, tvStatus;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedName = itemView.findViewById(R.id.tvMedName);
            tvDosageTime = itemView.findViewById(R.id.tvDosageTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}