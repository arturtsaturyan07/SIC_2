package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final String currentUserId;
    private final Map<String, String> userNames;
    private final Map<String, String> userProfilePics;

    public ChatAdapter(List<ChatMessage> chatMessages, String currentUserId,
                       Map<String, String> userNames, Map<String, String> userProfilePics) {
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
        this.userNames = userNames;
        this.userProfilePics = userProfilePics;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        String senderId = chatMessage.getSenderId();
        boolean isSentByCurrentUser = senderId.equals(currentUserId);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(chatMessage.getTimestamp()));

        if (isSentByCurrentUser) {
            // Show self message
            holder.containerMe.setVisibility(View.VISIBLE);
            holder.containerOther.setVisibility(View.GONE);

            holder.messageTextMe.setText(chatMessage.getMessage());
            holder.timeTextMe.setText(formattedTime);

            // Set status indicator (✔️ / ✔️✔️)
            if (chatMessage.isRead()) {
                holder.statusIndicator.setText("✔️✔️");
            } else if (chatMessage.isDelivered()) {
                holder.statusIndicator.setText("✔️");
            } else {
                holder.statusIndicator.setText("");
            }

        } else {
            // Show received message
            holder.containerOther.setVisibility(View.VISIBLE);
            holder.containerMe.setVisibility(View.GONE);

            holder.senderNameOther.setText(userNames.getOrDefault(senderId, "User"));
            holder.messageTextOther.setText(chatMessage.getMessage());
            holder.timeTextOther.setText(formattedTime);

            String profileUrl = userProfilePics.get(senderId);
            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(profileUrl)
                        .placeholder(R.drawable.default_profile)
                        .into(holder.profileImageOther);
            } else {
                holder.profileImageOther.setImageResource(R.drawable.default_profile);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        // Sent by me
        LinearLayout containerMe;
        TextView messageTextMe;
        TextView timeTextMe;
        TextView statusIndicator;

        // Received from others
        LinearLayout containerOther;
        CircleImageView profileImageOther;
        TextView senderNameOther;
        TextView messageTextOther;
        TextView timeTextOther;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            // Sent by me
            containerMe = itemView.findViewById(R.id.message_container_me);
            messageTextMe = itemView.findViewById(R.id.message_text_me);
            timeTextMe = itemView.findViewById(R.id.time_text_me);
            statusIndicator = itemView.findViewById(R.id.status_indicator_me);

            // Received from other
            containerOther = itemView.findViewById(R.id.message_container_other);
            profileImageOther = itemView.findViewById(R.id.profile_image_other);
            senderNameOther = itemView.findViewById(R.id.sender_name_other);
            messageTextOther = itemView.findViewById(R.id.message_text_other);
            timeTextOther = itemView.findViewById(R.id.time_text_other);
        }
    }
}