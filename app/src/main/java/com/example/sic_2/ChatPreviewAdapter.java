package com.example.sic_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatPreviewAdapter extends RecyclerView.Adapter<ChatPreviewAdapter.ChatPreviewViewHolder> {

    private final List<ChatPreview> chatPreviews;
    private final Context context;

    public ChatPreviewAdapter(List<ChatPreview> chatPreviews, Context context) {
        this.chatPreviews = chatPreviews;
        this.context = context;
    }

    @NonNull
    @Override
    public ChatPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_preview, parent, false);
        return new ChatPreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatPreviewViewHolder holder, int position) {
        ChatPreview preview = chatPreviews.get(position);

        holder.lastMessage.setText(preview.getLastMessage());

        // Format timestamp as time or date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        String formattedDate = sdf.format(new Date(preview.getTimestamp()));
        holder.date.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        return chatPreviews.size();
    }

    static class ChatPreviewViewHolder extends RecyclerView.ViewHolder {
        TextView lastMessage;
        TextView date;

        public ChatPreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            lastMessage = itemView.findViewById(R.id.preview_message);
            date = itemView.findViewById(R.id.preview_date);
        }
    }
}