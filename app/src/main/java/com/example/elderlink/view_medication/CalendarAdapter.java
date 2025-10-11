package com.example.elderlink.view_medication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private List<String> dateList;
    private String selectedDate;
    private OnDateClickListener listener;
    private Map<String, String> dateStatusMap; // Map to store status for each date

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public CalendarAdapter(List<String> dateList, String selectedDate, OnDateClickListener listener) {
        this.dateList = dateList;
        this.selectedDate = selectedDate;
        this.listener = listener;
        this.dateStatusMap = new HashMap<>();
    }

    // Method to update medication status for dates
    public void updateDateStatus(Map<String, String> statusMap) {
        this.dateStatusMap.clear();
        this.dateStatusMap.putAll(statusMap);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.calendar_item, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String date = dateList.get(position);

        // Format the date for display
        String formattedDate = formatDateForDisplay(date);
        holder.dateText.setText(formattedDate);

        // Get today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        // Reset default style first
        holder.dateText.setBackgroundColor(Color.TRANSPARENT);
        holder.dateText.setTextColor(Color.BLACK);
        holder.indicatorDot.setVisibility(View.VISIBLE);

        // Set indicator dot color based on medication status
        String status = dateStatusMap.get(date);
        if (status != null) {
            switch (status) {
                case "GREEN": // All medications taken
                    holder.indicatorDot.setBackgroundResource(R.drawable.indicator_dot_green);
                    break;
                case "RED": // At least one medication missed
                    holder.indicatorDot.setBackgroundResource(R.drawable.indicator_dot_red);
                    break;
                case "BLUE": // null, Upcoming, or Pending
                    holder.indicatorDot.setBackgroundResource(R.drawable.indicator_dot_default);
                    break;
                default:
                    holder.indicatorDot.setVisibility(View.INVISIBLE);
            }
        } else {
            // No medications for this date
            holder.indicatorDot.setVisibility(View.INVISIBLE);
        }

        // Mark today with black border
        if (date.equals(today)) {
            holder.dateText.setBackgroundResource(R.drawable.calendar_today_border);
            holder.dateText.setTextColor(Color.WHITE);
        }

        // Color selected date
        if (date.equals(selectedDate)) {
            holder.dateText.setTextColor(Color.WHITE);
            holder.dateText.setBackgroundColor(Color.parseColor("#0066ff")); // blue fill
            // If selected date is also today, keep border visible:
            if (date.equals(today)) {
                holder.dateText.setBackgroundResource(R.drawable.calendar_today_border);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            selectedDate = date;
            listener.onDateClick(date);
            notifyDataSetChanged();
        });
    }

    private String formatDateForDisplay(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = inputFormat.parse(rawDate);

            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE\nMMM d", Locale.getDefault());
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return rawDate; // fallback
        }
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        View indicatorDot;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.calendarDateText);
            indicatorDot = itemView.findViewById(R.id.indicatorDot);
        }
    }
}