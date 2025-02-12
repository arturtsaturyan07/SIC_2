package com.example.sic_2;

import android.annotation.SuppressLint;
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
        if (isAdded()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Create a Card");

            final EditText input = new EditText(requireContext());
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String message = input.getText().toString().trim();
                if (!message.isEmpty()) {
                    saveCardToFirebase(message);
                } else {
                    Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        }
    }

    private void saveCardToFirebase(String message) {
        String cardId = database.push().getKey();
        if (cardId != null) {
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("cardId", cardId);
            cardData.put("message", message);

            database.child(cardId).setValue(cardData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    createCardView(cardId, message);
                } else {
                    Toast.makeText(requireContext(), "Failed to save card", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadCards() {
        if (isAdded()) {
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
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
                    Log.e("Firebase", "Error loading cards", error.toException());
                }
            });
        }
    }

    private void createCardView(String cardId, String message) {
        if (isAdded()) {
            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.activity_blank_fragment, cardContainer, false);
            TextView cardMessage = cardView.findViewById(R.id.card_message);
            cardMessage.setText(message);

            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), Card.class);
                intent.putExtra("card_message", message);
                startActivity(intent);
            });

             Button deleteButton = cardView.findViewById(R.id.delete_button);
            deleteButton.setOnClickListener(v -> deleteCard(cardId, cardView));

            cardContainer.addView(cardView);
        }
    }



    private void filterCards(String query) {
        // Implement filtering logic here (if needed)
    }


    private void filterUsers(String query) {
        DatabaseReference usersDatabase = FirebaseDatabase.getInstance().getReference("users");
        usersDatabase.orderByKey().startAt(query).endAt(query + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cardContainer.removeAllViews(); // Clear previous results
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
                Log.e("Firebase", "Error searching users", error.toException());
            }
        });
    }

    private void deleteCard(String cardId, View cardView) {
        database.child(cardId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                cardContainer.removeView(cardView);
                Toast.makeText(requireContext(), "Card deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to delete card", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void createUserCardView(String userId, String username) {
        View userCardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_view_layout, cardContainer, false);
        TextView userNameTextView = userCardView.findViewById(R.id.card_message);
        userNameTextView.setText(username);
        userCardView.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Clicked on: " + username, Toast.LENGTH_SHORT).show();
        });
        cardContainer.addView(userCardView);
    }




}
