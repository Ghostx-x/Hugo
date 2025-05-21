package com.example.hugo.bottomnavbar.Home;

import android.os.Bundle;
import android.util.Log;
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

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private RecyclerView chatsRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList;
    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef, usersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_chat, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate fragment_chat: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to load chat UI", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            chatsRecyclerView = view.findViewById(R.id.chats_recycler_view);
            if (chatsRecyclerView == null) {
                Log.e(TAG, "chats_recycler_view not found in fragment_chat");
                Toast.makeText(getContext(), "UI error: RecyclerView not found", Toast.LENGTH_SHORT).show();
                return view;
            }
            chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            chatList = new ArrayList<>();
            chatAdapter = new ChatAdapter(chatList, getContext(), chat -> {
                if (chat != null && chat.getOtherUserId() != null) {
                    ConversationFragment conversationFragment = ConversationFragment.newInstance(
                            chat.getOtherUserId(),
                            chat.getOtherUserName() != null ? chat.getOtherUserName() : "Unknown"
                    );
                    try {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, conversationFragment)
                                .addToBackStack(null)
                                .commit();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to navigate to ConversationFragment: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Failed to open chat", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Invalid chat or otherUserId");
                    Toast.makeText(getContext(), "Cannot open chat: Invalid user", Toast.LENGTH_SHORT).show();
                }
            });
            chatsRecyclerView.setAdapter(chatAdapter);

            ImageView backArrow = view.findViewById(R.id.back_arrow);
            if (backArrow != null) {
                backArrow.setOnClickListener(v -> {
                    try {
                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack();
                        } else {
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, new HomeFragment())
                                    .commit();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to handle back navigation: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Navigation error", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.w(TAG, "back_arrow not found in fragment_chat");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            Toast.makeText(getContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
            return view;
        }

        mAuth = FirebaseAuth.getInstance();
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadChats();

        return view;
    }

    private void loadChats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not signed in");
            Toast.makeText(getContext(), "Please sign in to view chats", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = user.getUid();
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId == null) {
                        Log.w(TAG, "Chat ID is null");
                        continue;
                    }

                    DataSnapshot participantsSnapshot = chatSnapshot.child("participants");
                    List<String> participants = new ArrayList<>();
                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                        String participantId = participant.getValue(String.class);
                        if (participantId != null) {
                            participants.add(participantId);
                        }
                    }

                    if (participants.size() < 2 || !participants.contains(currentUserId)) {
                        Log.w(TAG, "Invalid participants for chatId: " + chatId + ", participants: " + participants);
                        continue;
                    }

                    String otherUserId = participants.get(0).equals(currentUserId) ? participants.get(1) : participants.get(0);
                    usersRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (!userSnapshot.exists()) {
                                Log.w(TAG, "User data not found for userId: " + otherUserId);
                                return;
                            }

                            String otherUserName = userSnapshot.child("name").getValue(String.class);
                            String profileImageBase64 = userSnapshot.child("profileImageBase64").getValue(String.class);

                            chatsRef.child(chatId).child("messages")
                                    .orderByChild("timestamp")
                                    .limitToLast(1)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot messageSnapshot) {
                                            String lastMessage = null;
                                            Long lastMessageTimestamp = null;
                                            for (DataSnapshot msg : messageSnapshot.getChildren()) {
                                                Message message = msg.getValue(Message.class);
                                                if (message != null) {
                                                    switch (message.getType() != null ? message.getType() : "") {
                                                        case "image":
                                                            lastMessage = "Sent an image";
                                                            break;
                                                        case "video":
                                                            lastMessage = "Sent a video";
                                                            break;
                                                        case "location":
                                                            lastMessage = "Shared a location";
                                                            break;
                                                        default:
                                                            lastMessage = message.getMessage();
                                                            break;
                                                    }
                                                    lastMessageTimestamp = message.getTimestamp();
                                                } else {
                                                    Log.w(TAG, "Failed to deserialize message for chatId: " + chatId);
                                                }
                                            }

                                            Chat chat = new Chat(
                                                    chatId,
                                                    otherUserId,
                                                    otherUserName != null ? otherUserName : "Unknown",
                                                    profileImageBase64,
                                                    lastMessage,
                                                    lastMessageTimestamp
                                            );
                                            chatList.add(chat);
                                            chatAdapter.notifyDataSetChanged();
                                            Log.d(TAG, "Added chat: " + chatId + ", otherUser: " + otherUserName);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, "Failed to load last message for chatId: " + chatId + ", error: " + error.getMessage());
                                            Toast.makeText(getContext(), "Failed to load last message", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to load user data for userId: " + otherUserId + ", error: " + error.getMessage());
                            Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chats: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
class Chat {
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String profileImageBase64;
    private String lastMessage;
    private Long lastMessageTimestamp;

    public Chat(String chatId, String otherUserId, String otherUserName, String profileImageBase64, String lastMessage, Long lastMessageTimestamp) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.profileImageBase64 = profileImageBase64;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public Long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }
}