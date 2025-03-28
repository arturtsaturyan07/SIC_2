package com.example.sic_2;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CardFragment extends Fragment {

    private static final String TAG = "CardFragment";
    private static final String ARG_CARD_ID = "cardId";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final String CLOUDINARY_FOLDER = "secure_uploads";

    private RecyclerView publicationsRecyclerView;
    private PublicationsAdapter publicationsAdapter;
    private List<Publication> publicationsList;
    private DatabaseReference postsRef;
    private String cardId;
    private String currentUserId;
    private Uri imageUri;
    private Button addPhotoButton;
    private ProgressDialog progressDialog;
    private ValueEventListener publicationsListener;

    public static CardFragment newInstance(String cardId) {
        CardFragment fragment = new CardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CARD_ID, cardId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        initializeCloudinary();

        if (getArguments() != null) {
            cardId = getArguments().getString(ARG_CARD_ID);
            Log.d(TAG, "Card ID: " + cardId);
        }
        publicationsList = new ArrayList<>();
    }

    private void initializeCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "disiijbpp");
            config.put("api_key", "265226997838638");
            config.put("api_secret", "RsPtut3zPunRm-8Hwh8zRqQ8uG8");
            MediaManager.init(requireContext(), config);
            Log.d(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Cloudinary initialization failed", e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_card, container, false);
        initializeDependencies();
        initializeViews(view);
        return view;
    }

    private void initializeDependencies() {
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (cardId == null || cardId.isEmpty()) {
            showToast("Card ID is missing");
            Log.e(TAG, "Card ID is missing");
            requireActivity().finish();
            return;
        }

        if (currentUserId == null) {
            showToast("Please sign in first");
            Log.e(TAG, "User not authenticated");
            requireActivity().finish();
            return;
        }
    }

    private void initializeViews(View view) {
        publicationsRecyclerView = view.findViewById(R.id.publications_recycler_view);
        Button addPublicationButton = view.findViewById(R.id.add_publication_button);
        addPhotoButton = view.findViewById(R.id.add_photo_button);

        setupRecyclerView();
        setupButtonListeners(addPublicationButton);

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setCancelable(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - Loading publications");
        loadPublications();
    }

    private void setupRecyclerView() {
        publicationsAdapter = new PublicationsAdapter(publicationsList, requireContext(), currentUserId);
        publicationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        publicationsRecyclerView.setAdapter(publicationsAdapter);
    }

    private void setupButtonListeners(Button addPublicationButton) {
        addPublicationButton.setOnClickListener(v -> showAddPublicationDialog());
        addPhotoButton.setOnClickListener(v -> showImagePickerDialog());
    }

    private void showAddPublicationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Post");

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("What's on your mind?");
        builder.setView(input);

        builder.setPositiveButton("Post", (dialog, which) -> {
            String content = input.getText().toString().trim();
            if (!content.isEmpty()) {
                createPublication(content, null);
            } else {
                showToast("Post cannot be empty");
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadPublications() {
        Log.d(TAG, "Loading publications for card: " + cardId);

        // Clear existing listener if any
        if (publicationsListener != null) {
            postsRef.removeEventListener(publicationsListener);
        }

        postsRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(cardId);

        publicationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                publicationsList.clear();
                Log.d(TAG, "Found " + snapshot.getChildrenCount() + " publications");

                if (snapshot.exists()) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        try {
                            Publication publication = postSnapshot.getValue(Publication.class);
                            if (publication != null) {
                                publicationsList.add(0, publication); // Newest first
                                Log.d(TAG, "Loaded publication: " + publication.getContent()
                                        + " | Image: " + publication.getImageUrl());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing publication", e);
                        }
                    }
                    publicationsAdapter.updatePublications(publicationsList);
                } else {
                    Log.d(TAG, "No publications found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load publications: " + error.getMessage());
                showToast("Failed to load posts: " + error.getMessage());
            }
        };

        postsRef.orderByChild("timestamp").addValueEventListener(publicationsListener);
    }

    private void createPublication(String content, String imageUrl) {
        Log.d(TAG, "Creating new publication");

        DatabaseReference newPostRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(cardId)
                .push();

        Publication publication = new Publication(currentUserId, content, imageUrl, System.currentTimeMillis());

        newPostRef.setValue(publication)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String message = imageUrl != null ? "Image posted!" : "Post published!";
                        showToast(message);
                        Log.d(TAG, "Publication created successfully");
                    } else {
                        showToast("Failed to create post");
                        Log.e(TAG, "Publication creation failed", task.getException());
                    }
                });
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Photo")
                .setItems(new String[]{"Gallery", "Camera"}, (dialog, which) -> {
                    if (which == 0) openGallery();
                    else openCamera();
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                imageUri = Uri.fromFile(photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            Log.d(TAG, "Image file created: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            showToast("Error creating image");
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
                Log.d(TAG, "Image selected from gallery: " + imageUri);
            }

            if (imageUri != null) {
                Log.d(TAG, "Starting image upload");
                uploadImageToCloudinary();
            }
        }
    }

    private void uploadImageToCloudinary() {
        if (imageUri == null) {
            showToast("No image selected");
            return;
        }

        progressDialog.setMessage("Uploading image...");
        progressDialog.show();

        try {
            MediaManager.get().upload(imageUri)
                    .option("folder", CLOUDINARY_FOLDER)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Upload started");
                            requireActivity().runOnUiThread(() ->
                                    progressDialog.setMessage("Uploading..."));
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int progress = (int) ((100 * bytes) / totalBytes);
                            requireActivity().runOnUiThread(() ->
                                    progressDialog.setMessage("Uploading: " + progress + "%"));
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                String imageUrl = (String) resultData.get("secure_url");
                                if (imageUrl != null) {
                                    Log.d(TAG, "Image uploaded successfully: " + imageUrl);
                                    createPublication(null, imageUrl);
                                } else {
                                    Log.e(TAG, "Upload failed - no URL returned");
                                    showToast("Upload failed");
                                }
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Log.e(TAG, "Upload error: " + error.getDescription());
                                showToast("Upload failed: " + error.getDescription());
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.d(TAG, "Upload rescheduled");
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Upload failed", e);
            showToast("Upload failed");
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");

        // Clean up Firebase listener
        if (postsRef != null && publicationsListener != null) {
            postsRef.removeEventListener(publicationsListener);
        }
    }
}