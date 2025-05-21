package com.example.hugo.bottomnavbar.Home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.ViewProfileFragment;
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
    private ImageView attachButton, cameraButton;
    private ImageButton sendButton;
    private TextView otherUserNameText;
    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef;
    private FusedLocationProviderClient fusedLocationClient;
    private String otherUserId, otherUserName, chatId;

    // Activity result launchers for picking media, requesting permissions, and camera
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> capturePhotoLauncher;

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
            Log.e(TAG, "Exception stack trace: ", e);
            Toast.makeText(requireContext(), "Failed to load chat UI: " + e.getMessage(), Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return null;
        }

        try {
            messagesRecyclerView = view.findViewById(R.id.messages_recycler_view);
            messageInput = view.findViewById(R.id.message_input);
            sendButton = view.findViewById(R.id.send_button);
            attachButton = view.findViewById(R.id.attach_button);
            cameraButton = view.findViewById(R.id.camera_button);
            otherUserNameText = view.findViewById(R.id.other_user_name);

            if (messagesRecyclerView == null || messageInput == null || sendButton == null ||
                    attachButton == null || cameraButton == null || otherUserNameText == null) {
                Log.e(TAG, "One or more views not found in fragment_conversation: " +
                        "messagesRecyclerView=" + messagesRecyclerView +
                        ", messageInput=" + messageInput +
                        ", sendButton=" + sendButton +
                        ", attachButton=" + attachButton +
                        ", cameraButton=" + cameraButton +
                        ", otherUserNameText=" + otherUserNameText);
                Toast.makeText(requireContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "Exception stack trace: ", e);
            Toast.makeText(requireContext(), "UI initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
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

        capturePhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                if (photo != null) {
                    sendCameraPhoto(photo);
                } else {
                    Log.w(TAG, "Failed to capture photo: Bitmap is null");
                    Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (getArguments() != null) {
            otherUserId = getArguments().getString(ARG_OTHER_USER_ID);
            otherUserName = getArguments().getString(ARG_OTHER_USER_NAME);
            Log.d(TAG, "Received arguments: otherUserId=" + otherUserId + ", otherUserName=" + otherUserName);
        } else {
            Log.w(TAG, "No arguments provided");
            Toast.makeText(requireContext(), "Invalid chat data", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Log.w(TAG, "Invalid otherUserId");
            Toast.makeText(requireContext(), "Cannot load chat: Invalid user", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        if (otherUserName == null || otherUserName.isEmpty()) {
            otherUserName = "Unknown User";
            Log.w(TAG, "Using fallback otherUserName: " + otherUserName);
        }

        otherUserNameText.setText(otherUserName);
        otherUserNameText.setOnClickListener(v -> navigateToProfile());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not signed in");
            Toast.makeText(requireContext(), "Please sign in to chat", Toast.LENGTH_SHORT).show();
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
        attachButton.setOnClickListener(v -> showAttachmentMenu());
        cameraButton.setOnClickListener(v -> requestCameraPermission());

        loadMessages();

        return view;
    }

    private void navigateToProfile() {
        ViewProfileFragment viewProfileFragment = ViewProfileFragment.newInstance(otherUserId);
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, viewProfileFragment); // Replace with your container ID
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showAttachmentMenu() {
        String[] options = {"Share Image", "Share Location"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose an option");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Share Image
                    requestMediaPermission("image");
                    break;
                case 1: // Share Location
                    requestLocationPermission();
                    break;
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
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
            Toast.makeText(requireContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = chatsRef.child(chatId).child("messages").push().getKey();
        if (messageId == null) {
            Log.e(TAG, "Failed to generate messageId");
            Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = new Message(user.getUid(), "text", messageText, null, null, null, System.currentTimeMillis());
        sendMessageToFirebase(messageId, message);
    }

    private void requestMediaPermission(String mediaType) {
        String permission = android.os.Build.VERSION.SDK_INT >= 33 ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openMediaPicker(mediaType);
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openMediaPicker(String mediaType) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        try {
            pickImageLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open media picker for " + mediaType + ": " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Cannot open media picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            capturePhotoLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Cannot open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMedia(Uri mediaUri, String mediaType) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = chatsRef.child(chatId).child("messages").push().getKey();
        if (messageId == null) {
            Log.e(TAG, "Failed to generate messageId");
            Toast.makeText(requireContext(), "Failed to send " + mediaType, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(mediaUri));
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image from URI: " + mediaUri);
                Toast.makeText(requireContext(), "Invalid image file", Toast.LENGTH_SHORT).show();
                return;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] bytes = baos.toByteArray();
            String base64String = Base64.encodeToString(bytes, Base64.DEFAULT);
            Log.d(TAG, "Image Base64 size: " + (bytes.length / 1024) + " KB");

            Message message = new Message(user.getUid(), mediaType, null, base64String, null, null, System.currentTimeMillis());
            sendMessageToFirebase(messageId, message);
            Log.d(TAG, "Image sent successfully, Base64 length: " + base64String.length());
        } catch (Exception e) {
            Log.e(TAG, "Failed to process " + mediaType + ": " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to send " + mediaType, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCameraPhoto(Bitmap photo) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = chatsRef.child(chatId).child("messages").push().getKey();
        if (messageId == null) {
            Log.e(TAG, "Failed to generate messageId");
            Toast.makeText(requireContext(), "Failed to send photo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] bytes = baos.toByteArray();
            String base64String = Base64.encodeToString(bytes, Base64.DEFAULT);
            Log.d(TAG, "Camera photo Base64 size: " + (bytes.length / 1024) + " KB");

            Message message = new Message(user.getUid(), "image", null, base64String, null, null, System.currentTimeMillis());
            sendMessageToFirebase(messageId, message);
            Log.d(TAG, "Camera photo sent successfully, Base64 length: " + base64String.length());
        } catch (Exception e) {
            Log.e(TAG, "Failed to process camera photo: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to send photo", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            String messageId = chatsRef.child(chatId).child("messages").push().getKey();
                            if (messageId == null) {
                                Log.e(TAG, "Failed to generate messageId");
                                Toast.makeText(requireContext(), "Failed to share location", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Message message = new Message(user.getUid(), "location", null, null, location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
                            sendMessageToFirebase(messageId, message);
                            Log.d(TAG, "Location shared: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Failed to share location", Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException getting location: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Location access denied", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        if (chatId.isEmpty()) {
            Log.w(TAG, "Cannot load messages: Invalid chatId");
            Toast.makeText(requireContext(), "Cannot load messages", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Failed to load messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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