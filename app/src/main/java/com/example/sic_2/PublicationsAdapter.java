package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.sic_2.Publication;

import java.util.List;

public class PublicationsAdapter extends RecyclerView.Adapter<PublicationsAdapter.PublicationViewHolder> {

    private final List<Publication> publications;

    public PublicationsAdapter(List<Publication> publications) {
        this.publications = publications;
    }

    @NonNull
    @Override
    public PublicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publication, parent, false);
        return new PublicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicationViewHolder holder, int position) {
        Publication publication = publications.get(position);
        holder.contentTextView.setText(publication.getContent());

        // Load the photo using Glide
        String imageUrl = publication.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .into(holder.publicationImageView);
            holder.publicationImageView.setVisibility(View.VISIBLE);
        } else {
            holder.publicationImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return publications.size();
    }

    static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView contentTextView;
        ImageView publicationImageView;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.content_text_view);
            publicationImageView = itemView.findViewById(R.id.publication_image);
        }
    }
}