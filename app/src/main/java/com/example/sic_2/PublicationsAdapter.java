package com.example.sic_2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PublicationsAdapter extends RecyclerView.Adapter<PublicationsAdapter.PublicationViewHolder> {

    private List<Publication> publicationsList;
    private Context context;
    private String cardId;
    private PublicationActionListener actionListener;

    public interface PublicationActionListener {
        void onEditImageRequest(Publication publication);
        void onDeletePublicationRequest(Publication publication); // ADDED
    }

    public PublicationsAdapter(List<Publication> publicationsList, Context context, String cardId, PublicationActionListener actionListener) {
        this.publicationsList = publicationsList;
        this.context = context;
        this.cardId = cardId;
        this.actionListener = actionListener;
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
            // Remove click listeners if invalid user
            holder.authorTextView.setOnClickListener(null);
            holder.profileImageView.setOnClickListener(null);
        } else {
            // Set profile/name click listeners to open ProfileActivity
            View.OnClickListener profileClickListener = v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", userId);
                context.startActivity(intent);
            };
            holder.authorTextView.setOnClickListener(profileClickListener);
            holder.profileImageView.setOnClickListener(profileClickListener);

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

        // ---- REACTIONS ----
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (cardId != null && publication.getId() != null) {
            DatabaseReference reactionsRef = FirebaseDatabase.getInstance().getReference("posts")
                    .child(cardId)
                    .child(publication.getId())
                    .child("reactions");

            reactionsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int likeCount = 0, dislikeCount = 0;
                    Map<String, Integer> emojiMap = new HashMap<>();
                    for (DataSnapshot reaction : snapshot.getChildren()) {
                        String val = reaction.getValue(String.class);
                        if ("like".equals(val)) likeCount++;
                        else if ("dislike".equals(val)) dislikeCount++;
                        else if (val != null) emojiMap.put(val, emojiMap.getOrDefault(val,0)+1);
                    }
                    StringBuilder summary = new StringBuilder();
                    if (likeCount > 0) summary.append("👍 ").append(likeCount).append(" ");
                    if (dislikeCount > 0) summary.append("👎 ").append(dislikeCount).append(" ");
                    for (String emoji : emojiMap.keySet()) {
                        summary.append(emoji).append(" ").append(emojiMap.get(emoji)).append(" ");
                    }
                    holder.tvReactionSummary.setText(summary.toString().trim());
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // Like (tap = like, long = emoji picker)
        holder.btnLike.setOnClickListener(v -> setReaction(publication, "like"));
        holder.btnLike.setOnLongClickListener(v -> { showEmojiPicker(holder, publication); return true; });
        holder.btnDislike.setOnClickListener(v -> setReaction(publication, "dislike"));

        // Comments
        holder.btnComment.setOnClickListener(v -> {
            CommentsBottomSheet.newInstance(publication.getId())
                    .show(((AppCompatActivity) context).getSupportFragmentManager(), "comments_bottom_sheet");
        });

        // Edit (only for author)
        if (currentUserId != null && currentUserId.equals(publication.getUserId())) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> showEditDialog(publication));
            // DELETE button: show & handle for owner
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDeletePublicationRequest(publication);
            });
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    private void setReaction(Publication publication, String reaction) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (cardId != null && publication.getId() != null && userId != null) {
            FirebaseDatabase.getInstance().getReference("posts")
                    .child(cardId)
                    .child(publication.getId())
                    .child("reactions")
                    .child(userId)
                    .setValue(reaction);
        }
    }

    private void showEmojiPicker(PublicationViewHolder holder, Publication publication) {
        String[] emojis = {"❤️", "😂", "😮", "😢", "😡", "👏", "🔥"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("React with emoji");
        builder.setItems(emojis, (dialog, which) -> setReaction(publication, emojis[which]));
        builder.show();
    }

    private void showEditDialog(Publication publication) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Post");
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(publication.getContent());
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newContent = input.getText().toString().trim();
            FirebaseDatabase.getInstance().getReference("posts")
                    .child(cardId)
                    .child(publication.getId())
                    .child("content")
                    .setValue(newContent);
        });
        builder.setNeutralButton("Edit Image", (dialog, which) -> {
            if (actionListener != null) actionListener.onEditImageRequest(publication);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public int getItemCount() {
        return publicationsList.size();
    }

    public static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, contentTextView, timestampTextView, tvReactionSummary;
        ImageView imageView, btnLike, btnDislike, btnComment, btnEdit, btnDelete; // btnDelete added here!
        CircleImageView profileImageView;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.publication_author);
            contentTextView = itemView.findViewById(R.id.publication_content);
            imageView = itemView.findViewById(R.id.publication_image);
            profileImageView = itemView.findViewById(R.id.profile_image_view);
            timestampTextView = itemView.findViewById(R.id.publication_timestamp);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnDislike = itemView.findViewById(R.id.btn_dislike);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete); // <-- Make sure this exists in your layout!
            tvReactionSummary = itemView.findViewById(R.id.tv_reaction_summary);
        }
    }

    public void updatePublications(List<Publication> publications) {
        this.publicationsList = publications;
        notifyDataSetChanged();
    }
}