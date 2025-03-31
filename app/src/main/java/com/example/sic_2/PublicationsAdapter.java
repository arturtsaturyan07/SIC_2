package com.example.sic_2;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

public class PublicationsAdapter extends RecyclerView.Adapter<PublicationsAdapter.PublicationViewHolder> {

    private List<Publication> publicationsList;
    private Context context;
    private String currentUserId;

    public PublicationsAdapter(List<Publication> publicationsList, Context context, String currentUserId) {
        this.publicationsList = publicationsList;
        this.context = context;
        this.currentUserId = currentUserId;
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
        Publication publication = publicationsList.get(position);

        holder.contentTextView.setText(publication.getContent());

        if (publication.getImageUrl() != null && !publication.getImageUrl().isEmpty()) {
            holder.imageView.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(publication.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("image_url", publication.getImageUrl());
                context.startActivity(intent);
            });
        } else {
            holder.imageView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return publicationsList.size();
    }

    public static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView contentTextView;
        ImageView imageView;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.publication_content);
            imageView = itemView.findViewById(R.id.publication_image);
        }
    }

    public void updatePublications(List<Publication> publications) {
        this.publicationsList = publications;
        notifyDataSetChanged();
    }
}