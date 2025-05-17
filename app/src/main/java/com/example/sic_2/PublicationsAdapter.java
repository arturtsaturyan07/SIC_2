package com.example.sic_2;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PublicationsAdapter extends RecyclerView.Adapter<PublicationsAdapter.PublicationViewHolder> {

    private List<Publication> publicationsList;
    private Context context;

    public PublicationsAdapter(List<Publication> publicationsList, Context context) {
        this.publicationsList = publicationsList;
        this.context = context;
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

        // Load publication image (if any)
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

        // Set timestamp
        holder.timestampTextView.setText(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(publication.getTimestamp()))
        );

        // --- DYNAMICALLY LOAD USER INFO FROM DATABASE ---
        holder.authorTextView.setText("..."); // Loading placeholder
        holder.profileImageView.setImageResource(R.drawable.default_profile);

        String userId = publication.getUserId();
        if (userId == null || userId.isEmpty()) {
            holder.authorTextView.setText("Unknown");
            holder.profileImageView.setImageResource(R.drawable.default_profile);
            return;
        }

        FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        String surname = snapshot.child("surname").getValue(String.class);
                        String profileUrl = snapshot.child("profileImageUrl").getValue(String.class);

                        String fullName = name != null && surname != null && !surname.isEmpty()
                                ? name + " " + surname
                                : (name != null ? name : "Unknown");
                        holder.authorTextView.setText(fullName);

                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(holder.profileImageView);
                        } else {
                            holder.profileImageView.setImageResource(R.drawable.default_profile);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.authorTextView.setText("Unknown");
                        holder.profileImageView.setImageResource(R.drawable.default_profile);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return publicationsList.size();
    }

    public static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView;
        TextView contentTextView;
        ImageView imageView;
        CircleImageView profileImageView;
        TextView timestampTextView;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.publication_author);
            contentTextView = itemView.findViewById(R.id.publication_content);
            imageView = itemView.findViewById(R.id.publication_image);
            profileImageView = itemView.findViewById(R.id.profile_image_view);
            timestampTextView = itemView.findViewById(R.id.publication_timestamp);
        }
    }

    public void updatePublications(List<Publication> publications) {
        this.publicationsList = publications;
        notifyDataSetChanged();
    }
}