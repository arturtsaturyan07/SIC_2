package com.example.sic_2;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CampDateAdapter extends RecyclerView.Adapter<CampDateAdapter.DateViewHolder> {
    private List<Long> dateList; // List of millis for each day of camp
    private long selectedDate;
    private OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(long dateMillis);
    }

    public CampDateAdapter(List<Long> dateList, long selectedDate, OnDateSelectedListener listener) {
        this.dateList = dateList;
        this.selectedDate = selectedDate;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use a simple MaterialButton or CardView for each date
        Button btn = new Button(parent.getContext());
        btn.setAllCaps(false);
        btn.setPadding(24, 8, 24, 8);
        btn.setBackgroundResource(R.drawable.chip_selector); // custom drawable for selection
        return new DateViewHolder(btn);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        long millis = dateList.get(position);
        String dateText = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(millis));
        Button btn = (Button) holder.itemView;
        btn.setText(dateText);
        btn.setSelected(millis == selectedDate);
        btn.setOnClickListener(v -> {
            selectedDate = millis;
            notifyDataSetChanged();
            if (listener != null) listener.onDateSelected(millis);
        });
    }

    @Override
    public int getItemCount() { return dateList.size(); }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        public DateViewHolder(@NonNull View itemView) { super(itemView); }
    }
}