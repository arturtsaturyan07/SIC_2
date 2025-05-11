package com.example.sic_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sic_2.User;

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
        holder.userName.setText(user.getFullName());
        // Load image with Glide here if needed
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
}