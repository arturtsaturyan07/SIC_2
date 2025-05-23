package com.example.sic_2;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CardFragment extends Fragment implements PublicationsAdapter.PublicationActionListener {

    private static final String TAG = "CardFragment";
    private static final String ARG_CARD_ID = "cardId";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_EDIT_IMAGE = 3;
    private static final int REQUEST_CODE_STORAGE = 1009;
    private static final String CLOUDINARY_FOLDER = "secure_uploads";
    private int pendingAction = 0; // 1 = gallery, 2 = camera, 3 = editImage

    private RecyclerView publicationsRecyclerView;
    private PublicationsAdapter publicationsAdapter;
    private List<Publication> publicationsList;
    private DatabaseReference postsRef;
    private String cardId;
    private String currentUserId;
    private Uri imageUri;
    private Uri editImageUri;
    private ProgressDialog progressDialog;
    private ValueEventListener publicationsListener;

    private TextView emptyStateText;
    private Publication editTargetPublication;

    // New: Input bar
    private EditText publicationInput;
    private ImageButton addPhotoButton, addPublicationButton;

    public static CardFragment newInstance(String cardId, String originalOwnerId) {
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
        boolean cloudinaryInitialized = false;
        if (cloudinaryInitialized) return;
        try {
            MediaManager.get();
            cloudinaryInitialized = true;
        } catch (IllegalStateException e) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "disiijbpp");
            config.put("api_key", "265226997838638");
            config.put("api_secret", "RsPtut3zPunRm-8Hwh8zRqQ8uG8");
            try {
                MediaManager.init(requireContext().getApplicationContext(), config);
                cloudinaryInitialized = true;
            } catch (Exception ex) {
                Toast.makeText(requireContext(), "Cloudinary init failed", Toast.LENGTH_SHORT).show();
            }
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
            showToast("Please sign in first");
            requireActivity().finish();
            return;
        }
    }

    private void initializeViews(View view) {
        publicationsRecyclerView = view.findViewById(R.id.publications_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state);
        publicationInput = view.findViewById(R.id.publication_input);
        addPhotoButton = view.findViewById(R.id.add_photo_button);
        addPublicationButton = view.findViewById(R.id.add_publication_button);
        setupRecyclerView();
        setupInputBarListeners();
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setCancelable(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadPublications();
    }

    private void setupRecyclerView() {
        publicationsAdapter = new PublicationsAdapter(publicationsList, requireContext(), cardId, this);
        publicationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        publicationsRecyclerView.setAdapter(publicationsAdapter);
    }

    private void setupInputBarListeners() {
        addPublicationButton.setOnClickListener(v -> {
            String content = publicationInput.getText().toString().trim();
            if (!content.isEmpty()) {
                createPublication(content, null);
                publicationInput.setText("");
            } else {
                showToast("Post cannot be empty");
            }
        });
        addPhotoButton.setOnClickListener(v -> showImagePickerDialog());
    }

    private void showAddPublicationDialog() {
        // Not needed with new input bar (left for possible future use)
    }

    private void loadPublications() {
        if (postsRef != null && publicationsListener != null) {
            postsRef.removeEventListener(publicationsListener);
        }
        postsRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(cardId);
        publicationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                publicationsList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        Publication publication = postSnapshot.getValue(Publication.class);
                        if (publication != null) {
                            publication.setId(postSnapshot.getKey());
                            publication.setCardId(cardId);
                            publicationsList.add(0, publication);
                        }
                    }
                }
                publicationsAdapter.updatePublications(publicationsList);
                updateEmptyState();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Failed to load posts: " + error.getMessage());
                publicationsList.clear();
                publicationsAdapter.updatePublications(publicationsList);
                updateEmptyState();
            }
        };
        postsRef.orderByChild("timestamp").addValueEventListener(publicationsListener);
    }

    private void updateEmptyState() {
        if (emptyStateText != null) {
            if (publicationsList.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                publicationsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                publicationsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void createPublication(String content, String imageUrl) {
        DatabaseReference newPostRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(cardId)
                .push();
        if (content == null) content = "";
        Publication publication = new Publication(currentUserId, content, imageUrl, System.currentTimeMillis());
        newPostRef.setValue(publication)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast((imageUrl != null && !imageUrl.isEmpty()) ? "Image posted!" : "Post published!");
                    } else {
                        showToast("Failed to create post");
                    }
                });
    }

    // --- Storage Permission Support ---
    private void showImagePickerDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Photo")
                .setItems(new String[]{"Gallery", "Camera"}, (dialog, which) -> {
                    if (which == 0) {
                        if (needStoragePermission()) {
                            pendingAction = 1;
                            requestStoragePermission();
                        } else {
                            openGallery();
                        }
                    } else {
                        if (needStoragePermission()) {
                            pendingAction = 2;
                            requestStoragePermission();
                        } else {
                            openCamera();
                        }
                    }
                })
                .show();
    }

    private boolean needStoragePermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingAction == 1) openGallery();
                else if (pendingAction == 2) openCamera();
                else if (pendingAction == 3) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_EDIT_IMAGE);
                }
            } else {
                Toast.makeText(requireContext(), "Storage permission denied. Cannot add/edit photo.", Toast.LENGTH_LONG).show();
            }
            pendingAction = 0;
        }
    }
    // --- End Storage Permission Support ---

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                // Use FileProvider for Android 7.0+ (API 24+)
                imageUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile
                );
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
            }
            if (requestCode == REQUEST_IMAGE_CAPTURE && imageUri == null && data != null) {
                imageUri = data.getData();
            }
            if (requestCode == REQUEST_EDIT_IMAGE && data != null) {
                editImageUri = data.getData();
                if (editTargetPublication != null && editImageUri != null) {
                    uploadEditImageToCloudinary();
                }
            }
            if (imageUri != null && requestCode != REQUEST_EDIT_IMAGE) {
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
                            requireActivity().runOnUiThread(() -> progressDialog.setMessage("Uploading..."));
                        }
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int progress = (int) ((100 * bytes) / totalBytes);
                            requireActivity().runOnUiThread(() -> progressDialog.setMessage("Uploading: " + progress + "%"));
                        }
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                String imageUrl = (String) resultData.get("secure_url");
                                if (imageUrl != null) {
                                    createPublication("", imageUrl);
                                    imageUri = null;
                                } else {
                                    showToast("Upload failed");
                                }
                            });
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                showToast("Upload failed: " + error.getDescription());
                            });
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        } catch (Exception e) {
            progressDialog.dismiss();
            showToast("Upload failed");
        }
    }

    private void uploadEditImageToCloudinary() {
        if (editImageUri == null || editTargetPublication == null) return;
        progressDialog.setMessage("Uploading image...");
        progressDialog.show();
        try {
            MediaManager.get().upload(editImageUri)
                    .option("folder", CLOUDINARY_FOLDER)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                String newImageUrl = (String) resultData.get("secure_url");
                                if (newImageUrl != null) {
                                    updatePublicationImage(editTargetPublication, newImageUrl);
                                    editImageUri = null;
                                } else {
                                    showToast("Upload failed");
                                }
                            });
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                showToast("Upload failed: " + error.getDescription());
                            });
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        } catch (Exception e) {
            progressDialog.dismiss();
            showToast("Upload failed");
        }
    }

    private void updatePublicationImage(Publication publication, String imageUrl) {
        FirebaseDatabase.getInstance().getReference("posts")
                .child(cardId)
                .child(publication.getId())
                .child("imageUrl")
                .setValue(imageUrl)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Image updated!");
                    } else {
                        showToast("Failed to update image.");
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postsRef != null && publicationsListener != null) {
            postsRef.removeEventListener(publicationsListener);
        }
    }

    // PublicationsAdapter.PublicationActionListener
    @Override
    public void onEditImageRequest(Publication publication) {
        this.editTargetPublication = publication;
        if (needStoragePermission()) {
            pendingAction = 3;
            requestStoragePermission();
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_EDIT_IMAGE);
        }
    }
}