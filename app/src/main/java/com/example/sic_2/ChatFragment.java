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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String ARG_CARD_ID = "cardId"; // Key for the argument

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private DatabaseReference chatRef;
    private String cardId;
    private String currentUserId;

    // Factory method to create a new instance of ChatFragment
    public static ChatFragment newInstance(String cardId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CARD_ID, cardId); // Pass the cardId as an argument
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the cardId from arguments
        if (getArguments() != null) {
            cardId = getArguments().getString(ARG_CARD_ID);
            Log.d("ChatFragment", "Card ID: " + cardId); // Debugging
        }

        // In ChatFragment.java
        chatAdapter = new ChatAdapter(chatMessages, currentUserId);

        // Validate cardId
        if (cardId == null || cardId.isEmpty()) {
            Log.e("ChatFragment", "Card ID is missing");
            showToast("Card ID is missing");
            requireActivity().finish(); // Close the activity if cardId is missing
        }

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            Log.e("ChatFragment", "User authentication failed");
            showToast("User authentication failed");
            requireActivity().finish(); // Close the activity if user is not authenticated
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_chat, container, false);

        // Initialize RecyclerView and adapter
        chatMessages = new ArrayList<>();
        // In ChatFragment.java
        chatAdapter = new ChatAdapter(chatMessages, currentUserId);
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize Firebase reference for chat messages
        chatRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(cardId)
                .child("messages");

        // Load chat messages
        loadChatMessages();

        // Initialize send button and message input
        Button sendButton = view.findViewById(R.id.send_button);
        EditText messageInput = view.findViewById(R.id.message_input);

        // Send button click listener
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText(""); // Clear input after sending
            } else {
                showToast("Message cannot be empty");
            }
        });

        return view;
    }

    /**
     * Loads chat messages from Firebase.
     */
    private void loadChatMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessages.clear(); // Clear existing messages

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    ChatMessage chatMessage = messageSnapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        chatMessages.add(chatMessage);
                    }
                }

                // Notify adapter and scroll to the latest message
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading chat messages: " + error.getMessage());
                showToast("Failed to load chat messages");
            }
        });
    }

    /**
     * Sends a new message to Firebase.
     */
    private void sendMessage(String message) {
        if (currentUserId == null || cardId == null) {
            showToast("User authentication failed");
            return;
        }

        // Generate a unique message ID
        String messageId = chatRef.push().getKey();
        if (messageId != null) {
            // Create a new ChatMessage object
            ChatMessage chatMessage = new ChatMessage(currentUserId, message, System.currentTimeMillis());

            // Save the message to Firebase
            chatRef.child(messageId).setValue(chatMessage)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e("FirebaseError", "Failed to send message", task.getException());
                            showToast("Failed to send message");
                        }
                    });
        }
    }

    /**
     * Displays a short toast message.
     */
    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}