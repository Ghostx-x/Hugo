package com.example.hugo.bottomnavbar.Home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hugo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final String TAG = "MessageAdapter";
    private static final int VIEW_TYPE_SENT_TEXT = 1;
    private static final int VIEW_TYPE_RECEIVED_TEXT = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
    private static final int VIEW_TYPE_SENT_LOCATION = 5;
    private static final int VIEW_TYPE_RECEIVED_LOCATION = 6;

    private List<Message> messages;
    private Context context;
    private String currentUserId;

    public MessageAdapter(List<Message> messages, Context context, String currentUserId) {
        this.messages = messages;
        this.context = context;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message == null) {
            Log.w(TAG, "Message at position " + position + " is null");
            return VIEW_TYPE_SENT_TEXT; // Fallback
        }
        boolean isSent = message.getSenderId() != null && message.getSenderId().equals(currentUserId);
        String type = message.getType() != null ? message.getType() : "text";
        switch (type) {
            case "image":
                return isSent ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_RECEIVED_IMAGE;
            case "location":
                return isSent ? VIEW_TYPE_SENT_LOCATION : VIEW_TYPE_RECEIVED_LOCATION;
            default:
                return isSent ? VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType % 2 == 1 ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        if (message == null) {
            Log.w(TAG, "Message at position " + position + " is null");
            return;
        }

        holder.timestampText.setText(formatTimestamp(message.getTimestamp()));

        String type = message.getType() != null ? message.getType() : "text";
        switch (type) {
            case "image":
                holder.messageText.setVisibility(View.GONE);
                holder.mediaImage.setVisibility(View.VISIBLE);
                holder.locationText.setVisibility(View.GONE);
                String imageBase64 = message.getMediaBase64();
                if (imageBase64 != null && !imageBase64.isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                        Glide.with(context)
                                .load(decodedBytes)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(holder.mediaImage);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load image: " + e.getMessage(), e);
                        Glide.with(context)
                                .load(R.drawable.ic_profile)
                                .into(holder.mediaImage);
                    }
                } else {
                    Glide.with(context)
                            .load(R.drawable.ic_profile)
                            .into(holder.mediaImage);
                }
                holder.mediaImage.setOnClickListener(v -> {
                    if (imageBase64 != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("data:image/jpeg;base64," + imageBase64), "image/*");
                        try {
                            context.startActivity(Intent.createChooser(intent, "View Image"));
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open image: " + e.getMessage(), e);
                            Toast.makeText(context, "Cannot open image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case "location":
                holder.messageText.setVisibility(View.GONE);
                holder.mediaImage.setVisibility(View.GONE);
                holder.locationText.setVisibility(View.VISIBLE);
                holder.locationText.setText("Shared Location");
                holder.locationText.setOnClickListener(v -> {
                    Double lat = message.getLatitude();
                    Double lon = message.getLongitude();
                    if (lat != null && lon != null) {
                        Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        try {
                            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(mapIntent);
                            } else {
                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + lat + "," + lon)));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open location: " + e.getMessage(), e);
                            Toast.makeText(context, "Cannot open location", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Invalid location data", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            default:
                holder.messageText.setVisibility(View.VISIBLE);
                holder.mediaImage.setVisibility(View.GONE);
                holder.locationText.setVisibility(View.GONE);
                String text = message.getMessage();
                holder.messageText.setText(text != null ? text : "");
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestampText, locationText;
        ImageView mediaImage;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            mediaImage = itemView.findViewById(R.id.media_image);
            locationText = itemView.findViewById(R.id.location_text);

            if (messageText == null || timestampText == null || mediaImage == null || locationText == null) {
                Log.e(TAG, "One or more views not found in item_message: " +
                        "messageText=" + messageText + ", timestampText=" + timestampText +
                        ", mediaImage=" + mediaImage + ", locationText=" + locationText);
            }
        }
    }
}