package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> chatMessages;
    private String currentUserId;

    public ChatAdapter(List<ChatMessage> chatMessages, String currentUserId) {
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
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

        // Check if the message is from the current user
        if (chatMessage.getSenderId().equals(currentUserId)) {
            // Current user's message: show right-aligned container
            holder.messageContainerMe.setVisibility(View.VISIBLE);
            holder.messageTextMe.setText(chatMessage.getMessage());
            holder.messageContainerOther.setVisibility(View.GONE); // Hide left-aligned container
        } else {
            // Other user's message: show left-aligned container
            holder.messageContainerOther.setVisibility(View.VISIBLE);
            holder.messageTextOther.setText(chatMessage.getMessage());
            holder.messageContainerMe.setVisibility(View.GONE); // Hide right-aligned container
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainerOther;
        TextView messageTextOther;
        LinearLayout messageContainerMe;
        TextView messageTextMe;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainerOther = itemView.findViewById(R.id.message_container_other);
            messageTextOther = itemView.findViewById(R.id.message_text_other);
            messageContainerMe = itemView.findViewById(R.id.message_container_me);
            messageTextMe = itemView.findViewById(R.id.message_text_me);
        }
    }
}