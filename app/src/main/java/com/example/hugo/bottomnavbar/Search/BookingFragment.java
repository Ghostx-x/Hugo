package com.example.hugo.bottomnavbar.Search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingFragment extends Fragment {

    private static final String ARG_PROVIDER_ID = "provider_id";
    private static final String TAG = "BookingFragment";
    private Spinner daySpinner;
    private RecyclerView timeSlotsRecyclerView;
    private Button confirmBookingButton;
    private TimeSlotAdapter timeSlotAdapter;
    private DatabaseReference providerRef;
    private String providerId;
    private Map<String, List<String>> availability = new HashMap<>();
    private String selectedDay;
    private String selectedTimeSlot;

    public static BookingFragment newInstance(String providerId) {
        BookingFragment fragment = new BookingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROVIDER_ID, providerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Initialize UI
        daySpinner = view.findViewById(R.id.day_spinner);
        timeSlotsRecyclerView = view.findViewById(R.id.time_slots_recycler_view);
        confirmBookingButton = view.findViewById(R.id.confirm_booking_button);

        // Set up RecyclerView
        timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        timeSlotAdapter = new TimeSlotAdapter(new ArrayList<>(), timeSlot -> {
            selectedTimeSlot = timeSlot;
            Log.d(TAG, "Selected time slot: " + timeSlot);
        });
        timeSlotsRecyclerView.setAdapter(timeSlotAdapter);

        // Get provider ID
        providerId = getArguments() != null ? getArguments().getString(ARG_PROVIDER_ID) : null;
        if (providerId == null) {
            Log.w(TAG, "Invalid provider ID");
            Toast.makeText(getContext(), "Invalid provider", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        // Fetch provider availability
        providerRef = FirebaseDatabase.getInstance().getReference("Users").child(providerId);
        loadProviderAvailability();

        // Confirm booking
        confirmBookingButton.setOnClickListener(v -> confirmBooking());
    }

    private void loadProviderAvailability() {
        providerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null || user.availability == null) {
                    Log.w(TAG, "Provider or availability not found");
                    Toast.makeText(getContext(), "No availability found", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }

                availability = user.availability;
                List<String> days = new ArrayList<>(availability.keySet());
                if (days.isEmpty()) {
                    Log.w(TAG, "No available days");
                    Toast.makeText(getContext(), "No available days", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }

                // Populate day spinner
                ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, days);
                dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                daySpinner.setAdapter(dayAdapter);

                // Update time slots when day is selected
                daySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        selectedDay = days.get(position);
                        List<String> slots = availability.get(selectedDay);
                        timeSlotAdapter.updateTimeSlots(slots != null ? slots : new ArrayList<>());
                        selectedTimeSlot = null; // Reset selection
                        Log.d(TAG, "Selected day: " + selectedDay);
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        selectedDay = null;
                        timeSlotAdapter.updateTimeSlots(new ArrayList<>());
                    }
                });

                // Select first day by default
                if (!days.isEmpty()) {
                    daySpinner.setSelection(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load availability: " + error.getMessage());
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void confirmBooking() {
        if (selectedDay == null || selectedTimeSlot == null) {
            Log.w(TAG, "No day or time slot selected");
            Toast.makeText(getContext(), "Please select a day and time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user");
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings").push();
        Map<String, Object> booking = new HashMap<>();
        booking.put("userId", userId);
        booking.put("providerId", providerId);
        booking.put("day", selectedDay);
        booking.put("timeSlot", selectedTimeSlot);
        booking.put("timestamp", System.currentTimeMillis());
        booking.put("status", "pending");

        bookingsRef.setValue(booking)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking confirmed: day=" + selectedDay + ", slot=" + selectedTimeSlot);
                    Toast.makeText(getContext(), "Booking confirmed for " + selectedDay + " at " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to confirm booking: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to confirm booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}