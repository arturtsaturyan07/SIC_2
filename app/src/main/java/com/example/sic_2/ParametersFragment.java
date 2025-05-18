package com.example.sic_2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import yuku.ambilwarna.AmbilWarnaDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ParametersFragment extends Fragment {

    private String cardId;
    private String currentUserId;
    private String originalOwnerId;

    private DatabaseReference allCardsRef;
    private DatabaseReference sharedCardsRef;
    private DatabaseReference userCardsRef;
    private DatabaseReference cardAccessRef;

    // UI components
    private Switch switchNotifications, switchVisibility;
    private ToggleButton toggleFavorite;
    private Spinner categorySpinner, repeatSpinner;
    private Button colorBtn, shareBtn, deleteBtn, resetBtn;
    private EditText titleEditText, notesEditText;
    private Button renameBtn; // NEW: Button to confirm rename
    private View colorPreview;

    // Data
    private int selectedColor = Color.parseColor("#2196F3"); // Default color
    private String[] categories = {"None", "Work", "Fun", "School", "Personal"};
    private String[] repeatOptions = {"None", "Daily", "Weekly", "Monthly"};
    private boolean isOwner = false;

    public ParametersFragment() {}

    public static ParametersFragment newInstance(String param1, String param2, String cardId, String originalOwnerId) {
        ParametersFragment fragment = new ParametersFragment();
        Bundle args = new Bundle();
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
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        allCardsRef = database.getReference("allCards");
        sharedCardsRef = database.getReference("sharedCards");
        userCardsRef = database.getReference("cards");
        cardAccessRef = database.getReference("cardAccess");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parameters, container, false);

        // Find views
        switchNotifications = view.findViewById(R.id.switch_notifications);
        switchVisibility = view.findViewById(R.id.switch_visibility);
        toggleFavorite = view.findViewById(R.id.toggle_favorite);
        categorySpinner = view.findViewById(R.id.spinner_category);
        repeatSpinner = view.findViewById(R.id.spinner_repeat);
        colorBtn = view.findViewById(R.id.btn_color_picker);
        colorPreview = view.findViewById(R.id.color_preview);
        titleEditText = view.findViewById(R.id.edit_title);
        notesEditText = view.findViewById(R.id.edit_notes);
        shareBtn = view.findViewById(R.id.shareButton);
        deleteBtn = view.findViewById(R.id.deleteButton);
        resetBtn = view.findViewById(R.id.resetButton);
        renameBtn = view.findViewById(R.id.renameButton); // <-- Make sure you add this Button to your layout

        // Setup spinners
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(catAdapter);

        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, repeatOptions);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(repeatAdapter);

        // Setup color picker
        colorBtn.setOnClickListener(v -> openColorPicker());
        colorPreview.setBackgroundColor(selectedColor);

        // Hide title rename field and button for non-creators
        isOwner = (originalOwnerId == null || originalOwnerId.equals(currentUserId));
        if (!isOwner && titleEditText != null) {
            titleEditText.setVisibility(View.GONE);
        }
        if (!isOwner && renameBtn != null) {
            renameBtn.setVisibility(View.GONE);
        }

        // Load current state from Firebase
        loadCardPreferences();

        // Save changes in real time
        switchNotifications.setOnCheckedChangeListener((b, checked) -> savePreference("reminderEnabled", checked));
        switchVisibility.setOnCheckedChangeListener((b, checked) -> savePreference("archived", checked));
        toggleFavorite.setOnCheckedChangeListener((b, checked) -> savePreference("favorite", checked));
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { savePreference("category", categories[pos]); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        repeatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { savePreference("repeat", repeatOptions[pos]); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        notesEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) savePreference("notes", notesEditText.getText().toString());
        });

        // Only creator can rename and update the main title in DB via button
        if (isOwner && renameBtn != null && titleEditText != null) {
            renameBtn.setOnClickListener(v -> {
                String newTitle = titleEditText.getText().toString().trim();
                if (!newTitle.isEmpty()) {
                    updateCardTitleEverywhere(newTitle);
                } else {
                    showToast("Title cannot be empty.");
                }
            });
        }

        shareBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        shareBtn.setOnClickListener(v -> showShareDialog());
        deleteBtn.setOnClickListener(v -> confirmDeleteCard());
        resetBtn.setOnClickListener(v -> showResetDialog());

        return view;
    }

    private void openColorPicker() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(requireContext(), selectedColor, true,
                new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        selectedColor = color;
                        colorPreview.setBackgroundColor(color);
                        savePreference("color", color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // Do nothing or handle cancel
                    }
                }
        );
        dialog.show();
    }

    private void loadCardPreferences() {
        // Try from user's card, fallback to shared or allCards
        DatabaseReference cardRef = userCardsRef.child(currentUserId).child(cardId);
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    fillUiWithPrefs(snapshot);
                } else {
                    // Shared card or not found, try allCards
                    allCardsRef.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            fillUiWithPrefs(snapshot);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fillUiWithPrefs(DataSnapshot snapshot) {
        if (snapshot.child("reminderEnabled").exists())
            switchNotifications.setChecked(Boolean.TRUE.equals(snapshot.child("reminderEnabled").getValue(Boolean.class)));
        if (snapshot.child("archived").exists())
            switchVisibility.setChecked(Boolean.TRUE.equals(snapshot.child("archived").getValue(Boolean.class)));
        if (snapshot.child("favorite").exists())
            toggleFavorite.setChecked(Boolean.TRUE.equals(snapshot.child("favorite").getValue(Boolean.class)));

        String category = snapshot.child("category").getValue(String.class);
        if (category != null) categorySpinner.setSelection(Arrays.asList(categories).indexOf(category));

        String repeat = snapshot.child("repeat").getValue(String.class);
        if (repeat != null) repeatSpinner.setSelection(Arrays.asList(repeatOptions).indexOf(repeat));

        if (snapshot.child("color").exists()) {
            selectedColor = snapshot.child("color").getValue(Integer.class);
            colorPreview.setBackgroundColor(selectedColor);
        }

        if (snapshot.child("title").exists())
            titleEditText.setText(snapshot.child("title").getValue(String.class));
        if (snapshot.child("notes").exists())
            notesEditText.setText(snapshot.child("notes").getValue(String.class));
    }

    private void savePreference(String key, Object value) {
        DatabaseReference cardRef = isOwner
                ? userCardsRef.child(currentUserId).child(cardId)
                : sharedCardsRef.child(currentUserId).child(cardId);
        cardRef.child(key).setValue(value);

        // Keep allCards in sync for searchable/archivable
        if (key.equals("archived") || key.equals("favorite") || key.equals("category") || key.equals("color") || key.equals("customTitle")) {
            allCardsRef.child(cardId).child(key).setValue(value);
        }
    }

    // Only the creator can rename the main card title
    private void updateCardTitleEverywhere(String newTitle) {
        // User's own card
        userCardsRef.child(currentUserId).child(cardId).child("title").setValue(newTitle);
        // AllCards global reference
        allCardsRef.child(cardId).child("title").setValue(newTitle);

        // Update in all shared copies
        sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        sharedCardsRef.child(userId).child(cardId).child("title").setValue(newTitle);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        showToast("Card renamed for all participants");
    }

    private void showShareDialog() {
        if (getActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Share Card");
        final EditText userIdInput = new EditText(getActivity());
        userIdInput.setHint("Enter recipient's user ID");
        builder.setView(userIdInput);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String recipientUserId = userIdInput.getText().toString().trim();
            if (recipientUserId.isEmpty() || recipientUserId.equals(currentUserId)) {
                showToast("User ID invalid.");
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
        DatabaseReference cardRef = userCardsRef.child(currentUserId).child(cardId);
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) { showToast("Card not found"); return; }
                Map<String, Object> shareData = new HashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    shareData.put(child.getKey(), child.getValue());
                }
                shareData.put("id", cardId);
                shareData.put("originalOwnerId", currentUserId);
                shareData.put("sharedBy", currentUserId);
                shareData.put("timestamp", System.currentTimeMillis());
                allCardsRef.child(cardId).updateChildren(shareData);
                allCardsRef.child(cardId).child("campMembers").child(recipientUserId).setValue(true);
                cardAccessRef.child(cardId).child(recipientUserId).setValue(true);
                sharedCardsRef.child(recipientUserId).child(cardId).setValue(shareData)
                        .addOnSuccessListener(aVoid -> showToast("Card shared successfully"))
                        .addOnFailureListener(e -> showToast("Failed to share card"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { showToast("Database error: " + error.getMessage()); }
        });
    }

    private void confirmDeleteCard() {
        if (getActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
        if (!isOwner) {
            sharedCardsRef.child(currentUserId).child(cardId).removeValue()
                    .addOnSuccessListener(aVoid -> showToast("Shared card removed"))
                    .addOnFailureListener(e -> showToast("Failed to remove shared card"));
            cardAccessRef.child(cardId).child(currentUserId).removeValue();
            return;
        }
        userCardsRef.child(currentUserId).child(cardId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    allCardsRef.child(cardId).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                removeFromAllSharedCards();
                                cardAccessRef.child(cardId).removeValue();
                                showToast("Card deleted successfully");
                                if (getActivity() != null) getActivity().finish();
                            })
                            .addOnFailureListener(e -> showToast("Failed to delete card from allCards"));
                })
                .addOnFailureListener(e -> showToast("Failed to delete card"));
    }

    private void removeFromAllSharedCards() {
        sharedCardsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        sharedCardsRef.child(userId).child(cardId).removeValue();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showResetDialog() {
        if (getActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Reset Settings");
        builder.setMessage("Reset all card preferences to default?");
        builder.setPositiveButton("Reset", (dialog, which) -> resetPreferences());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void resetPreferences() {
        switchNotifications.setChecked(false);
        switchVisibility.setChecked(false);
        toggleFavorite.setChecked(false);
        categorySpinner.setSelection(0);
        repeatSpinner.setSelection(0);
        titleEditText.setText("");
        notesEditText.setText("");
        selectedColor = Color.parseColor("#2196F3");
        colorPreview.setBackgroundColor(selectedColor);

        // Remove from DB
        DatabaseReference cardRef = isOwner
                ? userCardsRef.child(currentUserId).child(cardId)
                : sharedCardsRef.child(currentUserId).child(cardId);
        for (String key : new String[]{"reminderEnabled", "archived", "favorite", "category", "repeat", "color", "customTitle", "notes"}) {
            cardRef.child(key).removeValue();
            allCardsRef.child(cardId).child(key).removeValue();
        }
        showToast("Preferences reset");
    }

    private void showToast(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}