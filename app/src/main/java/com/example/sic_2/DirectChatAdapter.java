package com.example.sic_2;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sic_2.ChatActivity;
import com.example.sic_2.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class DirectChatAdapter extends RecyclerView.Adapter<DirectChatAdapter.DirectChatViewHolder> {

    private final Context context;
    private final List<User> users;

    public DirectChatAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public DirectChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_direct_chat, parent, false);
        return new DirectChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DirectChatViewHolder holder, int position) {
        User user = users.get(position);

        // Set user name or placeholder
        String fullName = user.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            fullName = "Unknown";
        }
        holder.userName.setText(fullName);

        // Optional: Load profile image using Glide if available

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            String otherUserId = user.getUserId();
            String chatId = "direct_chat_" + user.getUserId() + "_" + otherUserId;

            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("cardId", chatId);
            context.startActivity(intent);

            if (otherUserId != null && !otherUserId.isEmpty()) {
                // Generate consistent direct chat ID

                FirebaseDatabase.getInstance()
                        .getReference("user_chats")
                        .child(user.getUserId())
                        .child(chatId)
                        .child("read").setValue(true);

                FirebaseDatabase.getInstance()
                        .getReference("user_chats")
                        .child(otherUserId)
                        .child(chatId)
                        .child("read").setValue(false);

                intent.putExtra("cardId", chatId);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class DirectChatViewHolder extends RecyclerView.ViewHolder {
        TextView userName;

        public DirectChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.direct_chat_user_name);
        }
    }

    // Helper method (you can get currentUserId from ViewModel or pass it in constructor)
    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}