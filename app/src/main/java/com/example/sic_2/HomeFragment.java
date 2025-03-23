package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private FloatingActionButton fab;
    private LinearLayout cardContainer;
    private SearchView searchView;
    private DatabaseReference database;
    private String userId;
    private List<Card> cardList; // List to store cards
    private CardAdapter cardAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (isAdded()) {
            fab = view.findViewById(R.id.fab);
            cardContainer = view.findViewById(R.id.card_container);
            searchView = view.findViewById(R.id.search);

            // Initialize the card list
            cardList = new ArrayList<>();

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                userId = auth.getCurrentUser().getUid();
                database = FirebaseDatabase.getInstance().getReference("cards").child(userId);
                loadCards(); // Load regular cards
                loadSharedCards(); // Load shared cards
            }

            fab.setOnClickListener(v -> showInputDialog());

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterCards(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterCards(newText);
                    return true;
                }
            });
        }

        return view;
    }

    private void showInputDialog() {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create a Card");

        final EditText input = new EditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                saveCardToFirebase(message);
            } else {
                Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }



//    private void loadCards() {
//        if (!isAdded() || database == null) return;
//
//        database.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                if (!isAdded()) return;
//                cardList.clear(); // Clear the list before adding new cards
//                for (DataSnapshot data : snapshot.getChildren()) {
//                    Card card = data.getValue(Card.class);
//                    if (card != null) {
//                        cardList.add(card); // Add the card to the list
//                        createCardView(card.getId(), card.getMessage(), false); // Regular card
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                if (isAdded()) {
//                    Log.e("Firebase", "Error loading cards", error.toException());
//                }
//            }
//        });
//    }

    private void loadSharedCards() {
        if (!isAdded() || getContext() == null) return;

        DatabaseReference sharedCardsRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(userId);

        sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!isAdded()) return;
                for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                    String cardId = cardSnapshot.getKey(); // Use the key as the cardId
                    String message = cardSnapshot.child("message").getValue(String.class);
                    String sharedBy = cardSnapshot.child("sharedBy").getValue(String.class);

                    // Log the shared card details
                    Log.d("HomeFragment", "Shared Card ID: " + cardId + ", Message: " + message + ", Shared By: " + sharedBy);

                    if (cardId != null && message != null && sharedBy != null) {
                        // Check if the shared card still exists in the original user's database
                        DatabaseReference originalCardRef = FirebaseDatabase.getInstance()
                                .getReference("cards")
                                .child(sharedBy)
                                .child(cardId);

                        originalCardRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    // Shared card exists, create the card view
                                    createCardView(cardId, message, true); // Shared card
                                } else {
                                    // Shared card has been deleted, remove it from the shared list
                                    sharedCardsRef.child(cardId).removeValue()
                                            .addOnSuccessListener(aVoid -> Log.d("HomeFragment", "Deleted shared card: " + cardId))
                                            .addOnFailureListener(e -> Log.e("HomeFragment", "Failed to delete shared card", e));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("HomeFragment", "Failed to check shared card", error.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (isAdded()) {
                    Log.e("Firebase", "Error loading shared cards", error.toException());
                }
            }
        });
    }

    private void saveCardToFirebase(String message) {
        if (message == null || message.isEmpty()) {
            Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query to check if a card with the same message already exists
        database.orderByChild("message").equalTo(message).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // A card with the same message already exists
                    Toast.makeText(requireContext(), "A card with this name already exists", Toast.LENGTH_SHORT).show();
                } else {
                    // No card with the same message exists, proceed to create a new card
                    String cardId = database.push().getKey();
                    if (cardId != null) {
                        Map<String, Object> cardData = new HashMap<>();
                        cardData.put("cardId", cardId);
                        cardData.put("message", message);
                        cardData.put("authorId", userId);
                        cardData.put("timestamp", System.currentTimeMillis());

                        database.child(cardId).setValue(cardData).addOnCompleteListener(task -> {
                            if (task.isSuccessful() && isAdded()) {
                                Card card = new Card(cardId, message, userId, System.currentTimeMillis());
                                cardList.add(card); // Add the new card to the list

                                // Check if cardAdapter is not null before calling notifyDataSetChanged
                                if (cardAdapter != null) {
                                    cardAdapter.notifyDataSetChanged(); // Refresh the RecyclerView
                                }

                                Toast.makeText(requireContext(), "Card created", Toast.LENGTH_SHORT).show();
                            } else if (isAdded()) {
                                Toast.makeText(requireContext(), "Failed to save card", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCards() {
        if (!isAdded() || database == null) return;

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!isAdded()) return;
                cardList.clear(); // Clear the list before adding new cards
                for (DataSnapshot data : snapshot.getChildren()) {
                    String cardId = data.getKey(); // Use the key as the cardId
                    String message = data.child("message").getValue(String.class);

                    // Log the cardId and message
                    Log.d("HomeFragment", "Loaded Card ID: " + cardId + ", Message: " + message);

                    if (cardId != null && message != null) {
                        Card card = new Card(cardId, message, userId, System.currentTimeMillis());
                        cardList.add(card); // Add the card to the list
                        createCardView(cardId, message, false); // Regular card
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (isAdded()) {
                    Log.e("Firebase", "Error loading cards", error.toException());
                }
            }
        });
    }

    private void createCardView(String cardId, String message, boolean isShared) {
        if (!isAdded()) return;

        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.home_card_item, cardContainer, false);
        TextView cardMessage = cardView.findViewById(R.id.card_message);

        cardMessage.setText(message);

        if (isShared) {
            cardMessage.setText("[Shared] " + message); // Indicate shared cards
        }

        // Log the cardId before passing it
        Log.d("HomeFragment", "Card ID: " + cardId);

        // Open Card activity on card click
        cardView.setOnClickListener(v -> {
            if (cardId == null || cardId.isEmpty()) {
                Toast.makeText(requireContext(), "Card ID is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the card exists in the database
            DatabaseReference cardRef = FirebaseDatabase.getInstance()
                    .getReference("cards")
                    .child(userId)
                    .child(cardId);

            cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Card exists, proceed to open it
                        Intent intent = new Intent(requireContext(), CardActivity.class);
                        intent.putExtra("cardId", cardId); // Pass the cardId to CardActivity
                        Log.d("HomeFragment", "Intent Card ID: " + cardId);
                        startActivity(intent);
                    } else {
                        // Card does not exist, show a message to the user
                        Toast.makeText(requireContext(), "This card has been deleted", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle database error
                    Toast.makeText(requireContext(), "Failed to check card: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        cardContainer.addView(cardView);
    }

    private void filterCards(String query) {
        if (!isAdded()) return;

        // Clear the card container before displaying filtered results
        cardContainer.removeAllViews();

        // Perform the search query
        database.orderByChild("message")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        int matchCount = 0; // Track the number of matching cards

                        for (DataSnapshot data : snapshot.getChildren()) {
                            Card card = data.getValue(Card.class);
                            if (card != null) {
                                createCardView(card.getId(), card.getMessage(), false); // Regular card
                                matchCount++; // Increment the match count
                            }
                        }

                        // If no cards match the query, display a "No cards found" message
                        if (matchCount == 0) {
                            showNoCardsFoundMessage();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (isAdded()) {
                            Log.e("Firebase", "Error filtering cards", error.toException());
                        }
                    }
                });
    }

    private void showNoCardsFoundMessage() {
        if (!isAdded()) return;

        // Create a TextView to display the "No cards found" message
        TextView noCardsFoundText = new TextView(requireContext());
        noCardsFoundText.setText("No cards found");
        noCardsFoundText.setTextSize(18); // Set text size
        noCardsFoundText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray)); // Set text color
        noCardsFoundText.setGravity(Gravity.CENTER); // Center the text

        // Add the TextView to the card container
        cardContainer.addView(noCardsFoundText);
    }

    public void onCardDeleted(String cardId) {
        for (int i = 0; i < cardList.size(); i++) {
            if (cardList.get(i).getId().equals(cardId)) {
                cardList.remove(i); // Remove the card from the list
                cardAdapter.notifyItemRemoved(i); // Notify the adapter
                break;
            }
        }
    }

    public void reloadData() {
        cardList.clear(); // Clear the existing data
        loadCards(); // Load cards from Firebase
        loadSharedCards(); // Load shared cards from Firebase
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        reloadData(); // Reload data when the fragment is resumed
//    }

}
