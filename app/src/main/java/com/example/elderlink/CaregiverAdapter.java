package com.example.elderlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CaregiverAdapter extends RecyclerView.Adapter<CaregiverAdapter.CaregiverViewHolder> {
    private List<Caregiver> caregiverList;

    public CaregiverAdapter(List<Caregiver> caregiverList) {
        this.caregiverList = caregiverList;
    }

    @NonNull
    @Override
    public CaregiverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.caregiver_item, parent, false);
        return new CaregiverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaregiverViewHolder holder, int position) {
        Caregiver caregiver = caregiverList.get(position);

        holder.caregiverName.setText(caregiver.getName());
        holder.caregiverEmail.setText(caregiver.getEmail());
        holder.caregiverPhone.setText(caregiver.getPhone() != null ? caregiver.getPhone() : "Not provided");

        // Show "You" badge if this is the current caregiver
        if (caregiver.isCurrentUser()) {
            holder.youBadge.setVisibility(View.VISIBLE);
        } else {
            holder.youBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return caregiverList.size();
    }

    static class CaregiverViewHolder extends RecyclerView.ViewHolder {
        TextView caregiverName, caregiverEmail, caregiverPhone, youBadge;

        CaregiverViewHolder(@NonNull View itemView) {
            super(itemView);
            caregiverName = itemView.findViewById(R.id.caregiverName);
            caregiverEmail = itemView.findViewById(R.id.caregiverEmail);
            caregiverPhone = itemView.findViewById(R.id.caregiverPhone);
            youBadge = itemView.findViewById(R.id.youBadge);
        }
    }
}