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
                loadCards();
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
                    createCardView(cardId, message);
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
                cardContainer.removeAllViews();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String cardId = data.child("cardId").getValue(String.class);
                    String message = data.child("message").getValue(String.class);
                    if (cardId != null && message != null) {
                        createCardView(cardId, message);
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

    private void createCardView(String cardId, String message) {
        if (!isAdded()) return;

        // Inflate the fragment-specific card layout (only message and delete button)
        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.home_card_item, cardContainer, false);
        TextView cardMessage = cardView.findViewById(R.id.card_message);
        Button deleteButton = cardView.findViewById(R.id.delete_button);

        cardMessage.setText(message);

        // Open Card activity on card click
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Card.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });

        // Delete button logic
        deleteButton.setOnClickListener(v -> deleteCard(cardId, cardView));

        cardContainer.addView(cardView);
    }

    private void filterCards(String query) {
        // Implement filtering logic for cards (if needed)
    }

    private void filterUsers(String query) {
        if (!isAdded() || getContext() == null) return;

        DatabaseReference usersDatabase = FirebaseDatabase.getInstance().getReference("users");
        usersDatabase.orderByKey().startAt(query).endAt(query + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                cardContainer.removeAllViews();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    if (username != null) {
                        createUserCardView(userId, username);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    Log.e("Firebase", "Error searching users", error.toException());
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

    private void createUserCardView(String userId, String username) {
        if (!isAdded() || getContext() == null) return;

        // Inflate a user-specific card layout (different from the activity's layout)
        View userCardView = LayoutInflater.from(requireContext()).inflate(R.layout.home_card_item, cardContainer, false);
        TextView userNameTextView = userCardView.findViewById(R.id.card_message);
        userNameTextView.setText(username);

        userCardView.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Clicked on: " + username, Toast.LENGTH_SHORT).show();
        });

        cardContainer.addView(userCardView);
    }
}