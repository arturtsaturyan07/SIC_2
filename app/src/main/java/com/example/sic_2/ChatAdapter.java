package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> chatMessages;
    private String currentUserId;
    private Map<String, String> userNames;
    private Map<String, String> userProfilePics;

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
        String senderName = userNames.containsKey(senderId) ? userNames.get(senderId) : "User";

        // Format the timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(chatMessage.getTimestamp()));

        if (senderId.equals(currentUserId)) {
            // Current user's message
            holder.messageContainerOther.setVisibility(View.GONE);
            holder.messageContainerMe.setVisibility(View.VISIBLE);

            holder.senderNameMe.setText("You");
            holder.messageTextMe.setText(chatMessage.getMessage());
            holder.timeTextMe.setText(formattedTime);

            // Styling for sent messages
            holder.messageBubbleMe.setBackground(ContextCompat.getDrawable(
                    holder.itemView.getContext(),
                    R.drawable.bubble_outgoing
            ));
        } else {
            // Other user's message
            holder.messageContainerMe.setVisibility(View.GONE);
            holder.messageContainerOther.setVisibility(View.VISIBLE);

            holder.senderNameOther.setText(senderName);
            holder.messageTextOther.setText(chatMessage.getMessage());
            holder.timeTextOther.setText(formattedTime);

            // Load profile picture
            String profileImageUrl = chatMessage.getProfileImageUrl() != null ?
                    chatMessage.getProfileImageUrl() :
                    userProfilePics.get(senderId);

            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.default_profile)
                        .into(holder.profileImageOther);
            } else {
                holder.profileImageOther.setImageResource(R.drawable.default_profile);
            }

            // Styling for received messages
            holder.messageBubbleOther.setBackground(ContextCompat.getDrawable(
                    holder.itemView.getContext(),
                    R.drawable.bubble_incoming
            ));
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        this.chatMessages = newMessages;
        notifyDataSetChanged();
    }

    public void updateUserNames(Map<String, String> newUserNames) {
        this.userNames.clear();
        this.userNames.putAll(newUserNames);
        notifyDataSetChanged();
    }

    public void updateUserProfilePics(Map<String, String> newProfilePics) {
        this.userProfilePics.clear();
        this.userProfilePics.putAll(newProfilePics);
        notifyDataSetChanged();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        // Other user's message components
        LinearLayout messageContainerOther;
        CircleImageView profileImageOther;
        TextView senderNameOther;
        LinearLayout messageBubbleOther;
        TextView messageTextOther;
        TextView timeTextOther;

        // Current user's message components
        LinearLayout messageContainerMe;
        TextView senderNameMe;
        LinearLayout messageBubbleMe;
        TextView messageTextMe;
        TextView timeTextMe;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // Other user's views
            messageContainerOther = itemView.findViewById(R.id.message_container_other);
            profileImageOther = itemView.findViewById(R.id.profile_image_other);
            senderNameOther = itemView.findViewById(R.id.sender_name_other);
            messageBubbleOther = itemView.findViewById(R.id.message_bubble_other);
            messageTextOther = itemView.findViewById(R.id.message_text_other);
            timeTextOther = itemView.findViewById(R.id.time_text_other);

            // Current user's views
            messageContainerMe = itemView.findViewById(R.id.message_container_me);
            senderNameMe = itemView.findViewById(R.id.sender_name_me);
            messageBubbleMe = itemView.findViewById(R.id.message_bubble_me);
            messageTextMe = itemView.findViewById(R.id.message_text_me);
            timeTextMe = itemView.findViewById(R.id.time_text_me);
        }
    }

    private void setupSentMessage(ChatViewHolder holder, ChatMessage chatMessage, String formattedTime) {
        holder.messageContainerOther.setVisibility(View.GONE);
        holder.messageContainerMe.setVisibility(View.VISIBLE);

        holder.senderNameMe.setText("You");
        holder.messageTextMe.setText(chatMessage.getMessage());
        holder.timeTextMe.setText(formattedTime);

        holder.messageBubbleMe.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.bubble_outgoing));
    }

    private void setupReceivedMessage(ChatViewHolder holder, ChatMessage chatMessage, String formattedTime) {
        holder.messageContainerMe.setVisibility(View.GONE);
        holder.messageContainerOther.setVisibility(View.VISIBLE);

        String senderName = userNames.getOrDefault(chatMessage.getSenderId(), "User");
        holder.senderNameOther.setText(senderName);
        holder.messageTextOther.setText(chatMessage.getMessage());
        holder.timeTextOther.setText(formattedTime);

        String profileImageUrl = chatMessage.getProfileImageUrl() != null ?
                chatMessage.getProfileImageUrl() :
                userProfilePics.get(chatMessage.getSenderId());

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(profileImageUrl)
                    .placeholder(R.drawable.default_profile)
                    .into(holder.profileImageOther);
        } else {
            holder.profileImageOther.setImageResource(R.drawable.default_profile);
        }

        holder.messageBubbleOther.setBackground(ContextCompat.getDrawable(
                holder.itemView.getContext(),
                R.drawable.bubble_incoming
        ));
    }
}