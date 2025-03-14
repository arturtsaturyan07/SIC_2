package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
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
    private FloatingActionButton fab;
    private LinearLayout cardContainer;
    private SearchView searchView;
    private DatabaseReference database;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (isAdded()) {
            fab = view.findViewById(R.id.fab);
            cardContainer = view.findViewById(R.id.card_container);
            searchView = view.findViewById(R.id.search);

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

    private void saveCardToFirebase(String message) {
        String cardId = database.push().getKey();
        if (cardId != null) {
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("cardId", cardId);
            cardData.put("message", message);

            database.child(cardId).setValue(cardData).addOnCompleteListener(task -> {
                if (task.isSuccessful() && isAdded()) {
                    createCardView(cardId, message, false); // Regular card
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to save card", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadCards() {
        if (!isAdded() || database == null) return;

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!isAdded()) return;
                for (DataSnapshot data : snapshot.getChildren()) {
                    String cardId = data.child("cardId").getValue(String.class);
                    String message = data.child("message").getValue(String.class);
                    if (cardId != null && message != null) {
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
                    String cardId = cardSnapshot.getKey();
                    String message = cardSnapshot.child("message").getValue(String.class);
                    String sharedBy = cardSnapshot.child("sharedBy").getValue(String.class);

                    if (cardId != null && message != null && sharedBy != null) {
                        createCardView(cardId, message, true); // Shared card
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

    private void createCardView(String cardId, String message, boolean isShared) {
        if (!isAdded()) return;

        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.home_card_item, cardContainer, false);
        TextView cardMessage = cardView.findViewById(R.id.card_message);
        Button deleteButton = cardView.findViewById(R.id.delete_button);

        cardMessage.setText(message);

        if (isShared) {
            cardMessage.setText("[Shared] " + message); // Indicate shared cards
            deleteButton.setVisibility(View.GONE); // Disable delete for shared cards
        }

        // Open Card activity on card click
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CardActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });

        // Delete button logic (only for regular cards)
        deleteButton.setOnClickListener(v -> deleteCard(cardId, cardView));

        cardContainer.addView(cardView);
    }

    private void filterCards(String query) {
        if (!isAdded()) return;

        cardContainer.removeAllViews();
        database.orderByChild("message").startAt(query).endAt(query + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!isAdded()) return;
                for (DataSnapshot data : snapshot.getChildren()) {
                    String cardId = data.child("cardId").getValue(String.class);
                    String message = data.child("message").getValue(String.class);
                    if (cardId != null && message != null) {
                        createCardView(cardId, message, false); // Regular card
                    }
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

    private void deleteCard(String cardId, View cardView) {
        if (!isAdded() || database == null) return;

        database.child(cardId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful() && isAdded()) {
                cardContainer.removeView(cardView);
                Toast.makeText(requireContext(), "Card deleted", Toast.LENGTH_SHORT).show();
            } else if (isAdded()) {
                Toast.makeText(requireContext(), "Failed to delete card", Toast.LENGTH_SHORT).show();
            }
        });
    }
}