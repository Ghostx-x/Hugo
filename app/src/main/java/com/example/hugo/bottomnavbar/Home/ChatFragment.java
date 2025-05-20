package com.example.hugo.bottomnavbar.Home;

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

    private RecyclerView chatsRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList;
    private FirebaseAuth mAuth;
    private DatabaseReference chatsRef, usersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatsRecyclerView = view.findViewById(R.id.chats_recycler_view);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList, getContext(), chat -> {
            // Navigate to ConversationFragment
            ConversationFragment conversationFragment = ConversationFragment.newInstance(chat.getOtherUserId(), chat.getOtherUserName());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, conversationFragment)
                    .addToBackStack(null)
                    .commit();
        });
        chatsRecyclerView.setAdapter(chatAdapter);

        ImageView backArrow = view.findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadChats();

        return view;
    }

    private void loadChats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = user.getUid();
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    DataSnapshot participantsSnapshot = chatSnapshot.child("participants");
                    List<String> participants = new ArrayList<>();
                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                        participants.add(participant.getValue(String.class));
                    }

                    if (participants.contains(currentUserId)) {
                        String otherUserId = participants.get(0).equals(currentUserId) ? participants.get(1) : participants.get(0);
                        usersRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    String otherUserName = userSnapshot.child("name").getValue(String.class);
                                    String profileImageBase64 = userSnapshot.child("profileImageBase64").getValue(String.class);
                                    Chat chat = new Chat(chatId, otherUserId, otherUserName != null ? otherUserName : "Unknown", profileImageBase64);
                                    chatList.add(chat);
                                    chatAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load chats", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

class Chat {
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String profileImageBase64;

    public Chat(String chatId, String otherUserId, String otherUserName, String profileImageBase64) {
        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.profileImageBase64 = profileImageBase64;
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
}