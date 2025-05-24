package com.example.hugo.bottomnavbar;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String appointmentId = getInputData().getString("appointmentId");
        String ownerId = getInputData().getString("ownerId");
        String walkerId = getInputData().getString("walkerId");
        String date = getInputData().getString("date");
        String time = getInputData().getString("time");

        if (appointmentId != null && ownerId != null && walkerId != null) {
            sendReminderNotification(ownerId, walkerId, "Reminder: Booking starts in 15 minutes on " + date + " at " + time);
        }
        return Result.success();
    }

    private void sendReminderNotification(String ownerId, String walkerId, String message) {
        DatabaseReference ownerAlertsRef = FirebaseDatabase.getInstance().getReference("Users").child(ownerId).child("Alerts").push();
        DatabaseReference walkerAlertsRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId).child("Alerts").push();
        long timestamp = System.currentTimeMillis();
        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("timestamp", timestamp);
        ownerAlertsRef.setValue(notification);
        walkerAlertsRef.setValue(notification);
    }
}