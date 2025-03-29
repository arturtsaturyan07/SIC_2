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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PublicationsAdapter extends RecyclerView.Adapter<PublicationsAdapter.PostViewHolder> {
    private List<Publication> publications;
    private Context context;

    public PublicationsAdapter(List<Publication> publications, Context context) {
        this.publications = publications;
        this.context = context;
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

        // Handle text content
        holder.postText.setVisibility(publication.getContent() != null ? View.VISIBLE : View.GONE);
        if (publication.getContent() != null) {
            holder.postText.setText(publication.getContent());
        }

        // Handle image content
        holder.postImage.setVisibility(publication.getImageUrl() != null ? View.VISIBLE : View.GONE);
        if (publication.getImageUrl() != null) {
            Glide.with(context)
                    .load(publication.getImageUrl())
                    .placeholder(R.drawable.baseline_account_circle_24) // Add a placeholder
                    .error(R.drawable.baseline_dangerous_24) // Add an error image
                    .into(holder.postImage);
        }

        // Set formatted timestamp
        holder.postTime.setText(formatTimestamp(publication.getTimestamp()));

        // Optional: Add click listener for images
        if (publication.getImageUrl() != null) {
            holder.postImage.setOnClickListener(v -> {
                // Handle image click (e.g., open full screen)
            });
        }
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
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
        TextView postText;
        ImageView postImage;
        TextView postTime;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postText = itemView.findViewById(R.id.post_text);
            postImage = itemView.findViewById(R.id.post_image);
            postTime = itemView.findViewById(R.id.post_time);
        }
    }
}