package com.example.hugo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CHANNEL_NAME = "Chat Notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received: " + remoteMessage.getData());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            String senderId = remoteMessage.getData().get("senderId");
            String senderName = remoteMessage.getData().get("senderName");
            String messageText = remoteMessage.getData().get("messageText");

            if (senderId != null && senderName != null && messageText != null) {
                sendNotification(senderId, senderName, messageText);
            } else {
                Log.w(TAG, "Missing data in message payload");
            }
        }
    }

    private void sendNotification(String senderId, String senderName, String messageText) {
        // Create an intent to open ConversationFragment when the notification is tapped
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigateTo", "ConversationFragment");
        intent.putExtra("otherUserId", senderId);
        intent.putExtra("otherUserName", senderName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Chat message notifications");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this drawable exists
                .setContentTitle("New Message from " + senderName)
                .setContentText(messageText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }

        // Update unread message count in SharedPreferences
        updateUnreadMessageCount(senderId);
    }

    private void updateUnreadMessageCount(String senderId) {
        // Use SharedPreferences to store unread message counts per sender
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        int currentCount = prefs.getInt("unread_" + senderId, 0);
        editor.putInt("unread_" + senderId, currentCount + 1);
        editor.putBoolean("hasUnreadMessages", true); // Flag for HomeFragment badge
        editor.apply();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        // Send token to your server if needed
    }
}