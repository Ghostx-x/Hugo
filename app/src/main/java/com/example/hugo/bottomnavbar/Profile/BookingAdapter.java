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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.ViewProfileFragment;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private static final String TAG = "BookingAdapter";
    private List<Booking> bookings;
    private Context context;
    private FragmentManager fragmentManager;

    public BookingAdapter(List<Booking> bookings, Context context, FragmentManager fragmentManager) {
        this.bookings = bookings;
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        Log.d(TAG, "Creating view holder for item_booking");
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        Log.d(TAG, "Binding booking: " + booking.bookedUserName + ", time: " + booking.bookedTime);
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
                Log.e(TAG, "Failed to load profile image: " + e.getMessage(), e);
                holder.userImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.userImage.setImageResource(R.drawable.ic_profile);
        }

        holder.viewDetailsButton.setOnClickListener(v -> {
            Log.d(TAG, "View Details clicked, bookedUserId: " + booking.bookedUserId);
            if (booking.bookedUserId == null || booking.bookedUserId.isEmpty()) {
                Log.w(TAG, "Invalid bookedUserId");
                Toast.makeText(context, "Cannot view profile: Invalid user ID", Toast.LENGTH_SHORT).show();
                return;
            }
            ViewProfileFragment viewProfileFragment = new ViewProfileFragment();
            Bundle args = new Bundle();
            args.putString("user_id", booking.bookedUserId);
            viewProfileFragment.setArguments(args);
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, viewProfileFragment)
                    .addToBackStack("ViewProfileFragment")
                    .commit();
        });

        holder.payButton.setOnClickListener(v -> {
            Log.d(TAG, "Pay button clicked for booking with user: " + booking.bookedUserName + ", appointmentId: " + booking.appointmentId);
            if (booking.appointmentId == null || booking.appointmentId.isEmpty()) {
                Log.w(TAG, "Invalid appointmentId for payment");
                Toast.makeText(context, "Cannot process payment: Invalid appointment ID", Toast.LENGTH_SHORT).show();
                return;
            }
            PaymentFragment paymentFragment = new PaymentFragment();
            Bundle args = new Bundle();
            args.putString("appointmentId", booking.appointmentId);
            paymentFragment.setArguments(args);

            try {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, paymentFragment)
                        .addToBackStack(null)
                        .commit();
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to PaymentFragment: " + e.getMessage());
                Toast.makeText(context, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "Item count: " + bookings.size());
        return bookings.size();
    }

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

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView userName, bookingTime, status;
        Button viewDetailsButton, payButton;

        BookingViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            bookingTime = itemView.findViewById(R.id.booking_time);
            status = itemView.findViewById(R.id.booking_status);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
            payButton = itemView.findViewById(R.id.pay_button);
        }
    }
}