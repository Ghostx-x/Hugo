package com.example.hugo.bottomnavbar.Home;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hugo.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final String TAG = "ChatAdapter";
    private List<Chat> chats;
    private Context context;
    private OnChatClickListener onChatClickListener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatAdapter(List<Chat> chats, Context context, OnChatClickListener listener) {
        this.chats = chats;
        this.context = context;
        this.onChatClickListener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);
        if (chat == null) {
            Log.w(TAG, "Chat at position " + position + " is null");
            return;
        }

        // Set user name
        String userName = chat.getOtherUserName();
        holder.userName.setText(userName != null ? userName : "Unknown");

        // Display last message
        String lastMessage = chat.getLastMessage();
        if (lastMessage != null && !lastMessage.isEmpty()) {
            // Truncate long text messages, but not multimedia/location previews
            if (!lastMessage.equals("Sent an image") &&
                    !lastMessage.equals("Sent a video") &&
                    !lastMessage.equals("Shared a location")) {
                if (lastMessage.length() > 50) {
                    lastMessage = lastMessage.substring(0, 47) + "...";
                }
            }
            holder.lastMessage.setText(lastMessage);
            holder.lastMessage.setVisibility(View.VISIBLE);
        } else {
            holder.lastMessage.setText("No messages yet");
            holder.lastMessage.setVisibility(View.VISIBLE);
        }

        // Load circular profile image with Glide
        String profileImageBase64 = chat.getProfileImageBase64();
        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(profileImageBase64, Base64.DEFAULT);
                Glide.with(context)
                        .load(decodedBytes)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(holder.userImage);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load profile image: " + e.getMessage(), e);
                Glide.with(context)
                        .load(R.drawable.ic_profile)
                        .circleCrop()
                        .into(holder.userImage);
            }
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.userImage);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onChatClickListener != null) {
                onChatClickListener.onChatClick(chat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chats != null ? chats.size() : 0;
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView userName;
        TextView lastMessage;

        ChatViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            lastMessage = itemView.findViewById(R.id.last_message);

            // Validate views
            if (userImage == null || userName == null || lastMessage == null) {
                Log.e(TAG, "One or more views not found in item_chat: " +
                        "userImage=" + userImage + ", userName=" + userName + ", lastMessage=" + lastMessage);
            }
        }
    }
}