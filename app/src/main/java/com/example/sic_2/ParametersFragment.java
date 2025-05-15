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

    private String cardId;
    private String currentUserId;
    private String originalOwnerId;

    private DatabaseReference allCardsRef;
    private DatabaseReference sharedCardsRef;
    private DatabaseReference userCardsRef;

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
            cardId = getArguments().getString("cardId");
            originalOwnerId = getArguments().getString("originalOwnerId");
        }

        // Initialize Firebase references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        allCardsRef = database.getReference("allCards");
        sharedCardsRef = database.getReference("sharedCards");
        userCardsRef = database.getReference("cards");

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parameters, container, false);

        Button shareButton = view.findViewById(R.id.shareButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);

        // Hide share button if current user is not the original owner
        if (originalOwnerId != null && !originalOwnerId.equals(currentUserId)) {
            shareButton.setVisibility(View.GONE);
        }

        shareButton.setOnClickListener(v -> showShareDialog());
        deleteButton.setOnClickListener(v -> confirmDeleteCard());

        return view;
    }

    private void showShareDialog() {
        if (getActivity() == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
        builder.setTitle("Share Card");

        final EditText userIdInput = new EditText(getActivity());
        userIdInput.setHint("Enter recipient's user ID");
        builder.setView(userIdInput);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String recipientUserId = userIdInput.getText().toString().trim();
            if (recipientUserId.isEmpty()) {
                showToast("User ID cannot be empty");
                return;
            }

            if (recipientUserId.equals(currentUserId)) {
                showToast("You can't share with yourself");
                return;
            }

            shareCardWithUser(recipientUserId);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void shareCardWithUser(String recipientUserId) {
        if (cardId == null || currentUserId == null) {
            showToast("Invalid card or user");
            return;
        }

        // Determine the correct card reference based on originalOwnerId
        DatabaseReference cardRef = originalOwnerId != null
                ? userCardsRef.child(originalOwnerId).child(cardId)
                : userCardsRef.child(currentUserId).child(cardId);

        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showToast("Card not found");
                    return;
                }

                // Create shared card data
                Map<String, Object> shareData = new HashMap<>();
                shareData.put("id", cardId);
                shareData.put("originalOwnerId", originalOwnerId != null ? originalOwnerId : currentUserId);
                shareData.put("sharedBy", currentUserId);
                shareData.put("timestamp", System.currentTimeMillis());

                // Copy all card fields to shared data
                for (DataSnapshot child : snapshot.getChildren()) {
                    shareData.put(child.getKey(), child.getValue());
                }

                // Save to both allCards and recipient's sharedCards
                allCardsRef.child(cardId).setValue(shareData)
                        .addOnSuccessListener(aVoid -> {
                            sharedCardsRef.child(recipientUserId).child(cardId).setValue(true)
                                    .addOnSuccessListener(aVoid1 -> showToast("Card shared successfully"))
                                    .addOnFailureListener(e -> showToast("Failed to share card"));
                        })
                        .addOnFailureListener(e -> showToast("Failed to update card"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Database error: " + error.getMessage());
            }
        });
    }

    private void confirmDeleteCard() {
        if (getActivity() == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete this card?");

        builder.setPositiveButton("Delete", (dialog, which) -> deleteCard());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteCard() {
        if (cardId == null || currentUserId == null) {
            showToast("Invalid card or user");
            return;
        }

        // If this is a shared card (not owned by current user)
        if (originalOwnerId != null && !originalOwnerId.equals(currentUserId)) {
            sharedCardsRef.child(currentUserId).child(cardId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        notifyCardDeleted();
                        showToast("Shared card removed");
                    })
                    .addOnFailureListener(e -> showToast("Failed to remove shared card"));
            return;
        }

        // If this is our own card, delete from all locations
        DatabaseReference cardRef = userCardsRef.child(currentUserId).child(cardId);

        cardRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Remove from allCards
                    allCardsRef.child(cardId).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                // Remove from all sharedCards references
                                removeFromAllSharedCards();
                                notifyCardDeleted();
                                showToast("Card deleted successfully");
                            })
                            .addOnFailureListener(e -> showToast("Failed to delete card from allCards"));
                })
                .addOnFailureListener(e -> showToast("Failed to delete card"));
    }

    private void removeFromAllSharedCards() {
        sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        sharedCardsRef.child(userId).child(cardId).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Failed to clean up shared cards: " + error.getMessage());
            }
        });
    }

    private void notifyCardDeleted() {
        if (getActivity() instanceof CardActivity) {
            ((CardActivity) getActivity()).onCardDeleted(cardId);
            getActivity().finish();
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    public void onCardDeleted() {
        // Handle any fragment-specific cleanup
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

}