package com.example.hugo.bottomnavbar.Search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;

import java.util.ArrayList;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<String> timeSlots;
    private OnTimeSlotClickListener clickListener;
    private int selectedPosition = -1;

    public TimeSlotAdapter(List<String> timeSlots, OnTimeSlotClickListener clickListener) {
        this.timeSlots = new ArrayList<>(timeSlots); // Create a copy to avoid external modification
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        String timeSlot = timeSlots.get(position);
        holder.timeButton.setText(timeSlot);
        holder.timeButton.setSelected(position == selectedPosition);
        holder.timeButton.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (clickListener != null) {
                clickListener.onTimeSlotClick(timeSlot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    public void updateTimeSlots(List<String> newTimeSlots) {
        this.timeSlots.clear();
        if (newTimeSlots != null) {
            this.timeSlots.addAll(newTimeSlots);
        }
        notifyDataSetChanged();
    }

    public static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        Button timeButton;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            timeButton = itemView.findViewById(R.id.btnTime);
        }
    }

    public interface OnTimeSlotClickListener {
        void onTimeSlotClick(String timeSlot);
    }
}