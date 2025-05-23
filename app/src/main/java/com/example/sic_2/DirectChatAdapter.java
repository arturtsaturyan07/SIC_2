package com.example.sic_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class DirectChatAdapter extends RecyclerView.Adapter<DirectChatAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public DirectChatAdapter(Context context, List<User> userList, OnItemClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_direct_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.name.setText(user.getName());

        // Load image using Glide if needed
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.default_profile);
        }

        // Show unread count badge if there are unread messages
        int unreadCount = user.getUnreadCount();
        if (unreadCount > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(String.valueOf(unreadCount));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView name;
        TextView unreadBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.chat_user_image);
            name = itemView.findViewById(R.id.chat_user_name);
            unreadBadge = itemView.findViewById(R.id.chat_unread_badge);
        }
    }
}