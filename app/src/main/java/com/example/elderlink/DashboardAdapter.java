//This java file controls what to display in item_dashboard_date_group.xml
package com.example.elderlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DateGroupViewHolder> {

    private List<CheckOnElderlyActivity.DateGroup> dateGroupList;

    public DashboardAdapter(List<CheckOnElderlyActivity.DateGroup> dateGroupList) {
        this.dateGroupList = dateGroupList;
    }

    @NonNull
    @Override
    public DateGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_date_group, parent, false);
        return new DateGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateGroupViewHolder holder, int position) {
        CheckOnElderlyActivity.DateGroup dateGroup = dateGroupList.get(position);

        // Format date for display
        String formattedDate = formatDate(dateGroup.getDate());
        holder.tvDate.setText(formattedDate);
        holder.tvProgress.setText(dateGroup.getProgress());

        // Setup nested RecyclerView for medications
        DashboardAdapterMedication medicationAdapter = new DashboardAdapterMedication(dateGroup.getMedications());
        holder.medicationsRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.medicationsRecyclerView.setAdapter(medicationAdapter);
    }

    @Override
    public int getItemCount() {
        return dateGroupList.size();
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    static class DateGroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvProgress;
        RecyclerView medicationsRecyclerView;

        public DateGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            medicationsRecyclerView = itemView.findViewById(R.id.medicationsRecyclerView);
        }
    }
}