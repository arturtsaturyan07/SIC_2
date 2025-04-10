package com.example.sic_2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class ParametersFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private String cardId;
    private String currentUserId;
    private String originalOwnerId;

    private DatabaseReference database;
    private DatabaseReference sharedCardsRef;

    public ParametersFragment() {
        // Required empty public constructor
    }

    public static ParametersFragment newInstance(String param1, String param2, String cardId, String originalOwnerId) {
        ParametersFragment fragment = new ParametersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString("cardId", cardId);
        args.putString("originalOwnerId", originalOwnerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            cardId = getArguments().getString("cardId");
            originalOwnerId = getArguments().getString("originalOwnerId");
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference("cards");
        sharedCardsRef = FirebaseDatabase.getInstance().getReference("sharedCards");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parameters, container, false);

        // Initialize buttons
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button shareButton = view.findViewById(R.id.shareButton);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button deleteButton = view.findViewById(R.id.deleteButton);

        // Hide share button if this is a shared card (not owned by current user)
        if (originalOwnerId != null && !originalOwnerId.equals(currentUserId)) {
            shareButton.setVisibility(View.GONE);
        }

        // Set click listeners
        shareButton.setOnClickListener(v -> showShareDialog());
        deleteButton.setOnClickListener(v -> deleteCard());

        return view;
    }

    private void showShareDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Share Card");

        final EditText userIdInput = new EditText(requireContext());
        userIdInput.setHint("Recipient User ID");
        builder.setView(userIdInput);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String recipientUserId = userIdInput.getText().toString().trim();
            if (recipientUserId.isEmpty()) {
                Toast.makeText(requireContext(), "User ID cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user is trying to share with themselves
            if (recipientUserId.equals(currentUserId)) {
                Toast.makeText(requireContext(), "You can't share a card with yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            shareCardWithUser(recipientUserId);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void shareCardWithUser(String recipientUserId) {
        if (recipientUserId == null || recipientUserId.isEmpty() || cardId == null || cardId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid user ID or card ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if we're sharing our own card or someone else's
        String ownerId = originalOwnerId != null ? originalOwnerId : currentUserId;

        // Reference to the original card
        DatabaseReference cardRef = database.child(ownerId).child(cardId);

        // Reference to the sharedCards node for the recipient user
        DatabaseReference recipientSharedCardsRef = sharedCardsRef.child(recipientUserId).child(cardId);

        // Fetch the card data
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Card card = snapshot.getValue(Card.class);
                    if (card != null) {
                        // Create a map to store the shared card data
                        Map<String, Object> shareData = new HashMap<>();
                        shareData.put("id", card.getId());
                        shareData.put("title", card.getTitle());
                        shareData.put("description", card.getDescription());
                        shareData.put("priority", card.getPriority());
                        shareData.put("authorId", ownerId); // Original owner ID
                        shareData.put("sharedBy", currentUserId); // Who shared it
                        shareData.put("timestamp", System.currentTimeMillis());

                        // Save the shared card data to the recipient's sharedCards node
                        recipientSharedCardsRef.setValue(shareData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(requireContext(), "Card shared successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to share card", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(requireContext(), "Card data is invalid", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Card not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCard() {
        if (cardId == null || database == null || currentUserId == null) {
            Toast.makeText(requireContext(), "Invalid card or user", Toast.LENGTH_SHORT).show();
            return;
        }

        // If this is a shared card (not owned by current user), just remove from sharedCards
        if (originalOwnerId != null && !originalOwnerId.equals(currentUserId)) {
            sharedCardsRef.child(currentUserId).child(cardId).removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && isAdded()) {
                            if (getActivity() instanceof CardActivity) {
                                ((CardActivity) getActivity()).onCardDeleted(cardId);
                                ((CardActivity) getActivity()).reloadHomeFragmentData();
                            }
                            requireActivity().finish();
                            Toast.makeText(requireContext(), "Shared card removed", Toast.LENGTH_SHORT).show();
                        } else if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to remove shared card", Toast.LENGTH_SHORT).show();
                        }
                    });
            return;
        }

        // If this is our own card, delete from our cards and all sharedCards references
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);

        // First delete the main card
        cardRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful() && isAdded()) {
                // Then delete all shared references to this card
                sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            if (userId != null) {
                                sharedCardsRef.child(userId).child(cardId).removeValue();
                            }
                        }

                        // Notify the HomeFragment
                        if (getActivity() instanceof CardActivity) {
                            ((CardActivity) getActivity()).onCardDeleted(cardId);
                            ((CardActivity) getActivity()).reloadHomeFragmentData();
                        }

                        requireActivity().finish();
                        Toast.makeText(requireContext(), "Card deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to delete shared cards: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (isAdded()) {
                Toast.makeText(requireContext(), "Failed to delete card", Toast.LENGTH_SHORT).show();
            }
        });
    }
}