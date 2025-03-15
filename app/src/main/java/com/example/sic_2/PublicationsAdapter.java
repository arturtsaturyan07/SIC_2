package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.authorTextView.setText("Author ID: " + publication.getAuthorId());
        holder.contentTextView.setText(publication.getContent());
    }

    @Override
    public int getItemCount() {
        return publications.size();
    }

    static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, contentTextView;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.author_text_view);
            contentTextView = itemView.findViewById(R.id.content_text_view);
        }
    }
}