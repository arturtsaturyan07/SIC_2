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
    private DatabaseReference allCardsRef;
    private DatabaseReference campRequestsRef;
    private DatabaseReference usersRef;
    private String userId;
    private String userName;

    // Data
    private List<Card> cardList = new ArrayList<>();
    private Map<String, View> cardViewsMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupFirebase();
        setupListeners();
        loadUserData();
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
            allCardsRef = FirebaseDatabase.getInstance().getReference("allCards");
            campRequestsRef = FirebaseDatabase.getInstance().getReference("campRequests");
            usersRef = FirebaseDatabase.getInstance().getReference("users");
            loadData();
        }
    }

    private void loadUserData() {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userName = snapshot.child("name").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error loading user data", error.toException());
            }
        });
    }

    private void setupListeners() {
        fab.setOnClickListener(v -> showCardCreationDialog());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.startsWith("#camp ")) {
                    searchAllCards(query.substring(6).trim());
                    return true;
                }
                filterCards(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    filterCards("");
                }
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
        setupCampRequestListener();
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
                    Card card = data.getValue(Card.class);
                    if (card != null && card.getId() != null) {
                        if (!cardExists(card.getId())) {
                            cardList.add(card);
                            createAndAddCardView(card, true);
                        }
                    }
                }
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error loading shared cards", error.toException());
            }
        });
    }

    private void setupCampRequestListener() {
        campRequestsRef.child(userId).orderByChild("status").equalTo("pending")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            CampRequest request = requestSnapshot.getValue(CampRequest.class);
                            if (request != null && request.getStatus().equals("pending")) {
                                showCampRequestDialog(request);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeFragment", "Error loading camp requests", error.toException());
                    }
                });
    }

    private void searchAllCards(String query) {
        if (query.isEmpty()) return;

        showLoadingIndicator(true);
        allCardsRef.orderByChild("title").startAt(query).endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Card> searchResults = new ArrayList<>();
                        for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                            Card card = cardSnapshot.getValue(Card.class);
                            if (card != null && card.isCampCard() && !card.getAuthorId().equals(userId)) {
                                searchResults.add(card);
                            }
                        }
                        showLoadingIndicator(false);
                        showSearchResultsDialog(searchResults);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoadingIndicator(false);
                        Toast.makeText(getContext(), "Search failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSearchResultsDialog(List<Card> results) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Camp Cards Found");

        if (results.isEmpty()) {
            builder.setMessage("No camp cards found with this name");
            builder.setPositiveButton("OK", null);
        } else {
            String[] cardTitles = new String[results.size()];
            for (int i = 0; i < results.size(); i++) {
                cardTitles[i] = results.get(i).getTitle() + " (by " + results.get(i).getAuthorId() + ")";
            }

            builder.setItems(cardTitles, (dialog, which) -> {
                Card selectedCard = results.get(which);
                sendCampRequest(selectedCard);
            });
        }

        builder.show();
    }

    private void sendCampRequest(Card card) {
        if (userId == null || userName == null) {
            Toast.makeText(getContext(), "User information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String requestId = campRequestsRef.push().getKey();
        if (requestId == null) {
            Toast.makeText(getContext(), "Failed to create request", Toast.LENGTH_SHORT).show();
            return;
        }

        CampRequest request = new CampRequest(
                requestId,
                card.getId(),
                card.getTitle(),
                userId,
                userName,
                card.getAuthorId()
        );

        campRequestsRef.child(card.getAuthorId()).child(requestId).setValue(request)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request sent to camp members", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send request", Toast.LENGTH_SHORT).show();
                });
    }

    private void showCampRequestDialog(CampRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Camp Membership Request");
        builder.setMessage(request.getRequesterName() + " wants to join your camp for card: " + request.getCardTitle());

        builder.setPositiveButton("Accept", (dialog, which) -> {
            request.setStatus("approved");
            campRequestsRef.child(request.getOwnerId()).child(request.getRequestId()).setValue(request);

            // Add user to camp members
            allCardsRef.child(request.getCardId()).child("campMembers").child(request.getRequesterId()).setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        shareCardWithUser(request.getCardId(), request.getRequesterId());
                        Toast.makeText(getContext(), "Request approved", Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Reject", (dialog, which) -> {
            request.setStatus("rejected");
            campRequestsRef.child(request.getOwnerId()).child(request.getRequestId()).setValue(request);
            Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void shareCardWithUser(String cardId, String userId) {
        allCardsRef.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Card card = snapshot.getValue(Card.class);
                if (card != null) {
                    DatabaseReference sharedRef = FirebaseDatabase.getInstance()
                            .getReference("sharedCards")
                            .child(userId)
                            .child(cardId);

                    sharedRef.setValue(card)
                            .addOnSuccessListener(aVoid -> Log.d("HomeFragment", "Card shared successfully"))
                            .addOnFailureListener(e -> Log.e("HomeFragment", "Failed to share card", e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error fetching card to share", error.toException());
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

        final CheckBox campCheckbox = new CheckBox(getContext());
        campCheckbox.setText("This is a camp card (shareable with others)");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        layout.addView(titleInput);
        layout.addView(descInput);
        layout.addView(campCheckbox);

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String description = descInput.getText().toString().trim();
            boolean isCampCard = campCheckbox.isChecked();

            if (!title.isEmpty()) {
                createNewCard(title, description, isCampCard);
            } else {
                Toast.makeText(getContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void createNewCard(String title, String description, boolean isCampCard) {
        String cardId = database.push().getKey();
        if (cardId == null) {
            Toast.makeText(getContext(), "Failed to create card ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Card card = new Card(cardId, title, description, "Medium", userId, System.currentTimeMillis());
        card.setCampCard(isCampCard);

        database.child(cardId).setValue(card)
                .addOnSuccessListener(aVoid -> {
                    cardList.add(card);
                    createAndAddCardView(card, false);
                    updateEmptyState();
                    Toast.makeText(getContext(), "Card created", Toast.LENGTH_SHORT).show();

                    if (isCampCard) {
                        allCardsRef.child(cardId).setValue(card)
                                .addOnFailureListener(e -> Log.e("HomeFragment", "Failed to save camp card globally", e));
                    }
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
            String titleText = card.getTitle();
            if (isShared) titleText = "[Shared] " + titleText;
            if (card.isCampCard()) titleText = "🏕 " + titleText;

            recTitle.setText(titleText);
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
        intent.putExtra("isCampCard", card.isCampCard());
        if (isShared) {
            intent.putExtra("originalOwnerId", card.getAuthorId());
        }
        startActivity(intent);
    }

    private void filterCards(String query) {
        if (!isAdded()) return;

        clearNoResultsMessage();

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

        updateEmptyState();

        if (visibleCount == 0) {
            showNoResultsMessage();
        }
    }

    private void clearNoResultsMessage() {
        TextView noResultsView = cardContainer.findViewWithTag("no_results");
        if (noResultsView != null) {
            cardContainer.removeView(noResultsView);
        }
    }

    private void showNoResultsMessage() {
        TextView noResults = new TextView(getContext());
        noResults.setTag("no_results");
        noResults.setText("No cards found matching '" + searchView.getQuery() + "'");
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