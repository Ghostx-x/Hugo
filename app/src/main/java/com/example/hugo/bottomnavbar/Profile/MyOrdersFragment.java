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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MyOrdersFragment extends Fragment implements OrderAdapter.OnStatusUpdateListener {

    private RecyclerView ordersRecyclerView;
    private OrderAdapter adapter;
    private List<Notification> orderList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_orders, container, false);

        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderList = new ArrayList<>();
        adapter = new OrderAdapter(orderList, getContext(), this);
        ordersRecyclerView.setAdapter(adapter);

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

        loadOrders();
        return view;
    }

    private void loadOrders() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("Notifications")
                .child(user.getUid());
        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    String notificationId = notificationSnapshot.getKey();
                    String bookingId = notificationSnapshot.child("bookingId").getValue(String.class);
                    String userId = notificationSnapshot.child("userId").getValue(String.class);
                    String userName = notificationSnapshot.child("userName").getValue(String.class);
                    String photoBase64 = notificationSnapshot.child("userPhotoBase64").getValue(String.class);
                    String bookedTime = notificationSnapshot.child("bookedTime").getValue(String.class);
                    String status = notificationSnapshot.child("status").getValue(String.class);
                    if (bookingId != null && userId != null && userName != null && bookedTime != null && status != null) {
                        orderList.add(new Notification(notificationId, bookingId, userId, userName, photoBase64 != null ? photoBase64 : "", bookedTime, status));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStatusUpdate(String notificationId, String bookingId, String userId, String status) {
        // Find the Notification object to get bookedTime
        String bookedTime = null;
        for (Notification notification : orderList) {
            if (notification.getNotificationId().equals(notificationId)) {
                bookedTime = notification.getBookedTime();
                break;
            }
        }

        if (bookedTime == null) {
            Toast.makeText(getContext(), "Error: Booking time not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("Notifications")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(notificationId);
        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                .getReference("Bookings")
                .child(userId)
                .child(bookingId);

        String finalBookedTime = bookedTime; // For use in lambda expressions
        notificationRef.child("status").setValue(status)
                .addOnSuccessListener(aVoid -> {
                    bookingRef.child("status").setValue(status)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(), "Booking " + status, Toast.LENGTH_SHORT).show();
                                if (status.equals("Accepted")) {
                                    sendAcceptanceNotification(bookingId, userId, finalBookedTime);
                                    scheduleReminderNotification(bookingId, userId, FirebaseAuth.getInstance().getCurrentUser().getUid(), finalBookedTime);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to update booking status", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update notification status", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendAcceptanceNotification(String bookingId, String ownerId, String bookedTime) {
        DatabaseReference userAlertsRef = FirebaseDatabase.getInstance().getReference("Users").child(ownerId).child("Alerts").push();
        Map<String, Object> notification = new HashMap<>();
        notification.put("message", "Your booking for " + bookedTime + " has been accepted!");
        notification.put("timestamp", System.currentTimeMillis());
        userAlertsRef.setValue(notification).addOnSuccessListener(aVoid -> {
            // Notification sent successfully
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to send acceptance notification", Toast.LENGTH_SHORT).show();
        });
    }

    private void scheduleReminderNotification(String bookingId, String ownerId, String walkerId, String bookedTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date bookedDate = sdf.parse(bookedTime);
            if (bookedDate != null) {
                long reminderTime = bookedDate.getTime() - 15 * 60 * 1000; // 15 minutes before
                long delay = reminderTime - System.currentTimeMillis();
                if (delay > 0) {
                    Data inputData = new Data.Builder()
                            .putString("appointmentId", bookingId)
                            .putString("ownerId", ownerId)
                            .putString("walkerId", walkerId)
                            .putString("date", bookedTime.split(" ")[0])
                            .putString("time", bookedTime.split(" ")[1])
                            .build();

                    OneTimeWorkRequest reminderRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(inputData)
                            .build();

                    WorkManager.getInstance(requireContext()).enqueue(reminderRequest);
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to schedule reminder", Toast.LENGTH_SHORT).show();
        }
    }
}

class Notification {
    public String notificationId;
    public String bookingId;
    public String userId;
    public String userName;
    public String userPhotoBase64;
    public String bookedTime;
    public String status;

    public Notification() {
        // Default constructor for Firebase
    }

    public Notification(String notificationId, String bookingId, String userId, String userName, String userPhotoBase64, String bookedTime, String status) {
        this.notificationId = notificationId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.userName = userName;
        this.userPhotoBase64 = userPhotoBase64;
        this.bookedTime = bookedTime;
        this.status = status;
    }

    // Getters and setters
    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhotoBase64() {
        return userPhotoBase64;
    }

    public void setUserPhotoBase64(String userPhotoBase64) {
        this.userPhotoBase64 = userPhotoBase64;
    }

    public String getBookedTime() {
        return bookedTime;
    }

    public void setBookedTime(String bookedTime) {
        this.bookedTime = bookedTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}