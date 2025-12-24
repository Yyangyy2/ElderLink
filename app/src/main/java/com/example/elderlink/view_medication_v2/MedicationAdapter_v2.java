package com.example.elderlink.view_medication_v2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.R;

import java.util.List;

public class MedicationAdapter_v2 extends RecyclerView.Adapter<MedicationAdapter_v2.MedicationViewHolder> {

    private Context context;
    private List<Model_medication_v2> medicationList;
    private OnMedicationClickListener listener;

    // Interface for click events
    public interface OnMedicationClickListener {
        void onMedicationClick(int position, Model_medication_v2 medication);
        void onEditClick(int position, Model_medication_v2 medication);
    }

    public MedicationAdapter_v2(Context context, List<Model_medication_v2> medicationList, OnMedicationClickListener listener) {
        this.context = context;
        this.medicationList = medicationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.medication_item_v2, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Model_medication_v2 medication = medicationList.get(position);

        // Set brand name
        holder.brandName.setText(medication.getBrandname());

        // Set medication name
        holder.medName.setText(medication.getMedname());

        // Set times per day
        holder.timesPerDay.setText(medication.getTimesperday());

        // Load medication image from Base64
        if (medication.getImageBase64() != null && !medication.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(medication.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.medImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                // If image loading fails, use default image
                holder.medImage.setImageResource(R.drawable.view_medication_logo);
            }
        } else {
            // Use default image if no image is available
            holder.medImage.setImageResource(R.drawable.view_medication_logo);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMedicationClick(position, medication);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(position, medication);
            }
        });
    }

    @Override
    public int getItemCount() {
        return medicationList != null ? medicationList.size() : 0;
    }

    // Update data method
    public void updateData(List<Model_medication_v2> newList) {
        medicationList = newList;
        notifyDataSetChanged();
    }

    // Filter method for search
    public void filterList(List<Model_medication_v2> filteredList) {
        medicationList = filteredList;
        notifyDataSetChanged();
    }

    // Get medication at position
    public Model_medication_v2 getMedicationAt(int position) {
        if (position >= 0 && position < medicationList.size()) {
            return medicationList.get(position);
        }
        return null;
    }

    // ViewHolder class
    public static class MedicationViewHolder extends RecyclerView.ViewHolder {
        ImageView medImage;
        TextView brandName, medName, timesPerDay;
        View btnEdit;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            medImage = itemView.findViewById(R.id.med_image);
            brandName = itemView.findViewById(R.id.brand_name);
            medName = itemView.findViewById(R.id.med_name);
            timesPerDay = itemView.findViewById(R.id.times_per_day);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}