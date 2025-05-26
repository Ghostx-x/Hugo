package com.example.hugo.bottomnavbar.Profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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

    private static final String TAG = "MyBookingsFragment";
    private RecyclerView bookingsRecyclerView;
    private BookingAdapter adapter;
    private List<Booking> bookingList;
    private TextView emptyStateText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_bookings, container, false);
        bookingsRecyclerView = view.findViewById(R.id.bookings_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        bookingList = new ArrayList<>();
        adapter = new BookingAdapter(bookingList, getContext(), getParentFragmentManager());
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

    public void loadBookings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            if (emptyStateText != null) emptyStateText.setVisibility(View.VISIBLE);
            return;
        }

        Log.d(TAG, "Loading bookings for user: " + user.getEmail());
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        appointmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingList.clear();
                if (!snapshot.exists()) {
                    Log.w(TAG, "No appointments found in Appointments node");
                    updateEmptyState(true);
                    adapter.notifyDataSetChanged();
                    return;
                }

                int appointmentCount = 0;
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    String userEmail = appointmentSnapshot.child("userEmail").getValue(String.class);
                    Log.d(TAG, "Checking appointment: " + appointmentSnapshot.getKey() + ", userEmail: " + userEmail);
                    if (userEmail != null && userEmail.equalsIgnoreCase(user.getEmail())) {
                        appointmentCount++;
                        String appointmentId = appointmentSnapshot.child("uniqueID").getValue(String.class);
                        String bookedUserId = appointmentSnapshot.child("dogWalkerId").getValue(String.class);
                        String bookedTime = appointmentSnapshot.child("weekDay_monthName_dayOfMonth").getValue(String.class) + " " +
                                appointmentSnapshot.child("time").getValue(String.class);
                        String status = appointmentSnapshot.child("status").getValue(String.class);

                        if (bookedUserId != null && bookedTime != null && status != null) {
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(bookedUserId);
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                    String bookedUserName = userSnapshot.child("name").getValue(String.class);
                                    String photoBase64 = userSnapshot.child("profileImageBase64").getValue(String.class);
                                    if (bookedUserName != null) {
                                        bookingList.add(new Booking(
                                                appointmentId,
                                                bookedUserId,
                                                bookedUserName,
                                                photoBase64 != null ? photoBase64 : "",
                                                bookedTime,
                                                status
                                        ));
                                        Log.d(TAG, "Added booking: " + bookedUserName + ", time: " + bookedTime + ", id: " + appointmentId);
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState(false);
                                    } else {
                                        Log.w(TAG, "No name found for user: " + bookedUserId);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Failed to load user data: " + error.getMessage());
                                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Log.w(TAG, "Incomplete appointment data: " + appointmentSnapshot.getKey());
                        }
                    }
                }
                Log.d(TAG, "Found " + appointmentCount + " appointments for user");
                if (appointmentCount == 0) {
                    updateEmptyState(true);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load bookings: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load bookings", Toast.LENGTH_SHORT).show();
                updateEmptyState(true);
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (emptyStateText != null) {
            emptyStateText.setText(isEmpty ? "No bookings found" : "");
            emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            bookingsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}