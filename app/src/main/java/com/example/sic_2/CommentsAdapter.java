package com.example.sic_2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private Context context;
    private String currentUserId;
    private String publicationId; // Store publicationId if needed for DB path

    public CommentsAdapter(List<Comment> comments, Context context, String publicationId) {
        this.comments = comments;
        this.context = context;
        this.publicationId = publicationId;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        holder.contentTextView.setText(comment.getContent());
        holder.timestampTextView.setText(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(comment.getTimestamp()))
        );

        // Load user info
        holder.authorTextView.setText("...");
        holder.profileImageView.setImageResource(R.drawable.default_profile);
        String userId = comment.getUserId();
        if (userId != null && !userId.isEmpty()) {
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
        } else {
            holder.authorTextView.setText("Unknown");
            holder.profileImageView.setImageResource(R.drawable.default_profile);
        }

        // Likes count
        if (comment.getLikes() != null && !comment.getLikes().isEmpty()) {
            holder.likesTextView.setVisibility(View.VISIBLE);
            holder.likesTextView.setText("♥ " + comment.getLikes().size());
            // Highlight if user liked
            if (currentUserId != null && comment.getLikes().containsKey(currentUserId)) {
                holder.likesTextView.setTextColor(context.getResources().getColor(R.color.liked_heart));
            } else {
                holder.likesTextView.setTextColor(context.getResources().getColor(R.color.text_secondary));
            }
        } else {
            holder.likesTextView.setVisibility(View.GONE);
        }

        // Like click
        holder.likesTextView.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(context, "Sign in to like comments", Toast.LENGTH_SHORT).show();
                return;
            }
            if (publicationId == null || comment.getId() == null) {
                Toast.makeText(context, "Error: Missing information for liking.", Toast.LENGTH_SHORT).show();
                return;
            }
            DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference("comments")
                    .child(publicationId).child(comment.getId()).child("likes").child(currentUserId);
            if (comment.getLikes() != null && comment.getLikes().containsKey(currentUserId)) {
                // Unlike
                commentRef.removeValue();
            } else {
                // Like
                commentRef.setValue(true);
            }
        });

        // Long press: show popup menu (copy, like, edit, delete)
        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.itemView);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_comment, popup.getMenu());

            boolean isOwner = currentUserId != null && currentUserId.equals(comment.getUserId());
            popup.getMenu().findItem(R.id.action_edit).setVisible(isOwner);
            popup.getMenu().findItem(R.id.action_delete).setVisible(isOwner);

            if (currentUserId != null && comment.getLikes() != null && comment.getLikes().containsKey(currentUserId)) {
                popup.getMenu().findItem(R.id.action_like).setTitle("Unlike");
            } else {
                popup.getMenu().findItem(R.id.action_like).setTitle("Like");
            }

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_copy) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Comment", comment.getContent());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Comment copied", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.action_like) {
                    holder.likesTextView.performClick();
                    return true;
                } else if (id == R.id.action_edit) {
                    showEditDialog(comment);
                    return true;
                } else if (id == R.id.action_delete) {
                    showDeleteDialog(comment);
                    return true;
                }
                return false;
            });
            popup.show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private void showEditDialog(Comment comment) {
        if (publicationId == null || comment.getId() == null) {
            Toast.makeText(context, "Error: Missing information for editing.", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText editText = new EditText(context);
        editText.setText(comment.getContent());
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(context)
                .setTitle("Edit Comment")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = editText.getText().toString().trim();
                    if (!newText.isEmpty() && !newText.equals(comment.getContent())) {
                        FirebaseDatabase.getInstance().getReference("comments")
                                .child(publicationId)
                                .child(comment.getId())
                                .child("content")
                                .setValue(newText);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(Comment comment) {
        if (publicationId == null || comment.getId() == null) {
            Toast.makeText(context, "Error: Missing information for deleting.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference("comments")
                            .child(publicationId)
                            .child(comment.getId())
                            .removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, contentTextView, timestampTextView, likesTextView;
        CircleImageView profileImageView;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.comment_author);
            contentTextView = itemView.findViewById(R.id.comment_content);
            timestampTextView = itemView.findViewById(R.id.comment_timestamp);
            profileImageView = itemView.findViewById(R.id.comment_author_image);
            likesTextView = itemView.findViewById(R.id.comment_likes);
        }
    }
}