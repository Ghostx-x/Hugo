package com.example.hugo.bottomnavbar.Profile;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.TimeSlotAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingDialog extends Dialog {

    private static final String TAG = "BookingDialog";
    private TextView selectedDateText;
    private CalendarView calendarView;
    private RecyclerView timeSlotsRecyclerView;
    private Button bookButton;
    private TimeSlotAdapter timeSlotAdapter;
    private List<String> availableTimeSlots;
    private String selectedDateStr;
    private String selectedTimeSlot;
    private String dogWalkerId;
    private Map<String, List<String>> dogWalkerAvailability;
    private OnBookingConfirmedListener bookingConfirmedListener;

    public BookingDialog(Context context, String dogWalkerId, OnBookingConfirmedListener listener) {
        super(context);
        this.dogWalkerId = dogWalkerId;
        this.bookingConfirmedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_booking, null);
        setContentView(view);

        selectedDateText = view.findViewById(R.id.selected_date);
        calendarView = view.findViewById(R.id.calendar_view);
        timeSlotsRecyclerView = view.findViewById(R.id.time_slots_recycler_view);
        bookButton = view.findViewById(R.id.book_button);

        timeSlotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        availableTimeSlots = new ArrayList<>();
        timeSlotAdapter = new TimeSlotAdapter(availableTimeSlots, (timeSlot) -> selectedTimeSlot = timeSlot);
        timeSlotsRecyclerView.setAdapter(timeSlotAdapter);

        Calendar calendar = Calendar.getInstance();
        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        updateDateVariables(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            updateDateVariables(year, month, dayOfMonth);
            loadTimeSlotsForDate(year, month, dayOfMonth);
        });

        bookButton.setOnClickListener(v -> bookAppointment());

        loadDogWalkerAvailability();
    }

    @SuppressLint("SetTextI18n")
    private void updateDateVariables(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        String weekDay = dayFormat.format(calendar.getTime());
        String monthName = monthFormat.format(calendar.getTime());
        String dayOfMonthStr = String.valueOf(dayOfMonth);
        selectedDateStr = dateFormat.format(calendar.getTime());

        selectedDateText.setText(weekDay + " " + monthName + " " + dayOfMonthStr);
    }

    private void loadDogWalkerAvailability() {
        DatabaseReference dogWalkerRef = FirebaseDatabase.getInstance().getReference("Users").child(dogWalkerId);
        dogWalkerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<Map<String, List<String>>> availabilityType =
                        new GenericTypeIndicator<Map<String, List<String>>>() {};
                dogWalkerAvailability = snapshot.child("availability").getValue(availabilityType);
                if (dogWalkerAvailability == null) {
                    dogWalkerAvailability = new HashMap<>();
                }
                Calendar calendar = Calendar.getInstance();
                loadTimeSlotsForDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load availability", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTimeSlotsForDate(int year, int month, int dayOfMonth) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth);
        String dateStr = sdf.format(calendar.getTime());

        availableTimeSlots.clear();
        if (dogWalkerAvailability != null && dogWalkerAvailability.containsKey(dateStr)) {
            List<String> slots = dogWalkerAvailability.get(dateStr);
            if (slots != null && !slots.isEmpty()) {
                availableTimeSlots.addAll(slots);
                Log.d(TAG, "Time slots for " + dateStr + ": " + slots);
            } else {
                Log.d(TAG, "No time slots available for " + dateStr);
            }
        } else {
            Log.d(TAG, "No availability data for " + dateStr);
        }
        timeSlotAdapter.notifyDataSetChanged();
    }

    private void bookAppointment() {
        if (selectedTimeSlot == null) {
            Toast.makeText(getContext(), "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in to book", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Saving booking for user: " + user.getEmail() + ", walker: " + dogWalkerId);
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        String bookingId = bookingsRef.push().getKey();
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("uniqueID", bookingId);
        appointment.put("userEmail", user.getEmail());
        appointment.put("dogWalkerId", dogWalkerId);
        appointment.put("serviceName", "Dog Walking");
        appointment.put("weekDay_monthName_dayOfMonth", selectedDateText.getText().toString());
        appointment.put("time", selectedTimeSlot);
        appointment.put("serviceDuration", "1 hr");
        appointment.put("status", "Pending");

        if (bookingId != null) {
            bookingsRef.child(bookingId).setValue(appointment)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Booking saved successfully: " + bookingId);
                        sendNotification(user.getUid(), dogWalkerId, "Booking requested for " +
                                selectedDateText.getText().toString() + " at " + selectedTimeSlot);
                        Toast.makeText(getContext(), "Booking request sent", Toast.LENGTH_SHORT).show();
                        if (bookingConfirmedListener != null) {
                            bookingConfirmedListener.onBookingConfirmed(selectedDateText.getText().toString(), selectedTimeSlot);
                        }
                        dismiss();
                        // Refresh MyBookingsFragment
                        if (getContext() instanceof androidx.fragment.app.FragmentActivity) {
                            androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) getContext();
                            androidx.fragment.app.Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                            if (fragment instanceof MyBookingsFragment) {
                                ((MyBookingsFragment) fragment).loadBookings();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save booking: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to send booking request", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendNotification(String ownerId, String walkerId, String message) {
        DatabaseReference ownerAlertsRef = FirebaseDatabase.getInstance().getReference("Users").child(ownerId).child("Alerts").push();
        DatabaseReference walkerAlertsRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId).child("Alerts").push();
        long timestamp = System.currentTimeMillis();
        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("timestamp", timestamp);
        ownerAlertsRef.setValue(notification);
        walkerAlertsRef.setValue(notification);
    }

    public interface OnBookingConfirmedListener {
        void onBookingConfirmed(String date, String time);
    }
}