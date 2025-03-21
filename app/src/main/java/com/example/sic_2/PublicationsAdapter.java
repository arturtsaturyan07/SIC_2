package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PublicationsAdapter extends RecyclerView.Adapter<PublicationsAdapter.PublicationViewHolder> {

    private List<Publication> publicationsList;
    private String currentUserId;

    public PublicationsAdapter(List<Publication> publicationsList, String currentUserId) {
        this.publicationsList = publicationsList;
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

        // Check if the publication is from the current user
        if (publication.getAuthorId().equals(currentUserId)) {
            // Current user's publication: show right-aligned container
            holder.publicationContainerMe.setVisibility(View.VISIBLE);
            holder.publicationTextMe.setText(publication.getContent());
            holder.publicationContainerOther.setVisibility(View.GONE); // Hide left-aligned container
        } else {
            // Other user's publication: show left-aligned container
            holder.publicationContainerOther.setVisibility(View.VISIBLE);
            holder.publicationTextOther.setText(publication.getContent());
            holder.publicationContainerMe.setVisibility(View.GONE); // Hide right-aligned container
        }
    }

    @Override
    public int getItemCount() {
        return publicationsList.size();
    }

    static class PublicationViewHolder extends RecyclerView.ViewHolder {
        LinearLayout publicationContainerOther;
        TextView publicationTextOther;
        LinearLayout publicationContainerMe;
        TextView publicationTextMe;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            publicationContainerOther = itemView.findViewById(R.id.publication_container_other);
            publicationTextOther = itemView.findViewById(R.id.publication_text_other);
            publicationContainerMe = itemView.findViewById(R.id.publication_container_me);
            publicationTextMe = itemView.findViewById(R.id.publication_text_me);
        }
    }
}