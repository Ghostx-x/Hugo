package com.example.hugo.bottomnavbar.Home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

public class ConversationFragment extends Fragment {

    private static final String ARG_OTHER_USER_ID = "other_user_id";
    private static final String ARG_OTHER_USER_NAME = "other_user_name";
    private static final String TAG = "ConversationFragment";

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private Button sendButton;
    private TextView otherUserNameText;
    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef;
    private String otherUserId, otherUserName, chatId;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_conversation, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate layout: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to load chat UI", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return null;
        }

        try {
            messagesRecyclerView = view.findViewById(R.id.messages_recycler_view);
            messageInput = view.findViewById(R.id.message_input);
            sendButton = view.findViewById(R.id.send_button);
            otherUserNameText = view.findViewById(R.id.other_user_name);

            ImageView backArrow = view.findViewById(R.id.back_arrow);
            backArrow.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(getContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        mAuth = FirebaseAuth.getInstance();
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

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

        sendButton.setOnClickListener(v -> sendMessage());

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

    private void sendMessage() {
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

        Message message = new Message(user.getUid(), messageText, System.currentTimeMillis());

        chatsRef.child(chatId).child("messages").child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                    chatsRef.child(chatId).child("participants").setValue(new ArrayList<String>() {{
                        add(user.getUid());
                        add(otherUserId);
                    }});
                    Log.d(TAG, "Message sent successfully: " + messageText);
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
    private String message;
    private long timestamp;

    public Message() {
        // Default constructor for Firebase
    }

    public Message(String senderId, String message, long timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}