package com.example.sic_2;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ChatAdapter for displaying chat messages, supporting image messages, reactions, and long-press menu.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final String currentUserId;
    private final Map<String, String> userNames;
    private final Map<String, String> userProfilePics;
    private final Context context;
    private final OnMessageLongClickListener longClickListener;

    public ChatAdapter(Context context, List<ChatMessage> chatMessages, String currentUserId,
                       Map<String, String> userNames, Map<String, String> userProfilePics,
                       OnMessageLongClickListener longClickListener) {
        this.context = context;
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
        this.userNames = userNames;
        this.userProfilePics = userProfilePics;
        this.longClickListener = longClickListener;
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

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(chatMessage.getTimestamp()));

        // Hide all containers first
        holder.containerMe.setVisibility(View.GONE);
        holder.containerOther.setVisibility(View.GONE);

        // Sent by me
        if (isSentByCurrentUser) {
            holder.containerMe.setVisibility(View.VISIBLE);
            holder.messageTextMe.setVisibility(View.GONE);
            holder.messageImageMe.setVisibility(View.GONE);

            // Image message
            if (chatMessage.getImageUrl() != null && !chatMessage.getImageUrl().isEmpty()) {
                holder.messageImageMe.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chatMessage.getImageUrl())
                        .placeholder(R.drawable.image_placeholder)
                        .into(holder.messageImageMe);
                // Download image on long-press
                holder.messageImageMe.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(chatMessage, position);
                        return true;
                    }
                    return false;
                });
            }
            // Text message
            if (chatMessage.getMessage() != null && !chatMessage.getMessage().isEmpty()) {
                holder.messageTextMe.setVisibility(View.VISIBLE);
                holder.messageTextMe.setText(chatMessage.getMessage());
            }
            holder.timeTextMe.setText(formattedTime);

            // Reaction
            if (chatMessage.getReaction() != null && !chatMessage.getReaction().isEmpty()) {
                holder.reactionMe.setVisibility(View.VISIBLE);
                holder.reactionMe.setText(chatMessage.getReaction());
            } else {
                holder.reactionMe.setVisibility(View.GONE);
            }

            if (chatMessage.isReadByUser(currentUserId)) {
                holder.statusIndicator.setText("✔️✔️");
            } else if (chatMessage.isDeliveredByUser(currentUserId)) {
                holder.statusIndicator.setText("✔️");
            } else {
                holder.statusIndicator.setText("");
            }
        } else {
            // Received from others
            holder.containerOther.setVisibility(View.VISIBLE);
            holder.messageTextOther.setVisibility(View.GONE);
            holder.messageImageOther.setVisibility(View.GONE);

            String name = userNames.getOrDefault(senderId, "User");
            holder.senderNameOther.setText(name);

            // Text message
            if (chatMessage.getMessage() != null && !chatMessage.getMessage().isEmpty()) {
                holder.messageTextOther.setVisibility(View.VISIBLE);
                holder.messageTextOther.setText(chatMessage.getMessage());
            }
            // Image message
            if (chatMessage.getImageUrl() != null && !chatMessage.getImageUrl().isEmpty()) {
                holder.messageImageOther.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chatMessage.getImageUrl())
                        .placeholder(R.drawable.image_placeholder)
                        .into(holder.messageImageOther);
                holder.messageImageOther.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(chatMessage, position);
                        return true;
                    }
                    return false;
                });
            }
            holder.timeTextOther.setText(formattedTime);

            // Reaction
            if (chatMessage.getReaction() != null && !chatMessage.getReaction().isEmpty()) {
                holder.reactionOther.setVisibility(View.VISIBLE);
                holder.reactionOther.setText(chatMessage.getReaction());
            } else {
                holder.reactionOther.setVisibility(View.GONE);
            }

            String profileUrl = userProfilePics.get(senderId);
            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(context)
                        .load(profileUrl)
                        .placeholder(R.drawable.default_profile)
                        .into(holder.profileImageOther);
            } else {
                holder.profileImageOther.setImageResource(R.drawable.default_profile);
            }

            holder.profileImageOther.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", senderId);
                context.startActivity(intent);
            });
        }

        // Long-press support for each bubble
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(chatMessage, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        // Sent by me
        LinearLayout containerMe;
        TextView messageTextMe;
        ImageView messageImageMe;
        TextView timeTextMe;
        TextView statusIndicator;
        TextView reactionMe;

        // Received from others
        LinearLayout containerOther;
        ImageView profileImageOther;
        TextView senderNameOther;
        TextView messageTextOther;
        ImageView messageImageOther;
        TextView timeTextOther;
        TextView reactionOther;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            // Sent by me
            containerMe = itemView.findViewById(R.id.message_container_me);
            messageTextMe = itemView.findViewById(R.id.message_text_me);
            messageImageMe = itemView.findViewById(R.id.message_image_me);
            timeTextMe = itemView.findViewById(R.id.time_text_me);
            statusIndicator = itemView.findViewById(R.id.status_indicator_me);
            reactionMe = itemView.findViewById(R.id.reaction_me);

            // Received from other
            containerOther = itemView.findViewById(R.id.message_container_other);
            profileImageOther = itemView.findViewById(R.id.profile_image_other);
            senderNameOther = itemView.findViewById(R.id.sender_name_other);
            messageTextOther = itemView.findViewById(R.id.message_text_other);
            messageImageOther = itemView.findViewById(R.id.message_image_other);
            timeTextOther = itemView.findViewById(R.id.time_text_other);
            reactionOther = itemView.findViewById(R.id.reaction_other);
        }
    }
}