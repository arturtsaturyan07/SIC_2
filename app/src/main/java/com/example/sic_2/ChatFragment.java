package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final String ARG_CARD_ID = "cardId";
    private static final String ARG_OWNER_ID = "originalOwnerId";

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private DatabaseReference chatRef;
    private String cardId;
    private String currentUserId;
    private ChildEventListener chatListener;

    public static ChatFragment newInstance(String cardId, String originalOwnerId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CARD_ID, cardId);
        args.putString(ARG_OWNER_ID, originalOwnerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            cardId = getArguments().getString(ARG_CARD_ID);
            String originalOwnerId = getArguments().getString(ARG_OWNER_ID);
            Log.d(TAG, "Card ID: " + cardId + ", Owner ID: " + originalOwnerId);
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            showToast("Please sign in first");
            requireActivity().finish();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);
        setupViews(view);
        setupFirebase();
        return view;
    }

    private void setupViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        EditText messageInput = view.findViewById(R.id.message_input);
        Button sendButton = view.findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            } else {
                showToast("Message cannot be empty");
            }
        });
    }

    private void setupFirebase() {
        // Initialize Firebase reference based on your database structure
        chatRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardId)
                .child("chats")
                .child("messages");

        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    // Set the message ID from the snapshot key
                    message.setId(snapshot.getKey());
                    chatMessages.add(message);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle message updates if needed
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle message removal if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle message reordering if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Chat listener cancelled: " + error.getMessage());
                showToast("Failed to load messages");
            }
        };

        chatRef.addChildEventListener(chatListener);
    }

    private void sendMessage(String message) {
        if (currentUserId == null) {
            showToast("Please sign in first");
            return;
        }

        String messageId = chatRef.push().getKey();
        if (messageId == null) {
            showToast("Failed to create message");
            return;
        }

        ChatMessage chatMessage = new ChatMessage(
                currentUserId,
                message,
                System.currentTimeMillis()
        );

        chatRef.child(messageId).setValue(chatMessage)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Failed to send message");
                        Log.e(TAG, "Message send failed", task.getException());
                    }
                });
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }
    }
}