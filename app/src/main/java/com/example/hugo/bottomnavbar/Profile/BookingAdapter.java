package com.example.hugo.bottomnavbar.Profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.ViewProfileFragment;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private Context context;

    public BookingAdapter(List<Booking> bookings, Context context) {
        this.bookings = bookings;
        this.context = context;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.userName.setText(booking.bookedUserName);
        holder.bookingTime.setText("Booked for: " + booking.bookedTime);
        holder.status.setText("Status: " + booking.status);

        if (booking.bookedUserPhotoBase64 != null && !booking.bookedUserPhotoBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(booking.bookedUserPhotoBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                Bitmap circularBitmap = getCircularBitmap(bitmap);
                holder.userImage.setImageBitmap(circularBitmap);
            } catch (Exception e) {
                Log.e("BookingAdapter", "Failed to load profile image: " + e.getMessage(), e);
                holder.userImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.userImage.setImageResource(R.drawable.ic_profile);
        }

        holder.viewDetailsButton.setOnClickListener(v -> {
            Log.d("BookingAdapter", "View Details clicked, bookedUserId: " + booking.bookedUserId);
            if (booking.bookedUserId == null || booking.bookedUserId.isEmpty()) {
                Log.w("BookingAdapter", "Invalid bookedUserId");
                Toast.makeText(context, "Cannot view profile: Invalid user ID", Toast.LENGTH_SHORT).show();
                return;
            }
            ViewProfileFragment viewProfileFragment = new ViewProfileFragment();
            Bundle args = new Bundle();
            args.putString("user_id", booking.bookedUserId);
            viewProfileFragment.setArguments(args);
            FragmentActivity activity = (FragmentActivity) context;
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, viewProfileFragment)
                    .addToBackStack("ViewProfileFragment")
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
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

    class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView userName, bookingTime, status;
        Button viewDetailsButton;

        BookingViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            bookingTime = itemView.findViewById(R.id.booking_time);
            status = itemView.findViewById(R.id.booking_status);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
        }
    }
}