package com.example.sic_2;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<UnreadChatPreview> unreadChats;

    public NotificationAdapter(List<UnreadChatPreview> unreadChats) {
        this.unreadChats = unreadChats;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unread_chat, parent, false);
        return new NotificationViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        UnreadChatPreview chat = unreadChats.get(position);

        holder.messageText.setText(chat.getLastMessage());

        // Format timestamp as time or date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        String formattedDate = sdf.format(chat.getTimestamp());
        holder.dateText.setText(formattedDate);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
            intent.putExtra("open_chat", true);
            intent.putExtra("cardId", chat.getCardId());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return unreadChats.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView dateText;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.preview_message);
            dateText = itemView.findViewById(R.id.preview_date);
        }
    }
}