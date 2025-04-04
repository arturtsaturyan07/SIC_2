package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class HomeFragment extends Fragment {
    // UI Components
    private ExtendedFloatingActionButton fab;
    private LinearLayout cardContainer;
    private SearchView searchView;
    private View emptyStateView;
    private ProgressBar progressBar;

    // Firebase
    private DatabaseReference database;
    private DatabaseReference sharedCardsRef;
    private String userId;

    // Data
    private List<Card> cardList = new ArrayList<>();
    private Map<String, View> cardViewsMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupFirebase();
        setupListeners();
        return view;
    }

    private void initializeViews(View view) {
        fab = view.findViewById(R.id.fab);
        cardContainer = view.findViewById(R.id.card_container);
        searchView = view.findViewById(R.id.search);
        emptyStateView = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            database = FirebaseDatabase.getInstance().getReference("cards").child(userId);
            sharedCardsRef = FirebaseDatabase.getInstance().getReference("sharedCards").child(userId);
            loadData();
        }
    }

    private void setupListeners() {
        fab.setOnClickListener(v -> showCardCreationDialog());

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

    private void loadData() {
        showLoadingIndicator(true);
        cardContainer.removeAllViews();
        cardList.clear();
        cardViewsMap.clear();

        loadUserCards();
        loadSharedCards();
    }

    private void loadUserCards() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Card card = data.getValue(Card.class);
                    if (card != null && card.getId() != null && !cardExists(card.getId())) {
                        cardList.add(card);
                        createAndAddCardView(card, false);
                    }
                }
                updateEmptyState();
                showLoadingIndicator(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error loading cards", error.toException());
                showLoadingIndicator(false);
                Toast.makeText(getContext(), "Failed to load cards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSharedCards() {
        sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getActivity() == null) return;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String sharedBy = data.child("sharedBy").getValue(String.class);
                    Card card = data.getValue(Card.class);

                    if (card != null && card.getId() != null && sharedBy != null) {
                        verifyAndAddSharedCard(card, sharedBy);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error loading shared cards", error.toException());
            }
        });
    }

    private void verifyAndAddSharedCard(Card card, String sharedBy) {
        if (sharedBy == null || sharedBy.isEmpty() || card == null || card.getId() == null) {
            Log.e("HomeFragment", "Invalid shared card data");
            return;
        }

        DatabaseReference originalCardRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(sharedBy)
                .child(card.getId());

        originalCardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    getActivity().runOnUiThread(() -> {
                        if (!cardExists(card.getId())) {
                            cardList.add(card);
                            createAndAddCardView(card, true); // Mark as shared
                            updateEmptyState();
                        }
                    });
                } else {
                    // Remove invalid shared card reference
                    sharedCardsRef.child(card.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Log.d("HomeFragment", "Removed invalid shared card"))
                            .addOnFailureListener(e -> Log.e("HomeFragment", "Failed to remove shared card", e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error verifying shared card", error.toException());
            }
        });
    }

    private void showCardCreationDialog() {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create New Card");

        final EditText titleInput = new EditText(getContext());
        titleInput.setHint("Title");
        titleInput.setSingleLine();

        final EditText descInput = new EditText(getContext());
        descInput.setHint("Description (Optional)");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        layout.addView(titleInput);
        layout.addView(descInput);

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String description = descInput.getText().toString().trim();

            if (!title.isEmpty()) {
                createNewCard(title, description);
            } else {
                Toast.makeText(getContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void createNewCard(String title, String description) {
        String cardId = database.push().getKey();
        if (cardId == null) {
            Toast.makeText(getContext(), "Failed to create card ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Card card = new Card(cardId, title, description, "Medium", userId, System.currentTimeMillis());
        database.child(cardId).setValue(card)
                .addOnSuccessListener(aVoid -> {
                    cardList.add(card);
                    createAndAddCardView(card, false);
                    updateEmptyState();
                    Toast.makeText(getContext(), "Card created", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to create card", Toast.LENGTH_SHORT).show();
                });
    }

    private void createAndAddCardView(Card card, boolean isShared) {
        if (!isAdded() || getContext() == null) return;

        try {
            View cardView = LayoutInflater.from(getContext()).inflate(R.layout.rec_card, cardContainer, false);
            ShapeableImageView recImage = cardView.findViewById(R.id.recImage);
            TextView recTitle = cardView.findViewById(R.id.recTitle);
            TextView recPriority = cardView.findViewById(R.id.recPriority);
            TextView recDesc = cardView.findViewById(R.id.recDesc);

            // Set card details
            recTitle.setText(isShared ? "[Shared] " + card.getTitle() : card.getTitle());
            recDesc.setText(card.getDescription());
            recPriority.setText(card.getPriority());
            recImage.setImageResource(R.drawable.uploadimg);

            // Highlight shared cards visually
            if (isShared) {
                cardView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.shared_card_bg));
            }

            // Set click listener to open card details
            cardView.setOnClickListener(v -> openCardDetails(card, isShared));

            // Add the card view to the container
            cardContainer.addView(cardView);
            cardViewsMap.put(card.getId(), cardView);
        } catch (Exception e) {
            Log.e("HomeFragment", "Error creating card view", e);
        }
    }

    private void openCardDetails(Card card, boolean isShared) {
        if (!isAdded()) return;

        Intent intent = new Intent(getContext(), CardActivity.class);
        intent.putExtra("cardId", card.getId());
        if (isShared) {
            intent.putExtra("originalOwnerId", card.getAuthorId());
        }
        startActivity(intent);
    }

    private void filterCards(String query) {
        if (!isAdded()) return;

        if (query.isEmpty()) {
            for (View cardView : cardViewsMap.values()) {
                cardView.setVisibility(View.VISIBLE);
            }
            updateEmptyState();
            return;
        }

        int visibleCount = 0;
        String lowerCaseQuery = query.toLowerCase();

        for (Card card : cardList) {
            View cardView = cardViewsMap.get(card.getId());
            if (cardView != null) {
                boolean matches = (card.getTitle() != null && card.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                        (card.getDescription() != null && card.getDescription().toLowerCase().contains(lowerCaseQuery));

                cardView.setVisibility(matches ? View.VISIBLE : View.GONE);
                if (matches) visibleCount++;
            }
        }

        if (visibleCount == 0) {
            showNoResultsMessage();
        }
    }

    private void showNoResultsMessage() {
        TextView noResults = new TextView(getContext());
        noResults.setText("No matching cards found");
        noResults.setTextSize(16);
        noResults.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
        noResults.setGravity(Gravity.CENTER);
        noResults.setPadding(0, 32, 0, 0);
        cardContainer.addView(noResults);
    }

    public void onCardDeleted(String cardId) {
        if (!isAdded() || cardId == null) return;

        View cardView = cardViewsMap.remove(cardId);
        if (cardView != null) {
            cardContainer.removeView(cardView);
        }

        Iterator<Card> iterator = cardList.iterator();
        while (iterator.hasNext()) {
            Card card = iterator.next();
            if (card != null && cardId.equals(card.getId())) {
                iterator.remove();
                break;
            }
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (!isAdded()) return;

        boolean isEmpty = cardList.isEmpty();
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cardContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private boolean cardExists(String cardId) {
        if (cardId == null || cardList == null) return false;

        for (Card card : cardList) {
            if (card != null && cardId.equals(card.getId())) {
                return true;
            }
        }
        return false;
    }

    private void showLoadingIndicator(boolean show) {
        if (!isAdded() || progressBar == null) return;
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            loadData();
        }
    }

    public void reloadData() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            showLoadingIndicator(true);
            cardContainer.removeAllViews();
            cardList.clear();
            cardViewsMap.clear();
            loadUserCards();
            loadSharedCards();
        });
    }
}