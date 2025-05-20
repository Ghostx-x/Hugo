package com.example.hugo.bottomnavbar.Profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyBookingsFragment extends Fragment {

    private RecyclerView bookingsRecyclerView;
    private BookingAdapter adapter;
    private List<Booking> bookingList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_bookings, container, false);
        bookingsRecyclerView = view.findViewById(R.id.bookings_recycler_view);
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        bookingList = new ArrayList<>();
        adapter = new BookingAdapter(bookingList, getContext());
        bookingsRecyclerView.setAdapter(adapter);

        ImageView backArrow = view.findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .commit();
            }
        });

        loadBookings();
        return view;
    }

    private void loadBookings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance()
                .getReference("Bookings")
                .child(user.getUid());
        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingList.clear();
                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    String userId = bookingSnapshot.child("bookedUserId").getValue(String.class);
                    String userName = bookingSnapshot.child("bookedUserName").getValue(String.class);
                    String photoBase64 = bookingSnapshot.child("bookedUserPhotoBase64").getValue(String.class);
                    String bookedTime = bookingSnapshot.child("bookedTime").getValue(String.class);
                    String status = bookingSnapshot.child("status").getValue(String.class);
                    if (userId != null && userName != null && bookedTime != null && status != null) {
                        bookingList.add(new Booking(userId, userName, photoBase64 != null ? photoBase64 : "", bookedTime, status));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load bookings", Toast.LENGTH_SHORT).show();
            }
        });
    }
}