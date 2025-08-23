package com.example.elderlink.view_medication;

import android.content.Context;
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

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {

    private Context context;
    private List<Model_medication> medicationList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(Model_medication medication);
    }

    public MedicationAdapter(Context context, List<Model_medication> medicationList, OnItemClickListener listener) {
        this.context = context;
        this.medicationList = medicationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.medication_item, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Model_medication med = medicationList.get(position);

        holder.medName.setText(med.getName());
        holder.medTime.setText(med.getTime());
        holder.medUnit.setText(med.getUnit());

        // Convert Base64 image if available
        if (med.getImageBase64() != null && !med.getImageBase64().isEmpty()) {
            byte[] decodedBytes = Base64.decode(med.getImageBase64(), Base64.DEFAULT);
            holder.medImage.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
        } else {
            holder.medImage.setImageResource(R.drawable.view_medication_logo); // default
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(med));

    }

    @Override
    public int getItemCount() {
        return medicationList.size();
    }

    public static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView medName, medTime, medUnit, btnEdit;
        ImageView medImage;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            medName = itemView.findViewById(R.id.med_name);
            medTime = itemView.findViewById(R.id.med_time);
            medUnit = itemView.findViewById(R.id.med_unit);
            medImage = itemView.findViewById(R.id.med_image);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
