package com.example.hugo.bottomnavbar.Profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

        if (order.userPhotoBase64 != null && !order.userPhotoBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(order.userPhotoBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                Bitmap circularBitmap = getCircularBitmap(bitmap);
                holder.userImage.setImageBitmap(circularBitmap);
            } catch (Exception e) {
                Log.e("OrderAdapter", "Failed to load profile image: " + e.getMessage(), e);
                holder.userImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.userImage.setImageResource(R.drawable.ic_profile);
        }

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

    // Helper method to transform a bitmap into a circular shape
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(android.graphics.Color.WHITE);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView userName, bookedTime, status;
        Button acceptButton, declineButton;

        OrderViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            bookedTime = itemView.findViewById(R.id.booked_time);
            status = itemView.findViewById(R.id.order_status);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }
    }
}