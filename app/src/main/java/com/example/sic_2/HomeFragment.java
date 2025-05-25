package com.example.sic_2;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SearchView;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private List<Card> fullCardList = new ArrayList<>(); // All cards (including archived)
    private List<Card> cardList = new ArrayList<>();     // Only non-archived cards (shown by default)
    private Map<String, View> cardViewsMap = new HashMap<>();

    // Image selection/upload
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int PICK_IMAGE_UPDATE_REQUEST = 1002;
    private Uri imageUri = null;
    private ShapeableImageView dialogImageView = null;
    private Card pendingCardImageUpdate = null;
    private ShapeableImageView pendingUpdateImageView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupFirebase();
        initCloudinary();
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

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "disiijbpp");
            config.put("api_key", "265226997838638");
            config.put("api_secret", "RsPtut3zPunRm-8Hwh8zRqQ8uG8");
            MediaManager.init(requireContext().getApplicationContext(), config);
        } catch (IllegalStateException e) {
            // Already initialized, ignore
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (dialogImageView != null) {
                dialogImageView.setImageURI(imageUri);
            }
        }
        if (requestCode == PICK_IMAGE_UPDATE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri updateImageUri = data.getData();
            if (pendingCardImageUpdate != null && pendingUpdateImageView != null) {
                showToast("Uploading image...");
                uploadImageToCloudinary(updateImageUri, imageUrl -> {
                    if (imageUrl != null) {
                        updateCardImage(pendingCardImageUpdate, imageUrl, pendingUpdateImageView);
                    } else {
                        showToast("Image update failed.");
                    }
                    pendingCardImageUpdate = null;
                    pendingUpdateImageView = null;
                });
            }
        }
    }

    private void uploadImageToCloudinary(Uri uri, OnImageUploadedListener listener) {
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        listener.onUploaded(url);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        showToast("Image upload failed: " + error.getDescription());
                        listener.onUploaded(null);
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }
    private interface OnImageUploadedListener {
        void onUploaded(String imageUrl);
    }

    // Inside your HomeFragment

    private void showCardCreationDialog() {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.activity_upload, null, false);

        final ImageView uploadImage = dialogView.findViewById(R.id.uploadImage);
        final EditText uploadTopic = dialogView.findViewById(R.id.uploadTopic);
        final EditText uploadDesc = dialogView.findViewById(R.id.uploadDesc);
        // final EditText uploadLang = dialogView.findViewById(R.id.uploadLang);
        final Button saveButton = dialogView.findViewById(R.id.saveButton);

        // === Additional UI for your rich features ===
        final CheckBox shareableCheckbox = new CheckBox(requireContext());
        shareableCheckbox.setText("Make card shareable (camp card)");
        final CheckBox fullImageCheckbox = new CheckBox(requireContext());
        fullImageCheckbox.setText("Image covers entire card");

        final TextView startDateLabel = new TextView(requireContext());
        startDateLabel.setText("Start Date: Not set");
        final TextView endDateLabel = new TextView(requireContext());
        endDateLabel.setText("End Date: Not set");
        final Button pickStartDateBtn = new Button(requireContext());
        pickStartDateBtn.setText("Pick Start Date");
        final Button pickEndDateBtn = new Button(requireContext());
        pickEndDateBtn.setText("Pick End Date");

        final long[] startDateMillis = {0};
        final long[] endDateMillis = {0};

        pickStartDateBtn.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, dayOfMonth, 0, 0, 0);
                startDateMillis[0] = chosen.getTimeInMillis();
                startDateLabel.setText("Start Date: " + (month + 1) + "/" + dayOfMonth + "/" + year);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        pickEndDateBtn.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, dayOfMonth, 0, 0, 0);
                endDateMillis[0] = chosen.getTimeInMillis();
                endDateLabel.setText("End Date: " + (month + 1) + "/" + dayOfMonth + "/" + year);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        LinearLayout layout = (LinearLayout) ((CardView) ((ScrollView) dialogView).getChildAt(0)).getChildAt(0);
        layout.addView(shareableCheckbox, layout.indexOfChild(saveButton));
        layout.addView(fullImageCheckbox, layout.indexOfChild(saveButton));
        layout.addView(startDateLabel, layout.indexOfChild(saveButton));
        layout.addView(pickStartDateBtn, layout.indexOfChild(saveButton));
        layout.addView(endDateLabel, layout.indexOfChild(saveButton));
        layout.addView(pickEndDateBtn, layout.indexOfChild(saveButton));

        // === End additional UI ===

        // Handle image picker
        uploadImage.setOnClickListener(v -> {
            dialogImageView = (ShapeableImageView) uploadImage; // NO CAST!!
            imageUri = null;
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        saveButton.setOnClickListener(v -> {
            String topic = uploadTopic.getText().toString().trim();
            String description = uploadDesc.getText().toString().trim();
            //String language = uploadLang.getText().toString().trim();
            boolean isShareable = shareableCheckbox.isChecked();
            boolean isFullImage = fullImageCheckbox.isChecked();
            long startDate = startDateMillis[0];
            long endDate = endDateMillis[0];

            if (topic.isEmpty()) {
                showToast("Topic is required");
                return;
            }
            if (startDate == 0 || endDate == 0) {
                showToast("Please select start and end dates");
                return;
            }
            if (endDate < startDate) {
                showToast("End date cannot be before start date");
                return;
            }

            saveButton.setEnabled(false);

            if (imageUri != null) {
                uploadImageToCloudinary(imageUri, imageUrl -> {
                    if (imageUrl != null) {
                        createNewCard(topic, description, isShareable, startDate, endDate, imageUrl, isFullImage);
                    } else {
                        showToast("Image upload failed.");
                    }
                    dialog.dismiss();
                });
            } else {
                createNewCard(topic, description, isShareable, startDate, endDate, null, isFullImage);
                dialog.dismiss();
            }
        });
    }


    private void createNewCard(String title, String description, boolean isShareable, long startDate, long endDate, String imageUrl, boolean isFullImage) {
        String cardId = database.push().getKey();
        if (cardId == null) {
            showToast("Failed to create card ID");
            return;
        }

        Card card = new Card(cardId, title, description, "Medium", userId, System.currentTimeMillis());
        card.setCampCard(isShareable);
        card.setCampStartDate(startDate);
        card.setCampEndDate(endDate);
        card.setFullImageBackground(isFullImage);
        if (imageUrl != null) card.setImageUrl(imageUrl);

        database.child(cardId).setValue(card)
                .addOnSuccessListener(aVoid -> {
                    fullCardList.add(card);
                    cardList.add(card);
                    createAndAddCardView(card, false);
                    updateEmptyState();
                    showToast("Card created");

                    if (isShareable) {
                        allCardsRef.child(cardId).setValue(card)
                                .addOnFailureListener(e -> Log.e("HomeFragment", "Failed to save camp card globally", e));
                    }
                })
                .addOnFailureListener(e -> showToast("Failed to create card"));
        reloadData();
    }

    private void updateCardImage(Card card, String imageUrl, ShapeableImageView recImage) {
        card.setImageUrl(imageUrl);
        if (card.getAuthorId().equals(userId)) {
            database.child(card.getId()).child("imageUrl").setValue(imageUrl);
            if (card.isCampCard()) {
                allCardsRef.child(card.getId()).child("imageUrl").setValue(imageUrl);
            }
            showToast("Card image updated!");
            if (recImage != null && isAdded()) {
                Glide.with(requireContext()).load(imageUrl).into(recImage);
            }
        }
    }

    private void loadUserData() {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userName = snapshot.child("name").getValue(String.class);
                    if (userName == null || userName.isEmpty()) {
                        String email = snapshot.child("email").getValue(String.class);
                        userName = email != null ? email.split("@")[0] : "Anonymous";
                        usersRef.child(userId).child("name").setValue(userName);
                    }
                } else {
                    createUserProfile();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error loading user data", error.toException());
                userName = "Anonymous";
            }
        });
    }

    private void createUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String email = firebaseUser.getEmail();
            userName = email != null ? email.split("@")[0] : "Anonymous";

            Map<String, Object> user = new HashMap<>();
            user.put("name", userName);
            user.put("email", email != null ? email : "");

            usersRef.child(userId).setValue(user)
                    .addOnSuccessListener(aVoid -> Log.d("HomeFragment", "User profile created"))
                    .addOnFailureListener(e -> Log.e("HomeFragment", "Failed to create user profile", e));
        }
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
        fullCardList.clear();
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
                fullCardList.clear();
                cardList.clear();
                cardViewsMap.clear();
                cardContainer.removeAllViews();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Card card = data.getValue(Card.class);
                    if (card != null && card.getId() != null && !cardExists(card.getId(), fullCardList)) {
                        fullCardList.add(card);
                        createAndAddCardView(card, false); // ALWAYS create the view
                        if (!card.getArchived()) {
                            cardList.add(card);
                            View cardView = cardViewsMap.get(card.getId());
                            if (cardView != null) cardView.setVisibility(View.VISIBLE);
                        } else {
                            View cardView = cardViewsMap.get(card.getId());
                            if (cardView != null) cardView.setVisibility(View.GONE);
                        }
                    }
                }
                updateEmptyState();
                showLoadingIndicator(false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Error loading cards", error.toException());
                showLoadingIndicator(false);
                showToast("Failed to load cards");
            }
        });
    }

    private void loadSharedCards() {
        sharedCardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Object value = data.getValue();
                        if (value instanceof Map) {
                            Card card = data.getValue(Card.class);
                            if (card != null && card.getId() != null && !cardExists(card.getId(), fullCardList)) {
                                fullCardList.add(card);
                                createAndAddCardView(card, true); // ALWAYS create the view
                                if (!card.getArchived()) {
                                    cardList.add(card);
                                    View cardView = cardViewsMap.get(card.getId());
                                    if (cardView != null) cardView.setVisibility(View.VISIBLE);
                                } else {
                                    View cardView = cardViewsMap.get(card.getId());
                                    if (cardView != null) cardView.setVisibility(View.GONE);
                                }
                            }
                        } else {
                            Log.w("HomeFragment", "Skipping shared card (not a Card object): " + value);
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error parsing shared card", e);
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
                        if (!isAdded()) return;

                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            CampRequest request = requestSnapshot.getValue(CampRequest.class);
                            if (request != null && request.getStatus().equals("pending")) {
                                if (request.getOwnerId().equals(userId)) {
                                    // Show in-app dialog
                                    showCampRequestDialog(request);
                                    // Show notification in the bar
                                    NotificationUtils.showCampRequestNotification(requireContext(), request);
                                }
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
                        showToast("Search failed: " + error.getMessage());
                    }
                });
    }

    private void showSearchResultsDialog(List<Card> results) {
        if (!isAdded() || getContext() == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Camp Cards Found");

            if (results.isEmpty()) {
                builder.setMessage("No camp cards found with this name");
                builder.setPositiveButton("OK", null);
            } else {
                String[] cardTitles = new String[results.size()];
                for (int i = 0; i < results.size(); i++) {
                    cardTitles[i] = "[Shareable] " + results.get(i).getTitle() + " (by " + results.get(i).getAuthorId() + ")";
                }

                builder.setItems(cardTitles, (dialog, which) -> {
                    Card selectedCard = results.get(which);
                    sendCampRequest(selectedCard);
                });
            }

            builder.show();
        } catch (IllegalStateException e) {
            Log.e("HomeFragment", "Error showing search results dialog", e);
        }
    }

    private void sendCampRequest(Card card) {
        if (userId == null) {
            showToast("Please wait while we load your account");
            return;
        }

        String requestName = userName != null ? userName : "Anonymous User";
        String requestId = campRequestsRef.push().getKey();
        if (requestId == null) {
            showToast("Failed to create request");
            return;
        }

        CampRequest request = new CampRequest(
                requestId,
                card.getId(),
                card.getTitle(),
                userId,
                requestName,
                card.getAuthorId()
        );

        campRequestsRef.child(card.getAuthorId()).child(requestId).setValue(request)
                .addOnSuccessListener(aVoid -> showToast("Request sent to camp members"))
                .addOnFailureListener(e -> showToast("Failed to send request"));
    }

    private void showCampRequestDialog(CampRequest request) {
        if (!isAdded() || getContext() == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Camp Membership Request");
            builder.setMessage(request.getRequesterName() + " wants to join your camp for card: " + request.getCardTitle());

            builder.setPositiveButton("Accept", (dialog, which) -> {
                request.setStatus("approved");
                campRequestsRef.child(request.getOwnerId()).child(request.getRequestId()).setValue(request)
                        .addOnSuccessListener(aVoid -> {
                            allCardsRef.child(request.getCardId()).child("campMembers")
                                    .child(request.getRequesterId()).setValue(true)
                                    .addOnSuccessListener(aVoid1 -> {
                                        shareCardWithUser(request.getCardId(), request.getRequesterId());
                                        showToast("Request approved and card shared");
                                    });
                        });
            });

            builder.setNegativeButton("Reject", (dialog, which) -> {
                request.setStatus("rejected");
                campRequestsRef.child(request.getOwnerId()).child(request.getRequestId()).setValue(request);
                showToast("Request rejected");
            });

            builder.setCancelable(false);
            builder.show();
        } catch (IllegalStateException e) {
            Log.e("HomeFragment", "Error showing camp request dialog", e);
        }
    }

    public void shareCardWithUser(String cardId, String targetUserId) {
        database.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showToast("Card not found for sharing");
                    return;
                }
                Card card = snapshot.getValue(Card.class);
                if (card == null) {
                    showToast("Failed to parse card for sharing");
                    return;
                }

                Map<String, Object> sharedCard = new HashMap<>();
                sharedCard.put("id", card.getId());
                sharedCard.put("title", card.getTitle());
                sharedCard.put("description", card.getDescription());
                sharedCard.put("priority", card.getPriority());
                sharedCard.put("authorId", card.getAuthorId());
                sharedCard.put("timestamp", card.getTimestamp());
                sharedCard.put("isCampCard", card.isCampCard());
                sharedCard.put("sharedBy", userId);
                sharedCard.put("imageUrl", card.getImageUrl());

                // Preferences support
                sharedCard.put("archived", card.getArchived());
                sharedCard.put("favorite", card.getFavorite());
                sharedCard.put("category", card.getCategory());
                sharedCard.put("repeat", card.getRepeat());
                sharedCard.put("color", card.getColor());
                sharedCard.put("customTitle", card.getCustomTitle());
                sharedCard.put("notes", card.getNotes());
                sharedCard.put("reminderEnabled", card.getReminderEnabled());

                DatabaseReference sharedRef = FirebaseDatabase.getInstance()
                        .getReference("sharedCards")
                        .child(targetUserId)
                        .child(cardId);

                sharedRef.setValue(sharedCard)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Sharing", "Card successfully shared");
                            showToast("Card shared with user");
                            sendRefreshSignal(targetUserId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Sharing", "Failed to share card", e);
                            showToast("Failed to share card");
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Sharing", "Error sharing card", error.toException());
            }
        });
    }

    private void sendRefreshSignal(String userId) {
        // Implement notification or UI refresh if needed for userId
    }
    private void openCardStories(Card card) {
        CardStoryViewerDialog dialog = CardStoryViewerDialog.newInstance(card.getId(), userId);
        dialog.show(getChildFragmentManager(), "card_stories");
    }

    private void createAndAddCardView(Card card, boolean isShared) {
        if (!isAdded() || getContext() == null) return;
        try {
            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.rec_card, cardContainer, false);
            ShapeableImageView recImage = cardView.findViewById(R.id.recImage);
            TextView recTitle = cardView.findViewById(R.id.recTitle);
            TextView recPriority = cardView.findViewById(R.id.recPriority);
            TextView recDesc = cardView.findViewById(R.id.recDesc);
            ImageView starView = cardView.findViewById(R.id.starView);

            // Story ring logic
            View storyRing = cardView.findViewById(R.id.storyRing);
            if (storyRing != null) {
                DatabaseReference storiesRef = FirebaseDatabase.getInstance().getReference("stories").child(card.getId());
                long twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
                storiesRef.orderByChild("timestamp").startAt(twentyFourHoursAgo)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                storyRing.setVisibility(snapshot.exists() ? View.VISIBLE : View.GONE);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            // Optional: Story badge indicator (add story)
            ImageView storyBadge = cardView.findViewById(R.id.storyBadge);
            if (storyBadge != null) {
                storyBadge.setVisibility(View.GONE); // Only show if you want an "add" button
            }

            String titleText = card.getCustomTitle() != null && !card.getCustomTitle().isEmpty() ? card.getCustomTitle() : card.getTitle();
            if (isShared) titleText = "[Shared] " + titleText;
            recTitle.setText(titleText);

            recPriority.setText(card.isCampCard() ? "Shareable Card" : "Local Card");
            recDesc.setText(card.getNotes() != null && !card.getNotes().isEmpty() ? card.getNotes() : card.getDescription());

            if (card.getImageUrl() != null && !card.getImageUrl().isEmpty()) {
                if (card.isFullImageBackground()) {
                    recImage.setVisibility(View.GONE);
                    Glide.with(requireContext())
                            .load(card.getImageUrl())
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    cardView.setBackground(resource);
                                }
                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {}
                            });
                } else {
                    recImage.setVisibility(View.VISIBLE);
                    Glide.with(requireContext()).load(card.getImageUrl()).into(recImage);
                    cardView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_background));
                }
            } else {
                recImage.setVisibility(View.VISIBLE);
                recImage.setImageResource(R.drawable.uploadimg);
                cardView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_background));
            }

            if (starView != null) {
                starView.setVisibility(card.getFavorite() ? View.VISIBLE : View.GONE);
            }

            if (card.getColor() != null) {
                cardView.setBackgroundColor(card.getColor());
            } else if (isShared) {
                cardView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.shared_card_bg));
            }

            // Stories: Always tap to open story dialog
            recImage.setOnClickListener(v -> openCardStories(card));
            recImage.setAlpha(0.8f);
            recImage.setFocusable(true);
            recImage.setContentDescription("Tap to view/add stories");

            // Author: Long-press to update card image
            if (card.getAuthorId() != null && card.getAuthorId().equals(userId)) {
                recImage.setOnLongClickListener(v -> {
                    pendingCardImageUpdate = card;
                    pendingUpdateImageView = recImage;
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, PICK_IMAGE_UPDATE_REQUEST);
                    return true;
                });
            } else {
                recImage.setOnLongClickListener(null);
            }

            if (card.getArchived()) {
                cardView.setAlpha(0.4f);
            } else {
                cardView.setAlpha(1.0f);
            }

            cardView.setOnClickListener(v -> openCardDetails(card, isShared));

            cardContainer.addView(cardView);
            cardViewsMap.put(card.getId(), cardView);
        } catch (Exception e) {
            Log.e("HomeFragment", "Error creating card view", e);
        }
    }


    private void openCardDetails(Card card, boolean isShared) {
        if (!isAdded()) return;

        Intent intent = new Intent(requireContext(), CardActivity.class);
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
            for (Card card : cardList) {
                View cardView = cardViewsMap.get(card.getId());
                if (cardView != null) cardView.setVisibility(View.VISIBLE);
            }
            // Hide all archived cards
            for (Card card : fullCardList) {
                if (card.getArchived()) {
                    View cardView = cardViewsMap.get(card.getId());
                    if (cardView != null) cardView.setVisibility(View.GONE);
                }
            }
            updateEmptyState();
            return;
        }

        int visibleCount = 0;
        String lowerCaseQuery = query.toLowerCase();

        // Show all cards that match the search, even archived ones
        for (Card card : fullCardList) {
            View cardView = cardViewsMap.get(card.getId());
            if (cardView == null) continue;
            boolean matches = (card.getTitle() != null && card.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                    (card.getDescription() != null && card.getDescription().toLowerCase().contains(lowerCaseQuery)) ||
                    (card.getNotes() != null && card.getNotes().toLowerCase().contains(lowerCaseQuery)) ||
                    (card.getCustomTitle() != null && card.getCustomTitle().toLowerCase().contains(lowerCaseQuery));

            if (matches) {
                cardView.setVisibility(View.VISIBLE);
                visibleCount++;

                // Fade if archived, normal if not
                if (card.getArchived()) {
                    cardView.setAlpha(0.4f);
                } else {
                    cardView.setAlpha(1.0f);
                }
            } else {
                cardView.setVisibility(View.GONE);
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
        if (!isAdded() || getContext() == null) return;

        TextView noResults = new TextView(requireContext());
        noResults.setTag("no_results");
        noResults.setText("No cards found matching '" + searchView.getQuery() + "'");
        noResults.setTextSize(16);
        noResults.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
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
        Iterator<Card> fullIterator = fullCardList.iterator();
        while (fullIterator.hasNext()) {
            Card card = fullIterator.next();
            if (card != null && cardId.equals(card.getId())) {
                fullIterator.remove();
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

    private boolean cardExists(String cardId, List<Card> list) {
        if (cardId == null || list == null) return false;
        for (Card card : list) {
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

    private void showToast(String message) {
        if (!isAdded() || getContext() == null) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            loadData();
            setupSharedCardsListener();
        }
    }

    private void setupSharedCardsListener() {
        sharedCardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadSharedCards();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Shared cards listener cancelled", error.toException());
            }
        });
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