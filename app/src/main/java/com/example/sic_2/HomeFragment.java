package com.example.sic_2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private DatabaseReference database;
    private FirebaseAuth auth;
    private String userId;
    private LinearLayout cardContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance();
        cardContainer = view.findViewById(R.id.card_container);

        // Ensure the user is authenticated
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            database = FirebaseDatabase.getInstance().getReference("cards").child(userId);
            loadCards(); // Load existing cards from Firebase
        } else {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }

        // Floating Action Button to add new cards
        FloatingActionButton addButton = view.findViewById(R.id.fab);
        EditText messageInput = view.findViewById(R.id.message_input);

        addButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                saveCardToFirebase(message); // Save the card to Firebase
                messageInput.setText(""); // Clear the input field
            } else {
                Toast.makeText(getContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // Save a new card to Firebase
    private void saveCardToFirebase(String message) {
        String cardId = database.push().getKey(); // Generate a unique key for the card
        if (cardId != null) {
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("message", message);
            database.child(cardId).setValue(cardData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    createCardView(cardId, message); // Create the card view locally
                } else {
                    Toast.makeText(requireContext(), "Failed to save card", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Load cards from Firebase
    private void loadCards() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cardContainer.removeAllViews(); // Clear the container before loading new cards
                for (DataSnapshot data : snapshot.getChildren()) {
                    String cardId = data.getKey();
                    String message = data.child("message").getValue(String.class);
                    if (cardId != null && message != null) {
                        createCardView(cardId, message); // Create a card view for each item
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load cards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Create a card view programmatically
    private void createCardView(String cardId, String message) {
        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_view_layout, cardContainer, false);
        TextView cardMessage = cardView.findViewById(R.id.card_message);
        Button deleteButton = cardView.findViewById(R.id.delete_button);

        cardMessage.setText(message); // Set the message text

        // Handle delete button click
        deleteButton.setOnClickListener(v -> deleteCard(cardId, cardView));

        cardContainer.addView(cardView); // Add the card view to the container
    }

    // Delete a card from Firebase and the UI
    private void deleteCard(String cardId, View cardView) {
        database.child(cardId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                cardContainer.removeView(cardView); // Remove the card view from the UI
                Toast.makeText(requireContext(), "Card deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to delete card", Toast.LENGTH_SHORT).show();
            }
        });
    }
}