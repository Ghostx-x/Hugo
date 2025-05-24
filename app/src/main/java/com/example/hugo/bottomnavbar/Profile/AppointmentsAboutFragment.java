package com.example.hugo.bottomnavbar.Profile;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.ReminderWorker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppointmentsAboutFragment extends Fragment {

    private static final String TAG = "AppointmentsAbout";
    public static String weekDay_monthName_dayOfMonth_str;
    public static String Time_str;
    public static String ServiceName_str;

    private TextView dogWalkerNameText, statusText, weekDayMonthNameDayOfMonth, serviceDuration, dogWalkerAddress, serviceName, servicePrice, serviceDuration1;
    private ImageView dogWalkerImage;
    private Button bookAppointmentButton;
    private String appointmentId;
    private DatabaseReference appointmentRef;
    private String dogWalkerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointments_about, container, false);

        dogWalkerImage = view.findViewById(R.id.dog_walker_image);
        dogWalkerNameText = view.findViewById(R.id.dog_walker_name);
        statusText = view.findViewById(R.id.status);
        weekDayMonthNameDayOfMonth = view.findViewById(R.id.date_text);
        serviceDuration = view.findViewById(R.id.ServiceDuration);
        dogWalkerAddress = view.findViewById(R.id.dog_walker_adress);
        serviceName = view.findViewById(R.id.ServiceName);
        servicePrice = view.findViewById(R.id.ServicePrice);
        serviceDuration1 = view.findViewById(R.id.ServiceDuration1);
        bookAppointmentButton = view.findViewById(R.id.book_appointment_button);

        // Load Dog Walker details from arguments
        Bundle args = getArguments();
        if (args != null) {
            String dogWalkerName = args.getString("BarberShopName");
            dogWalkerId = args.getString("dogWalkerId");
            appointmentId = args.getString("appointmentId");
            weekDay_monthName_dayOfMonth_str = args.getString("weekDay_monthName_dayOfMonth");
            Time_str = args.getString("Time");
            String serviceDurationStr = args.getString("ServiceDuration");
            String address = args.getString("BarberShopAddress");
            ServiceName_str = args.getString("ServiceName");

            dogWalkerNameText.setText(dogWalkerName);
            weekDayMonthNameDayOfMonth.setText(weekDay_monthName_dayOfMonth_str != null ? weekDay_monthName_dayOfMonth_str : "Not Scheduled");
            serviceDuration.setText(serviceDurationStr);
            serviceDuration1.setText(serviceDurationStr);
            dogWalkerAddress.setText(address);
            serviceName.setText(ServiceName_str);
        }

        // Load price from Firebase
        DatabaseReference dogWalkerRef = FirebaseDatabase.getInstance().getReference("Users").child(dogWalkerId);
        dogWalkerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Float price = snapshot.child("pricePerHour").getValue(Float.class);
                if (price != null) {
                    servicePrice.setText("USD " + price);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load price", error.toException());
            }
        });

        bookAppointmentButton.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .addToBackStack(null)
                        .commit();
                return;
            }

            BookingDialog dialog = new BookingDialog(getContext(), dogWalkerId, (date, time) -> {
                weekDay_monthName_dayOfMonth_str = date;
                Time_str = time;
                weekDayMonthNameDayOfMonth.setText(weekDay_monthName_dayOfMonth_str);
                scheduleReminder(date, time, user.getUid(), dogWalkerId);
            });
            dialog.show();
        });

        // Listen for status changes
        appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        appointmentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String statusValue = snapshot.child("status").getValue(String.class);
                statusText.setText(statusValue != null ? statusValue : "Pending");
                if ("Accepted".equals(statusValue)) {
                    sendAcceptanceNotification(FirebaseAuth.getInstance().getCurrentUser().getUid(), dogWalkerId, weekDay_monthName_dayOfMonth_str, Time_str);
                }
                checkBookingEndTime();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load appointment status", error.toException());
            }
        });

        return view;
    }

    private void scheduleReminder(String dateStr, String timeStr, String ownerId, String walkerId) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE MMMM d yyyy HH:mm", Locale.US);
        try {
            Date date = sdf.parse(dateStr + " " + timeStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MINUTE, -15); // 15 minutes before
            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
            if (delay > 0) {
                Data inputData = new Data.Builder()
                        .putString("appointmentId", appointmentId)
                        .putString("ownerId", ownerId)
                        .putString("walkerId", walkerId)
                        .putString("date", dateStr)
                        .putString("time", timeStr)
                        .build();
                OneTimeWorkRequest reminderWork = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(inputData)
                        .build();
                WorkManager.getInstance(requireContext()).enqueue(reminderWork);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date/time", e);
        }
    }

    private void sendAcceptanceNotification(String ownerId, String walkerId, String date, String time) {
        String message = "Booking confirmed for " + date + " at " + time + " with Dog Walker";
        DatabaseReference ownerAlertsRef = FirebaseDatabase.getInstance().getReference("Users").child(ownerId).child("Alerts").push();
        DatabaseReference walkerAlertsRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId).child("Alerts").push();
        long timestamp = System.currentTimeMillis();
        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("timestamp", timestamp);
        ownerAlertsRef.setValue(notification);
        walkerAlertsRef.setValue(notification);
    }

    private void checkBookingEndTime() {
        String dateTimeStr = weekDay_monthName_dayOfMonth_str + " " + Time_str;
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE MMMM d yyyy HH:mm", Locale.US);
        try {
            Date startTime = sdf.parse(dateTimeStr);
            Calendar endTime = Calendar.getInstance();
            endTime.setTime(startTime);
            endTime.add(Calendar.HOUR, 1); // 1 hour duration
            if (System.currentTimeMillis() >= endTime.getTimeInMillis()) {
                PaymentFragment fragment = new PaymentFragment();
                Bundle args = new Bundle();
                args.putString("appointmentId", appointmentId);
                fragment.setArguments(args);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date/time", e);
        }
    }
}