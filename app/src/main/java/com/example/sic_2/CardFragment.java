package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class CardFragment extends Fragment {

    private static final String ARG_CARD_ID = "cardId"; // Key for the argument
    private RecyclerView publicationsRecyclerView;
    private PublicationsAdapter publicationsAdapter;
    private List<Publication> publicationsList;
    private DatabaseReference publicationsRef;
    private String cardId;
    private String currentUserId;

    // Factory method to create a new instance of CardFragment
    public static CardFragment newInstance(String cardId) {
        CardFragment fragment = new CardFragment(); // Create a new instance of the fragment
        Bundle args = new Bundle(); // Create a Bundle to hold the arguments
        args.putString(ARG_CARD_ID, cardId); // Add the cardId to the Bundle
        fragment.setArguments(args); // Set the arguments for the fragment
        return fragment; // Return the fragment
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the cardId from the arguments
        if (getArguments() != null) {
            cardId = getArguments().getString(ARG_CARD_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Validate card ID and user authentication
        if (cardId == null || cardId.isEmpty()) {
            Toast.makeText(requireContext(), "Card ID is missing", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return view;
        }
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User authentication failed", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return view;
        }

        // Initialize UI components
        publicationsRecyclerView = view.findViewById(R.id.publications_recycler_view);
        Button addPublicationButton = view.findViewById(R.id.add_publication_button);

        // Set up RecyclerView
        publicationsList = new ArrayList<>();
        publicationsAdapter = new PublicationsAdapter(publicationsList);
        publicationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        publicationsRecyclerView.setAdapter(publicationsAdapter);

        // Load publications
        loadPublications();

        // Add publication button click listener
        addPublicationButton.setOnClickListener(v -> showAddPublicationDialog());

        return view;
    }

    /**
     * Shows a dialog to add a new publication.
     */
    private void showAddPublicationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Add Publication");

        final android.widget.EditText contentInput = new android.widget.EditText(requireContext());
        contentInput.setHint("Enter publication content");
        builder.setView(contentInput);

        builder.setPositiveButton("Post", (dialog, which) -> {
            String content = contentInput.getText().toString().trim();
            if (!content.isEmpty()) {
                createPublication(content);
            } else {
                Toast.makeText(requireContext(), "Publication content cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Creates a new publication in Firebase.
     */
    private void createPublication(String content) {
        if (cardId == null || cardId.isEmpty()) {
            Toast.makeText(requireContext(), "Card ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference newPublicationRef = FirebaseDatabase.getInstance()
                .getReference("publications")
                .child(cardId)
                .push();

        String publicationId = newPublicationRef.getKey();
        if (publicationId != null) {
            Publication publication = new Publication(publicationId, currentUserId, content, System.currentTimeMillis());
            newPublicationRef.setValue(publication)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Publication added successfully", Toast.LENGTH_SHORT).show();
                            loadPublications(); // Reload publications
                        } else {
                            Toast.makeText(requireContext(), "Failed to add publication", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Loads publications for the current card.
     */
    private void loadPublications() {
        if (cardId == null || cardId.isEmpty()) {
            Toast.makeText(requireContext(), "Card ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        publicationsRef = FirebaseDatabase.getInstance()
                .getReference("publications")
                .child(cardId);

        publicationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                publicationsList.clear(); // Clear existing data

                if (snapshot.exists()) {
                    for (DataSnapshot publicationSnapshot : snapshot.getChildren()) {
                        String publicationId = publicationSnapshot.getKey();
                        String authorId = publicationSnapshot.child("authorId").getValue(String.class);
                        String content = publicationSnapshot.child("content").getValue(String.class);
                        Long timestamp = publicationSnapshot.child("timestamp").getValue(Long.class);

                        // Validate all required fields
                        if (publicationId != null && authorId != null && content != null && timestamp != null) {
                            Publication publication = new Publication(publicationId, authorId, content, timestamp);
                            publicationsList.add(publication);
                        } else {
                            Log.w("PublicationLoad", "Skipping invalid publication: " + publicationId);
                        }
                    }

                    // Notify adapter and scroll to the latest publication
                    publicationsAdapter.notifyDataSetChanged();
                    publicationsRecyclerView.scrollToPosition(publicationsList.size() - 1);
                } else {
                    Log.d("Publications", "No publications found");
                    Toast.makeText(requireContext(), "No publications found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading publications: " + error.getMessage());
                Toast.makeText(requireContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}