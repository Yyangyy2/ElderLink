package com.example.elderlink.view_medication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlink.R;

import java.text.BreakIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private List<String> dateList;
    private String selectedDate;
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public CalendarAdapter(List<String> dateList, String selectedDate, OnDateClickListener listener) {
        this.dateList = dateList;
        this.selectedDate = selectedDate;
        this.listener = listener;
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
        holder.dateText.setText(date);


        String rawDate = dateList.get(position);

        // Format the date from yyyy-MM-dd to EEE MMM d (for display)
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = inputFormat.parse(rawDate);

            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE\nMMM d", Locale.getDefault());
            String formattedDate = outputFormat.format(parsedDate);

            holder.dateText.setText(formattedDate);  // shows Thu, Aug 21 format
        } catch (ParseException e) {
            e.printStackTrace();
            holder.dateText.setText(rawDate); // fallback
        }


        // Get today's date (yyyy-MM-dd)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        // Reset default style first
        holder.dateText.setBackgroundColor(Color.TRANSPARENT);
        holder.dateText.setTextColor(Color.BLACK);

        // Always mark today with black border
        if (date.equals(today)) {
            holder.dateText.setBackgroundResource(R.drawable.calendar_today_border);
            holder.dateText.setTextColor(Color.WHITE);
        }

        // color selected date
        if (date.equals(selectedDate)) {
            holder.dateText.setTextColor(Color.WHITE);
            holder.dateText.setBackgroundColor(Color.parseColor("#0066ff")); // blue fill
            // if selected date is also today, keep border visible:
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

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.calendarDateText);
        }
    }
}

