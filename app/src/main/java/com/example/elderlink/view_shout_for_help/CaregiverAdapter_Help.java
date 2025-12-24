// This is a RecyclerView Adapter that displays a list of caregivers for the elderly to choose an emergency contact.

package com.example.elderlink.view_shout_for_help;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.Caregiver;
import com.example.elderlink.R;

import java.util.List;

public class CaregiverAdapter_Help extends RecyclerView.Adapter<CaregiverAdapter_Help.CaregiverViewHolder> {
    private List<Caregiver> caregiverList;
    private OnCaregiverSelectListener listener;

    public interface OnCaregiverSelectListener {
        void onCaregiverSelected(Caregiver caregiver);
    }

    public CaregiverAdapter_Help(List<Caregiver> caregiverList, OnCaregiverSelectListener listener) {
        this.caregiverList = caregiverList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CaregiverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.caregiver_item_help, parent, false);
        return new CaregiverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaregiverViewHolder holder, int position) {
        Caregiver caregiver = caregiverList.get(position);

        // Set caregiver information（name）
        holder.caregiverName.setText(caregiver.getName());


        // Set phone number with color coding
        if (caregiver.hasPhone()) {
            holder.caregiverPhone.setText("Phone: " + caregiver.getPhone());
            holder.caregiverPhone.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));

            // Enable and show select button for caregivers with phone
            if (holder.selectButton != null) {
                holder.selectButton.setEnabled(true);
                holder.selectButton.setVisibility(View.VISIBLE);
                holder.selectButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCaregiverSelected(caregiver);
                    }
                });
            }
        } else {
            holder.caregiverPhone.setText("Phone: Not provided");
            holder.caregiverPhone.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));

            // Disable and hide select button for caregivers without phone
            if (holder.selectButton != null) {
                holder.selectButton.setEnabled(false);
                holder.selectButton.setVisibility(View.GONE);
            }
        }

        // Show "Primary caregiver" badge if this is the current caregiver-- idk why not showing
        if (holder.youBadge != null) {
            if (caregiver.isCurrentUser()) {
                holder.youBadge.setVisibility(View.VISIBLE);
            } else {
                holder.youBadge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return caregiverList.size();
    }

    static class CaregiverViewHolder extends RecyclerView.ViewHolder {
        TextView caregiverName, caregiverPhone, youBadge;
        Button selectButton;

        CaregiverViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views with null checks
            caregiverName = itemView.findViewById(R.id.caregiverName);
            caregiverPhone = itemView.findViewById(R.id.caregiverPhone);

            // These might be null if not in layout, so check
            youBadge = itemView.findViewById(R.id.youBadge);
            selectButton = itemView.findViewById(R.id.selectButton);

            // Log for debugging
            if (selectButton == null) {
                android.util.Log.e("CaregiverAdapter", "selectButton not found in layout");
            }
            if (youBadge == null) {
                android.util.Log.e("CaregiverAdapter", "youBadge not found in layout");
            }
        }
    }
}