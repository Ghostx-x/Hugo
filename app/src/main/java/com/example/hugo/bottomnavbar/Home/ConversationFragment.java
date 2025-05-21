package com.example.hugo.bottomnavbar.Home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ConversationFragment extends Fragment {

    private static final String ARG_OTHER_USER_ID = "other_user_id";
    private static final String ARG_OTHER_USER_NAME = "other_user_name";
    private static final String TAG = "ConversationFragment";

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private Button sendButton;
    private ImageView attachImageButton, attachVideoButton, shareLocationButton;
    private TextView otherUserNameText;
    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef;
    private FusedLocationProviderClient fusedLocationClient;
    private String otherUserId, otherUserName, chatId;

    // Activity result launchers for picking media and requesting permissions
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> pickVideoLauncher;

    public static ConversationFragment newInstance(String otherUserId, String otherUserName) {
        ConversationFragment fragment = new ConversationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OTHER_USER_ID, otherUserId);
        args.putString(ARG_OTHER_USER_NAME, otherUserName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_conversation, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate fragment_conversation: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to load chat UI", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return null;
        }

        try {
            messagesRecyclerView = view.findViewById(R.id.messages_recycler_view);
            messageInput = view.findViewById(R.id.message_input);
            sendButton = view.findViewById(R.id.send_button);
            attachImageButton = view.findViewById(R.id.attach_image_button);
            attachVideoButton = view.findViewById(R.id.attach_video_button);
            shareLocationButton = view.findViewById(R.id.share_location_button);
            otherUserNameText = view.findViewById(R.id.other_user_name);

            if (messagesRecyclerView == null || messageInput == null || sendButton == null ||
                    attachImageButton == null || attachVideoButton == null || shareLocationButton == null ||
                    otherUserNameText == null) {
                Log.e(TAG, "One or more views not found in fragment_conversation");
                Toast.makeText(getContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
                return view;
            }

            ImageView backArrow = view.findViewById(R.id.back_arrow);
            if (backArrow != null) {
                backArrow.setOnClickListener(v -> getParentFragmentManager().popBackStack());
            } else {
                Log.w(TAG, "back_arrow not found in fragment_conversation");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(getContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        mAuth = FirebaseAuth.getInstance();
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize activity result launchers
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "Permission granted");
            } else {
                Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    sendMedia(imageUri, "image");
                }
            }
        });

        pickVideoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                Uri videoUri = result.getData().getData();
                if (videoUri != null) {
                    sendMedia(videoUri, "video");
                }
            }
        });

        if (getArguments() != null) {
            otherUserId = getArguments().getString(ARG_OTHER_USER_ID);
            otherUserName = getArguments().getString(ARG_OTHER_USER_NAME);
            Log.d(TAG, "Received arguments: otherUserId=" + otherUserId + ", otherUserName=" + otherUserName);
        } else {
            Log.w(TAG, "No arguments provided");
            Toast.makeText(getContext(), "Invalid chat data", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Log.w(TAG, "Invalid otherUserId");
            Toast.makeText(getContext(), "Cannot load chat: Invalid user", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        if (otherUserName == null || otherUserName.isEmpty()) {
            otherUserName = "Unknown User";
            Log.w(TAG, "Using fallback otherUserName: " + otherUserName);
        }

        otherUserNameText.setText(otherUserName);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not signed in");
            Toast.makeText(getContext(), "Please sign in to chat", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        chatId = generateChatId(currentUser.getUid(), otherUserId);
        Log.d(TAG, "Generated chatId: " + chatId);

        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, getContext(), currentUser.getUid());
        messagesRecyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> sendTextMessage());
        attachImageButton.setOnClickListener(v -> requestMediaPermission("image"));
        attachVideoButton.setOnClickListener(v -> requestMediaPermission("video"));
        shareLocationButton.setOnClickListener(v -> requestLocationPermission());

        loadMessages();

        return view;
    }

    private String generateChatId(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) {
            Log.e(TAG, "Cannot generate chatId: userId1=" + userId1 + ", userId2=" + userId2);
            return "";
        }
        return userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    private void sendTextMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(getContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = chatsRef.child(chatId).child("messages").push().getKey();
        if (messageId == null) {
            Log.e(TAG, "Failed to generate messageId");
            Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = new Message(user.getUid(), "text", messageText, null, null, null, System.currentTimeMillis());
        sendMessageToFirebase(messageId, message);
    }

    private void requestMediaPermission(String mediaType) {
        String permission = android.os.Build.VERSION.SDK_INT >= 33 ?
                (mediaType.equals("image") ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_MEDIA_VIDEO) :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openMediaPicker(mediaType);
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openMediaPicker(String mediaType) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(mediaType.equals("image") ? "image/*" : "video/*");
        (mediaType.equals("image") ? pickImageLauncher : pickVideoLauncher).launch(intent);
    }

    private void sendMedia(Uri mediaUri, String mediaType) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = chatsRef.child(chatId).child("messages").push().getKey();
        if (messageId == null) {
            Log.e(TAG, "Failed to generate messageId");
            Toast.makeText(getContext(), "Failed to send " + mediaType, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String base64String;
            if (mediaType.equals("image")) {
                Bitmap bitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(mediaUri));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] bytes = baos.toByteArray();
                base64String = Base64.encodeToString(bytes, Base64.DEFAULT);
            } else {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(mediaUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                int maxSize = 5 * 1024 * 1024; // 5MB limit
                int totalBytes = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1 && totalBytes < maxSize) {
                    baos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                inputStream.close();
                if (totalBytes >= maxSize) {
                    Toast.makeText(getContext(), "Video too large (max 5MB)", Toast.LENGTH_SHORT).show();
                    return;
                }
                base64String = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            }

            Message message = new Message(user.getUid(), mediaType, null, base64String, null, null, System.currentTimeMillis());
            sendMessageToFirebase(messageId, message);
            Log.d(TAG, mediaType + " sent as Base64");
        } catch (Exception e) {
            Log.e(TAG, "Failed to process " + mediaType + ": " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to send " + mediaType, Toast.LENGTH_SHORT).show();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            shareLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void shareLocation() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            String messageId = chatsRef.child(chatId).child("messages").push().getKey();
                            if (messageId == null) {
                                Log.e(TAG, "Failed to generate messageId");
                                Toast.makeText(getContext(), "Failed to share location", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Message message = new Message(user.getUid(), "location", null, null, location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
                            sendMessageToFirebase(messageId, message);
                            Log.d(TAG, "Location shared: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            Toast.makeText(getContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Failed to share location", Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException getting location: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Location access denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessageToFirebase(String messageId, Message message) {
        chatsRef.child(chatId).child("messages").child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                    chatsRef.child(chatId).child("participants").setValue(new ArrayList<String>() {{
                        add(mAuth.getCurrentUser().getUid());
                        add(otherUserId);
                    }});
                    Log.d(TAG, "Message sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        if (chatId.isEmpty()) {
            Log.w(TAG, "Cannot load messages: Invalid chatId");
            Toast.makeText(getContext(), "Cannot load messages", Toast.LENGTH_SHORT).show();
            return;
        }

        chatsRef.child(chatId).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        messageList.add(message);
                    } else {
                        Log.w(TAG, "Failed to deserialize message");
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                }
                Log.d(TAG, "Loaded " + messageList.size() + " messages");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load messages: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

class Message {
    private String senderId;
    private String type; // "text", "image", "video", "location"
    private String message; // for text
    private String mediaBase64; // for image/video
    private Double latitude; // for location
    private Double longitude; // for location
    private long timestamp;

    public Message() {
        // Default constructor for Firebase
    }

    public Message(String senderId, String type, String message, String mediaBase64, Double latitude, Double longitude, long timestamp) {
        this.senderId = senderId;
        this.type = type;
        this.message = message;
        this.mediaBase64 = mediaBase64;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getMediaBase64() {
        return mediaBase64;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }
}