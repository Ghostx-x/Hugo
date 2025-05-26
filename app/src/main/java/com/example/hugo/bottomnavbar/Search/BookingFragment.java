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
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Profile.MyBookingsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        daySpinner = view.findViewById(R.id.day_spinner);
        timeSlotsRecyclerView = view.findViewById(R.id.time_slots_recycler_view);
        confirmBookingButton = view.findViewById(R.id.confirm_booking_button);

        timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        timeSlotAdapter = new TimeSlotAdapter(new ArrayList<>(), timeSlot -> {
            selectedTimeSlot = timeSlot;
            Log.d(TAG, "Selected time slot: " + timeSlot);
        });
        timeSlotsRecyclerView.setAdapter(timeSlotAdapter);

        providerId = getArguments() != null ? getArguments().getString(ARG_PROVIDER_ID) : null;
        if (providerId == null) {
            Log.w(TAG, "Invalid provider ID");
            Toast.makeText(getContext(), "Invalid provider", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        providerRef = FirebaseDatabase.getInstance().getReference("Users").child(providerId);
        loadProviderAvailability();

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

                ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, days);
                dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                daySpinner.setAdapter(dayAdapter);

                daySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        selectedDay = days.get(position);
                        List<String> slots = availability.get(selectedDay);
                        timeSlotAdapter.updateTimeSlots(slots != null ? slots : new ArrayList<>());
                        selectedTimeSlot = null;
                        Log.d(TAG, "Selected day: " + selectedDay);
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        selectedDay = null;
                        timeSlotAdapter.updateTimeSlots(new ArrayList<>());
                    }
                });

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
        String userEmail = user.getEmail();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                User currentUser = userSnapshot.getValue(User.class);
                if (currentUser == null) {
                    Log.w(TAG, "Current user not found");
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                providerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot providerSnapshot) {
                        User provider = providerSnapshot.getValue(User.class);
                        if (provider == null) {
                            Log.w(TAG, "Provider not found");
                            Toast.makeText(getContext(), "Provider not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Convert selectedDay to weekDay_monthName_dayOfMonth format
                        String formattedDate = formatDate(selectedDay);
                        if (formattedDate == null) {
                            Log.w(TAG, "Invalid date format: " + selectedDay);
                            Toast.makeText(getContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
                        String appointmentId = appointmentsRef.push().getKey();
                        Map<String, Object> appointment = new HashMap<>();
                        appointment.put("uniqueID", appointmentId);
                        appointment.put("userEmail", userEmail);
                        appointment.put("dogWalkerId", providerId);
                        appointment.put("serviceName", "Dog Walking");
                        appointment.put("weekDay_monthName_dayOfMonth", formattedDate);
                        appointment.put("time", selectedTimeSlot);
                        appointment.put("serviceDuration", "1 hr");
                        appointment.put("status", "Pending");

                        appointmentsRef.child(appointmentId).setValue(appointment)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Appointment saved: id=" + appointmentId + ", date=" + formattedDate + ", time=" + selectedTimeSlot);
                                    Toast.makeText(getContext(), "Booking confirmed for " + formattedDate + " at " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
                                    // Refresh MyBookingsFragment
                                    FragmentActivity activity = getActivity();
                                    if (activity instanceof FragmentActivity) {
                                        Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                                        if (fragment instanceof MyBookingsFragment) {
                                            ((MyBookingsFragment) fragment).loadBookings();
                                        }
                                    }
                                    getParentFragmentManager().popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save appointment: " + e.getMessage());
                                    Toast.makeText(getContext(), "Failed to confirm booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load provider data: " + error.getMessage());
                        Toast.makeText(getContext(), "Failed to load provider data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user data: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE MMMM d", Locale.US);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(inputFormat.parse(dateStr));
            return outputFormat.format(calendar.getTime());
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + dateStr, e);
            return null;
        }
    }
}