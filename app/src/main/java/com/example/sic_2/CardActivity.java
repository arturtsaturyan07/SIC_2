package com.example.sic_2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardActivity extends AppCompatActivity {

    private DatabaseReference database;
    private TextView cardMessage;
    private String cardId;
    private String currentUserId;

    // Shared Cards RecyclerView
    private RecyclerView sharedCardsRecyclerView;
    private SharedCardsAdapter sharedCardsAdapter;
    private List<SharedCard> sharedCardList;

    // Publications RecyclerView
    private RecyclerView publicationsRecyclerView;
    private PublicationsAdapter publicationsAdapter;
    private List<Publication> publicationsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);



        Button addPhotoButton = findViewById(R.id.add_photo_button);
        addPhotoButton.setOnClickListener(v -> openImagePicker());

        // Initialize Firebase database and UI components
        database = FirebaseDatabase.getInstance().getReference("cards");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        cardId = getIntent().getStringExtra("cardId");

        // Validate card ID and user authentication
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            finish();
            return;
        }
        if (currentUserId == null) {
            showToast("User authentication failed");
            finish();
            return;
        }

        // Initialize UI components
        cardMessage = findViewById(R.id.card_message);
        ImageButton backButton = findViewById(R.id.back_button);
        Button addUserButton = findViewById(R.id.addUserButton);
        Button createEventButton = findViewById(R.id.create_event_button);
        Button shareCardButton = findViewById(R.id.shareCardButton);
        Button addPublicationButton = findViewById(R.id.add_publication_button);

        // Initialize Shared Cards RecyclerView
        sharedCardList = new ArrayList<>();
        sharedCardsAdapter = new SharedCardsAdapter(sharedCardList);
        sharedCardsRecyclerView = findViewById(R.id.shared_cards_recycler_view);
        sharedCardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sharedCardsRecyclerView.setAdapter(sharedCardsAdapter);

        // Initialize Publications RecyclerView
        publicationsList = new ArrayList<>();
        publicationsAdapter = new PublicationsAdapter(publicationsList);
        publicationsRecyclerView = findViewById(R.id.publications_recycler_view);
        publicationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        publicationsRecyclerView.setAdapter(publicationsAdapter);

        // Load data
        checkUserAccess();
        loadSharedCards();
        loadPublications();

        // Button functionalities
        backButton.setOnClickListener(v -> finish());
        addUserButton.setOnClickListener(v -> navigateTo(UserAddActivity.class));
        shareCardButton.setOnClickListener(v -> showShareDialog(cardId));
        addPublicationButton.setOnClickListener(v -> showAddPublicationDialog());

        // Bottom navigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_main) {
                    navigateTo(MainActivity.class);
                    return true;
                } else if (item.getItemId() == R.id.nav_chat) {
                    Intent intent = new Intent(CardActivity.this, ChatActivity.class);
                    intent.putExtra("cardId", cardId); // Pass the card ID to ChatActivity
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        } else {
            Log.e("CardActivity", "BottomNavigationView is null. Check your layout file.");
        }
    }


