package com.example.sic_2;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PublicationsAdapter extends RecyclerView.Adapter<PublicationsAdapter.PostViewHolder> {
    private List<Publication> publications;
    private Context context;
    private String currentUserId;

    // Colors for message bubbles
    private static final int COLOR_CURRENT_USER = 0xFF0084FF; // Blue
    private static final int COLOR_OTHER_USER = 0xFFEEEEEE;  // Light gray

    public PublicationsAdapter(List<Publication> publications, Context context, String currentUserId) {
        this.publications = publications;
        this.context = context;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Publication publication = publications.get(position);
        boolean isCurrentUser = publication.getUserId().equals(currentUserId);

        // Set message alignment based on sender
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        if (isCurrentUser) {
            // Current user's message - align right
            layoutParams.gravity = Gravity.END;
            holder.messageContainer.setBackgroundResource(R.drawable.bubble_outgoing);
            holder.postText.setTextColor(context.getResources().getColor(android.R.color.white));
        } else {
            // Other user's message - align left
            layoutParams.gravity = Gravity.START;
            holder.messageContainer.setBackgroundResource(R.drawable.bubble_incoming);
            holder.postText.setTextColor(context.getResources().getColor(android.R.color.black));
        }
        holder.messageContainer.setLayoutParams(layoutParams);

        // Handle text content
        if (publication.getContent() != null && !publication.getContent().isEmpty()) {
            holder.postText.setVisibility(View.VISIBLE);
            holder.postText.setText(publication.getContent());
        } else {
            holder.postText.setVisibility(View.GONE);
        }

        // Handle image content
        if (publication.getImageUrl() != null && !publication.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(publication.getImageUrl())
                    .placeholder(R.drawable.baseline_account_circle_24)
                    .error(R.drawable.bubble_outgoing)
                    .into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // Set timestamp
        holder.postTime.setText(formatTimestamp(publication.getTimestamp()));
        holder.postTime.setGravity(isCurrentUser ? Gravity.END : Gravity.START);

        // Image click listener
        if (publication.getImageUrl() != null) {
            holder.postImage.setOnClickListener(v -> {
                // Handle image click (e.g., open full screen)
            });
        }
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public void updatePublications(List<Publication> newPublications) {
        this.publications = newPublications;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return publications != null ? publications.size() : 0;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        TextView postText;
        ImageView postImage;
        TextView postTime;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.message_container);
            postText = itemView.findViewById(R.id.post_text);
            postImage = itemView.findViewById(R.id.post_image);
            postTime = itemView.findViewById(R.id.post_time);
        }
    }
}