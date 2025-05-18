package com.example.sic_2;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.EditText;
import android.widget.Button;

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
        commentsAdapter = new CommentsAdapter(commentList, this);
        commentsRecyclerView.setAdapter(commentsAdapter);

        loadComments();

        sendButton.setOnClickListener(v -> {
            String text = commentInput.getText().toString().trim();
            if (!text.isEmpty()) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("comments")
                        .child(publicationId)
                        .push();
                Comment c = new Comment(userId, text, System.currentTimeMillis());
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
                    if (c != null) commentList.add(c);
                }
                commentsAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}