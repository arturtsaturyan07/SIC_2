package com.example.sic_2;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class CommentsActivity extends AppCompatActivity {
    private String publicationId;
    private RecyclerView commentsRecyclerView;
    private EditText commentInput;
    private Button sendButton;
    private List<Comment> commentList;
    private CommentsAdapter commentsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        publicationId = getIntent().getStringExtra("publicationId");
        commentsRecyclerView = findViewById(R.id.comments_recycler_view);
        commentInput = findViewById(R.id.comment_input);
        sendButton = findViewById(R.id.send_comment_button);

        commentList = new ArrayList<>();
        commentsAdapter = new CommentsAdapter(commentList, this, publicationId);
        commentsRecyclerView.setAdapter(commentsAdapter);

        loadComments();

        sendButton.setOnClickListener(v -> {
            String text = commentInput.getText().toString().trim();
            if (!text.isEmpty()) {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(this, "You must be signed in to comment.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("comments")
                        .child(publicationId)
                        .push();
                String commentId = ref.getKey();
                Comment c = new Comment(userId, text, System.currentTimeMillis());
                c.setId(commentId);
                ref.setValue(c);
                commentInput.setText("");
            }
        });
    }

    private void loadComments() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("comments").child(publicationId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Comment c = ds.getValue(Comment.class);
                    if (c != null) {
                        c.setId(ds.getKey()); // Set the Firebase key as the comment's id!
                        commentList.add(c);
                    }
                }
                commentsAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}