//    private void showAddPublicationDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Add Publication");
//
//        final EditText contentInput = new EditText(this);
//        contentInput.setHint("Enter publication content");
//        builder.setView(contentInput);
//
//        // Add a button to select a photo
//        Button selectPhotoButton = new Button(this);
//        selectPhotoButton.setText("Select Photo");
//        builder.setView(selectPhotoButton);
//
//        builder.setPositiveButton("Post", (dialog, which) -> {
//            String content = contentInput.getText().toString().trim();
//            if (!content.isEmpty()) {
//                createPublication(content, selectedImageUrl); // Pass the photo URL
//            } else {
//                showToast("Publication content cannot be empty");
//            }
//        });
//
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//
//        // Handle photo selection
//        selectPhotoButton.setOnClickListener(v -> openImagePicker());
//    }

    // Variable to store the selected photo's download URL
    private String selectedImageUrl;

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1); // Use startActivityForResult to handle the result
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Get the selected image URI
            Uri imageUri = data.getData();
            uploadImageToFirebase(imageUri);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        // Create a reference to Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("images/" + System.currentTimeMillis() + ".jpg");

        // Upload the image
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        selectedImageUrl = uri.toString(); // Save the download URL
                        showToast("Photo uploaded successfully");
                    }).addOnFailureListener(e -> {
                        showToast("Failed to get download URL");
                    });
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to upload photo");
                });
    }


    private void saveImageUrlToDatabase(String imageUrl) {
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            return;
        }

        DatabaseReference publicationsRef = FirebaseDatabase.getInstance()
                .getReference("publications")
                .child(cardId)
                .push();

        String publicationId = publicationsRef.getKey();
        if (publicationId != null) {
            Publication publication = new Publication(publicationId, currentUserId, "", imageUrl, System.currentTimeMillis());
            publicationsRef.setValue(publication)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showToast("Publication added successfully");
                            loadPublications(); // Reload publications
                        } else {
                            showToast("Failed to add publication");
                        }
                    });
        }
    }

    /**
     * Checks if the current user has access to the specified card.
     */
    private void checkUserAccess() {
        if (currentUserId == null || cardId == null) {
            showToast("Invalid user or card ID");
            finish();
            return;
        }

        // Reference to the card in the user's own cards
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);

        // Reference to the card in shared cards
        DatabaseReference sharedCardRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(currentUserId)
                .child(cardId);

        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    loadCardData();
                } else {
                    // Check if the card exists in shared cards
                    sharedCardRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot sharedSnapshot) {
                            if (sharedSnapshot.exists()) {
                                loadSharedCardData(sharedSnapshot);
                            } else {
                                showToast("Access denied: Card not found");
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("FirebaseError", "Error checking shared cards: " + error.getMessage());
                            showToast("Database error: " + error.getMessage());
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error checking user access: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
                finish();
            }
        });
    }

    /**
     * Loads the card data from Firebase.
     */
    private void loadCardData() {
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String message = snapshot.child("message").getValue(String.class);
                if (message != null && !message.isEmpty()) {
                    cardMessage.setText(message);
                } else {
                    showToast("Card data is incomplete");
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Failed to load card");
                Log.e("Firebase", "Failed to load card", error.toException());
                finish();
            }
        });
    }

    /**
     * Loads shared card data.
     */
    private void loadSharedCardData(DataSnapshot sharedSnapshot) {
        String message = sharedSnapshot.child("message").getValue(String.class);
        String sharedBy = sharedSnapshot.child("sharedBy").getValue(String.class);
        if (message != null && !message.isEmpty()) {
            cardMessage.setText("[Shared] " + message); // Indicate shared card
        } else {
            showToast("Shared card data is incomplete");
            finish();
        }
    }

    /**
     * Loads shared cards for the current user.
     */
    private void loadSharedCards() {
        if (currentUserId == null) {
            showToast("User authentication failed");
            return;
        }

        DatabaseReference sharedCardsRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(currentUserId);

        sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sharedCardList.clear(); // Clear existing data
                if (snapshot.exists()) {
                    for (DataSnapshot cardSnapshot : snapshot.getChildren()) {
                        String cardId = cardSnapshot.getKey();
                        String message = cardSnapshot.child("message").getValue(String.class);
                        String sharedBy = cardSnapshot.child("sharedBy").getValue(String.class);
                        if (message != null && sharedBy != null) {
                            SharedCard sharedCard = new SharedCard(cardId, message, sharedBy);
                            sharedCardList.add(sharedCard);
                        }
                    }
                    sharedCardsAdapter.notifyDataSetChanged(); // Refresh RecyclerView
                } else {
                    Log.d("SharedCard", "No shared cards found");
                    showToast("No shared cards found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading shared cards: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
            }
        });
    }

    /**
     * Loads publications for the current card.
     */
    private void loadPublications() {
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            return;
        }

        DatabaseReference publicationsRef = FirebaseDatabase.getInstance()
                .getReference("publications")
                .child(cardId);

        publicationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                            Publication publication = new Publication(publicationId, authorId, content, selectedImageUrl, timestamp);
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
                    showToast("No publications found");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading publications: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
            }
        });
    }

    /**
     * Shows a dialog to add a new publication.
     */
    private void showAddPublicationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Publication");

        final EditText contentInput = new EditText(this);
        contentInput.setHint("Enter publication content");
        builder.setView(contentInput);

        builder.setPositiveButton("Post", (dialog, which) -> {
            String content = contentInput.getText().toString().trim();
            if (!content.isEmpty()) {
                createPublication(content, selectedImageUrl);
            } else {
                showToast("Publication content cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Creates a new publication in Firebase.
     */
    private void createPublication(String content, String imageUrl) {
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            return;
        }

        DatabaseReference publicationsRef = FirebaseDatabase.getInstance()
                .getReference("publications")
                .child(cardId)
                .push();

        String publicationId = publicationsRef.getKey();
        if (publicationId != null) {
            Publication publication = new Publication(publicationId, currentUserId, content, imageUrl, System.currentTimeMillis());
            publicationsRef.setValue(publication)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showToast("Publication added successfully");
                            loadPublications(); // Reload publications
                        } else {
                            showToast("Failed to add publication");
                        }
                    });
        }
    }

    /**
     * Shows a dialog to share the card with another user.
     */
    private void showShareDialog(String cardId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share Card");

        final EditText userIdInput = new EditText(this);
        userIdInput.setHint("Recipient User ID");
        builder.setView(userIdInput);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String recipientUserId = userIdInput.getText().toString().trim();
            if (!recipientUserId.isEmpty()) {
                shareCardWithUser(recipientUserId, cardId);
            } else {
                showToast("User ID cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Shares the card with another user by updating the sharedCards node in Firebase.
     */
    private void shareCardWithUser(String userId, String cardId) {
        if (userId == null || userId.isEmpty() || cardId == null || cardId.isEmpty()) {
            Log.e("ShareCard", "Invalid user ID or card ID");
            return;
        }

        DatabaseReference sharedCardsRef = FirebaseDatabase.getInstance()
                .getReference("sharedCards")
                .child(userId)
                .child(cardId);

        DatabaseReference cardRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(currentUserId)
                .child(cardId);

        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String message = snapshot.child("message").getValue(String.class);
                    if (message != null) {
                        Map<String, Object> shareData = new HashMap<>();
                        shareData.put("sharedBy", currentUserId);
                        shareData.put("message", message);
                        shareData.put("timestamp", System.currentTimeMillis());

                        sharedCardsRef.setValue(shareData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("ShareCard", "Card successfully shared with user " + userId);
                                        showToast("Card shared successfully");
                                    } else {
                                        Log.e("ShareCard", "Failed to share card", task.getException());
                                        showToast("Failed to share card");
                                    }
                                });
                    } else {
                        Log.e("ShareCard", "Card message is missing");
                        showToast("Card message is missing");
                    }
                } else {
                    Log.e("ShareCard", "Card not found");
                    showToast("Card not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching card data: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
            }
        });
    }

    /**
     * Displays a short toast message.
     */
    private void showToast(String message) {
        Toast.makeText(CardActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Navigates to the specified activity.
     */
    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(CardActivity.this, activityClass);
        startActivity(intent);
        finish();
    }
}