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

    private DatabaseReference database;

    public ParametersFragment() {
        // Required empty public constructor
    }

    public static ParametersFragment newInstance(String param1, String param2, String cardId) {
        ParametersFragment fragment = new ParametersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString("cardId", cardId); // Pass cardId to the fragment
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            cardId = getArguments().getString("cardId"); // Retrieve cardId
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference("cards");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_parameters, container, false);

        // Initialize buttons
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button shareButton = view.findViewById(R.id.shareButton);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button deleteButton = view.findViewById(R.id.deleteButton);

        // Set click listeners
        shareButton.setOnClickListener(v -> showShareDialog());
        deleteButton.setOnClickListener(v -> deleteCard());

        return view;
    }

    /**
     * Shows a dialog to share the card with another user.
     */
    private void showShareDialog() {
        // Create a dialog with an EditText for the recipient's user ID
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Share Card");

        final EditText userIdInput = new EditText(requireContext());
        userIdInput.setHint("Recipient User ID");
        builder.setView(userIdInput);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String recipientUserId = userIdInput.getText().toString().trim();
            if (!recipientUserId.isEmpty()) {
                shareCardWithUser(recipientUserId);
            } else {
                Toast.makeText(requireContext(), "User ID cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Shares the card with another user by updating the sharedCards node in Firebase.
     */
    private void shareCardWithUser(String recipientUserId) {
        if (recipientUserId == null || recipientUserId.isEmpty() || cardId == null || cardId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid user ID or card ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reference to the card in the current user's cards
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);

        // Reference to the sharedCards node for the recipient user
        DatabaseReference sharedCardsRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(recipientUserId)
                .child(cardId);

        // Fetch the card data
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String message = snapshot.child("message").getValue(String.class);
                    if (message != null) {
                        // Create a map to store the shared card data
                        Map<String, Object> shareData = new HashMap<>();
                        shareData.put("sharedBy", currentUserId); // ID of the user sharing the card
                        shareData.put("message", message); // Card message
                        shareData.put("timestamp", System.currentTimeMillis()); // Timestamp of sharing

                        // Save the shared card data to the recipient's sharedCards node
                        sharedCardsRef.setValue(shareData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(requireContext(), "Card shared successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to share card", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(requireContext(), "Card message is missing", Toast.LENGTH_SHORT).show();
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

    /**
     * Deletes the card from Firebase.
     */
    private void deleteCard() {
        if (cardId == null || cardId.isEmpty() || currentUserId == null) {
            Toast.makeText(requireContext(), "Invalid card or user", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cardRef = database.child(currentUserId).child(cardId);
        cardRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Card deleted successfully", Toast.LENGTH_SHORT).show();
                        requireActivity().finish(); // Close the activity after deletion
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete card", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}