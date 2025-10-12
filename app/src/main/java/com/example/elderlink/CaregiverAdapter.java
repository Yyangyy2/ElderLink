//This adapter is for displaying People caring for you section in MainActivityElder.

package com.example.elderlink;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CaregiverAdapter extends RecyclerView.Adapter<CaregiverAdapter.CaregiverViewHolder> {
    private List<String> caregiverList;

    public CaregiverAdapter(List<String> caregiverList) {
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
        String caregiverInfo = caregiverList.get(position);

        // Remove the "(You)" suffix for display if present
        String displayName = caregiverInfo.replace(" (You)", "");
        holder.caregiverName.setText(displayName);

        // Style differently if it's the main caregiver
        if (caregiverInfo.contains("(You)")) {
            holder.caregiverName.setTypeface(holder.caregiverName.getTypeface(), Typeface.BOLD);
            // You can add other styling like different color if needed
        } else {
            holder.caregiverName.setTypeface(holder.caregiverName.getTypeface(), Typeface.NORMAL);
        }
    }

    @Override
    public int getItemCount() {
        return caregiverList.size();
    }

    static class CaregiverViewHolder extends RecyclerView.ViewHolder {
        TextView caregiverName;

        CaregiverViewHolder(@NonNull View itemView) {
            super(itemView);
            caregiverName = itemView.findViewById(R.id.caregiverName);
        }
    }
}
