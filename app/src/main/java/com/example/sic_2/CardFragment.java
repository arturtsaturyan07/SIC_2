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

    private static final String ARG_CARD_ID = "cardId";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final String CLOUDINARY_FOLDER = "secure_uploads";

    private RecyclerView publicationsRecyclerView;
    private PublicationsAdapter publicationsAdapter;
    private List<Publication> publicationsList;
    private DatabaseReference publicationsRef;
    private String cardId;
    private String currentUserId;
    private Uri imageUri;
    private Button addPhotoButton;
    private ProgressDialog progressDialog;

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
        initializeCloudinary();

        if (getArguments() != null) {
            cardId = getArguments().getString(ARG_CARD_ID);
        }
        publicationsList = new ArrayList<>();
    }

    private void initializeCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "disiijbpp");
            config.put("api_key", "265226997838638");
            config.put("api_secret", "RsPtut3zPunRm-8Hwh8zRqQ8uG8"); // WARNING: For development only!
            MediaManager.init(requireContext(), config);
        } catch (IllegalStateException e) {
            Log.d("Cloudinary", "Already initialized");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
            requireActivity().finish();
            return;
        }
        if (currentUserId == null) {
            showToast("User authentication failed");
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
        loadPublications();

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setCancelable(false);
    }

    private void setupRecyclerView() {
        publicationsAdapter = new PublicationsAdapter(publicationsList, requireContext());
        publicationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        publicationsRecyclerView.setAdapter(publicationsAdapter);
    }

    private void setupButtonListeners(Button addPublicationButton) {
        addPublicationButton.setOnClickListener(v -> showAddPublicationDialog());
        addPhotoButton.setOnClickListener(v -> showImagePickerDialog());
    }

    private void showAddPublicationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Publication");

        final android.widget.EditText contentInput = new android.widget.EditText(requireContext());
        contentInput.setHint("Enter publication content");
        builder.setView(contentInput);

        builder.setPositiveButton("Post", (dialog, which) -> {
            String content = contentInput.getText().toString().trim();
            if (!content.isEmpty()) {
                createPublication(content);
            } else {
                showToast("Publication content cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadPublications() {
        DatabaseReference postsRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(cardId);

        postsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                publicationsList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        Publication publication = postSnapshot.getValue(Publication.class);
                        if (publication != null) {
                            publicationsList.add(0, publication); // Newest first
                        }
                    }
                    publicationsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Failed to load posts");
                Log.e("FirebaseError", error.getMessage());
            }
        });
    }

    private void createPublication(String content) {
        DatabaseReference newPostRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(cardId)
                .push();

        Publication publication = new Publication(currentUserId, content, System.currentTimeMillis());
        newPostRef.setValue(publication)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Publication created successfully");
                    } else {
                        showToast("Failed to create publication");
                    }
                });
    }

    private void saveImageUrlToFirebase(String imageUrl) {
        DatabaseReference newPostRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(cardId)
                .push();

        Publication publication = new Publication(currentUserId, imageUrl, System.currentTimeMillis());
        newPostRef.setValue(publication)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Image posted successfully");
                    } else {
                        showToast("Failed to post image");
                    }
                });
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Choose Image Source")
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
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Log.e("FileCreation", "Error creating image file", e);
            showToast("Error creating image file");
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
            }
            if (imageUri != null) {
                uploadImageToCloudinary();
            }
        }
    }

    private void uploadImageToCloudinary() {
        if (imageUri == null) {
            showToast("No image selected");
            return;
        }

        progressDialog.setMessage("Preparing upload...");
        progressDialog.show();

        try {
            MediaManager.get().upload(imageUri)
                    .option("folder", CLOUDINARY_FOLDER)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            requireActivity().runOnUiThread(() ->
                                    progressDialog.setMessage("Upload started..."));
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
                                    saveImageUrlToFirebase(imageUrl);
                                } else {
                                    showToast("Upload failed: No URL returned");
                                }
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                showToast("Upload failed: " + error.getDescription());
                                Log.e("CloudinaryError", error.getDescription());
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.d("Upload", "Rescheduling upload");
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            progressDialog.dismiss();
            showToast("Error starting upload: " + e.getMessage());
            Log.e("UploadError", "Upload preparation failed", e);
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}