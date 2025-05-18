package com.example.sic_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private Context context;

    public CommentsAdapter(List<Comment> comments, Context context) {
        this.comments = comments;
        this.context = context;
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
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, contentTextView, timestampTextView;
        CircleImageView profileImageView;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.comment_author);
            contentTextView = itemView.findViewById(R.id.comment_content);
            timestampTextView = itemView.findViewById(R.id.comment_timestamp);
            profileImageView = itemView.findViewById(R.id.comment_author_image);
        }
    }
}