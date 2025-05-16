package com.example.hugo.bottomnavbar.Profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

        Bitmap bitmap = decodeBase64ToBitmap(booking.bookedUserPhotoUrl);
        if (bitmap != null) {
            holder.userImage.setImageBitmap(bitmap);
        } else {
            holder.userImage.setImageResource(R.drawable.ic_profile);
        }

        holder.viewDetailsButton.setOnClickListener(v -> {
            ViewProfileFragment viewProfileFragment = new ViewProfileFragment();
            Bundle args = new Bundle();
            args.putString("userId", booking.bookedUserId);
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

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView userName, bookingTime;
        Button viewDetailsButton;

        BookingViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            bookingTime = itemView.findViewById(R.id.booking_time);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
        }
    }
}