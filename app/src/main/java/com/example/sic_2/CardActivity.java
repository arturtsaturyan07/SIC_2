package com.example.sic_2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
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

    private String selectedImageUrl; // Global variable to store the image URL
    private StorageReference storageRef;

    // Shared Cards RecyclerView
    private RecyclerView sharedCardsRecyclerView;
    private SharedCardsAdapter sharedCardsAdapter;
    private List<SharedCard> sharedCardList;

    // Publications RecyclerView
    private RecyclerView publicationsRecyclerView;
    private PublicationsAdapter publicationsAdapter;
    private List<Publication> publicationsList;

    private FirebaseFirestore firestore;
    private CollectionReference publicationsRef;
    private CollectionReference photosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);

        // Initialize Firebase database and UI components
        database = FirebaseDatabase.getInstance().getReference("cards");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        cardId = getIntent().getStringExtra("cardId");

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();
        publicationsRef = firestore.collection("publications");
        photosRef = firestore.collection("photos");

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
        Button addPhotoButton = findViewById(R.id.add_photo_button);

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
        addPhotoButton.setOnClickListener(v -> openImagePicker());

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

    private void showAddPublicationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Note");

        final EditText contentInput = new EditText(this);
        contentInput.setHint("Enter note content");
        builder.setView(contentInput);

        builder.setPositiveButton("Post", (dialog, which) -> {
            String content = contentInput.getText().toString().trim();
            if (!content.isEmpty()) {
                createPublication(content, selectedImageUrl); // Pass the photo URL
            } else {
                showToast("Note content cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1); // Use startActivityForResult to handle the result
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData(); // Get the selected image URI
            if (imageUri != null) {
                uploadImageToFirebase(imageUri); // Upload the image to Firebase Storage
            } else {
                showToast("Failed to retrieve image URI");
            }
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) {
            showToast("Invalid image URI");
            return;
        }

        // Create a reference to Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("images/" + System.currentTimeMillis() + ".jpg");

        // Upload the image
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        selectedImageUrl = uri.toString(); // Save the download URL
                        Log.d("ImageUpload", "Image uploaded successfully: " + selectedImageUrl);
                        showToast("Photo uploaded successfully");

                        // Save the image URL to Firestore
                        saveImageUrlToFirestore(selectedImageUrl);
                    }).addOnFailureListener(e -> {
                        Log.e("ImageUpload", "Failed to get download URL", e);
                        showToast("Failed to get download URL");
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("ImageUpload", "Failed to upload photo", e);
                    showToast("Failed to upload photo");
                });
    }

    private void saveImageUrlToFirestore(String imageUrl) {
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            return;
        }

        // Create a new photo document
        Map<String, Object> photoData = new HashMap<>();
        photoData.put("authorId", currentUserId);
        photoData.put("cardId", cardId);
        photoData.put("imageUrl", imageUrl);
        photoData.put("timestamp", System.currentTimeMillis());

        // Add the photo to Firestore
        photosRef.add(photoData)
                .addOnSuccessListener(documentReference -> {
                    showToast("Photo saved successfully");
                    loadPhotos(); // Reload photos
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to save photo");
                    Log.e("SaveImage", "Failed to save photo", e);
                });
    }

    private void loadPhotos() {
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            return;
        }

        // Query photos for the specific card
        photosRef.whereEqualTo("cardId", cardId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        publicationsList.clear();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            String photoId = document.getId();
                            String authorId = document.getString("authorId");
                            String imageUrl = document.getString("imageUrl");
                            Long timestamp = document.getLong("timestamp");

                            if (photoId != null && authorId != null && timestamp != null) {
                                Publication publication = new Publication(
                                        photoId,
                                        authorId,
                                        "", // Empty content for photos
                                        imageUrl,
                                        timestamp
                                );
                                publicationsList.add(publication);
                            }
                        }
                        publicationsAdapter.notifyDataSetChanged();
                    } else {
                        showToast("Failed to load photos");
                        Log.e("LoadPhotos", "Failed to load photos", task.getException());
                    }
                });
    }

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
                publicationsList.clear();
                for (DataSnapshot publicationSnapshot : snapshot.getChildren()) {
                    String publicationId = publicationSnapshot.getKey();
                    String authorId = publicationSnapshot.child("authorId").getValue(String.class);
                    String content = publicationSnapshot.child("content").getValue(String.class);
                    String imageUrl = publicationSnapshot.child("imageUrl").getValue(String.class);
                    Long timestamp = publicationSnapshot.child("timestamp").getValue(Long.class);

                    if (publicationId != null && authorId != null && timestamp != null) {
                        Publication publication = new Publication(
                                publicationId,
                                authorId,
                                content,
                                imageUrl,
                                timestamp
                        );
                        publicationsList.add(publication);
                    }
                }
                publicationsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error loading publications: " + error.getMessage());
                showToast("Database error: " + error.getMessage());
            }
        });
    }

    private void createPublication(String content, String imageUrl) {
        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            return;
        }

        DatabaseReference publicationsRef = database.child("publications").child(cardId).push();
        String publicationId = publicationsRef.getKey();
        if (publicationId != null) {
            Publication publication = new Publication(publicationId, currentUserId, content, imageUrl, System.currentTimeMillis());
            publicationsRef.setValue(publication)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showToast("Note added successfully");
                            loadPublications(); // Reload publications
                        } else {
                            showToast("Failed to add note");
                        }
                    });
        }
    }

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

    private void showToast(String message) {
        Toast.makeText(CardActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(CardActivity.this, activityClass);
        startActivity(intent);
        finish();
    }
}