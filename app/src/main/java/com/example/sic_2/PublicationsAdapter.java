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

        if (publication.getContent() != null && !publication.getContent().isEmpty()) {
            holder.postText.setVisibility(View.VISIBLE);
            holder.postText.setText(publication.getContent());
        } else {
            holder.postText.setVisibility(View.GONE);
        }

        if (publication.getImageUrl() != null && !publication.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(publication.getImageUrl())
                    .into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        holder.postTime.setText(formatTimestamp(publication.getTimestamp()));
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return publications.size();
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