package com.example.hugo.bottomnavbar.Search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<String> timeSlots;
    private OnTimeSlotClickListener listener;
    private int selectedPosition = -1;

    public interface OnTimeSlotClickListener {
        void onTimeSlotClick(String timeSlot);
    }

    public TimeSlotAdapter(List<String> timeSlots, OnTimeSlotClickListener listener) {
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    public void updateTimeSlots(List<String> newTimeSlots) {
        this.timeSlots = newTimeSlots;
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        String timeSlot = timeSlots.get(position);
        holder.textView.setText(timeSlot);
        holder.itemView.setBackgroundResource(position == selectedPosition ? R.color.darkblue : android.R.color.transparent);
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
            listener.onTimeSlotClick(timeSlot);
        });

        Context context = holder.itemView.getContext();
        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.color.darkblue);
            holder.textView.setTextColor(context.getResources().getColor(R.color.milky));
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
            holder.textView.setTextColor(context.getResources().getColor(R.color.black));
        }

    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.time_slot_text);
        }
    }
}