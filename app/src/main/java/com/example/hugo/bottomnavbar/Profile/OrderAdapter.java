package com.example.hugo.bottomnavbar.Profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Notification> orders;
    private Context context;
    private OnStatusUpdateListener onStatusUpdate;

    public interface OnStatusUpdateListener {
        void onStatusUpdate(String notificationId, String bookingId, String userId, String status);
    }

    public OrderAdapter(List<Notification> orders, Context context, OnStatusUpdateListener onStatusUpdate) {
        this.orders = orders;
        this.context = context;
        this.onStatusUpdate = onStatusUpdate;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Notification order = orders.get(position);
        holder.userName.setText(order.userName);
        holder.bookedTime.setText("Booked for: " + order.bookedTime);
        holder.status.setText("Status: " + order.status);

        if ("pending".equals(order.status)) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.declineButton.setVisibility(View.VISIBLE);
        } else {
            holder.acceptButton.setVisibility(View.GONE);
            holder.declineButton.setVisibility(View.GONE);
        }

        holder.acceptButton.setOnClickListener(v -> onStatusUpdate.onStatusUpdate(order.notificationId, order.bookingId, order.userId, "accepted"));
        holder.declineButton.setOnClickListener(v -> onStatusUpdate.onStatusUpdate(order.notificationId, order.bookingId, order.userId, "declined"));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView userName, bookedTime, status;
        Button acceptButton, declineButton;

        OrderViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            bookedTime = itemView.findViewById(R.id.booked_time);
            status = itemView.findViewById(R.id.order_status);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }
    }
}