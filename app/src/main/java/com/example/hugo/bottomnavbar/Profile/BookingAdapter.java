package com.example.hugo.bottomnavbar.Profile;

import android.content.Context;
import android.os.Bundle;
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
import com.squareup.picasso.Picasso;

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

        if (booking.bookedUserPhotoUrl != null && !booking.bookedUserPhotoUrl.isEmpty()) {
            Picasso.get()
                    .load(booking.bookedUserPhotoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.userImage);
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
            args.putString("user_id", booking.bookedUserId); // Fixed to match ARG_USER_ID
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