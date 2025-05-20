package com.example.hugo.bottomnavbar.Home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

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
        holder.userName.setText(chat.getOtherUserName());

        if (chat.getProfileImageBase64() != null && !chat.getProfileImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(chat.getProfileImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                Bitmap circularBitmap = getCircularBitmap(bitmap);
                holder.userImage.setImageBitmap(circularBitmap);
            } catch (Exception e) {
                Log.e("ChatAdapter", "Failed to load profile image: " + e.getMessage(), e);
                holder.userImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.userImage.setImageResource(R.drawable.ic_profile);
        }

        holder.itemView.setOnClickListener(v -> onChatClickListener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    // Helper method to transform a bitmap into a circular shape
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(android.graphics.Color.WHITE);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView userName;

        ChatViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
        }
    }
}