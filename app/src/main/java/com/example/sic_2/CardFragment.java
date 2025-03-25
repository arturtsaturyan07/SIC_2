package com.example.sic_2;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.database.Cursor;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.inappmessaging.model.ImageData;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CardFragment extends Fragment {

    private static final String ARG_CARD_ID = "cardId"; // Key for the argument
    private RecyclerView publicationsRecyclerView;
    private PublicationsAdapter publicationsAdapter;
    private List<Publication> publicationsList;
    private DatabaseReference publicationsRef;
    private String cardId;
    private String currentUserId;
    private Uri imageUri;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

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

        // In CardFragment.java
        publicationsAdapter = new PublicationsAdapter(publicationsList, currentUserId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card, container, false);

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;


        Button addPhotoButton = view.findViewById(R.id.add_photo_button);
        addPhotoButton.setOnClickListener(v -> showImagePickerDialog());
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
        publicationsAdapter = new PublicationsAdapter(publicationsList, currentUserId);
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
            Publication publication = new Publication(currentUserId, content, System.currentTimeMillis());
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
                            Publication publication = new Publication(authorId, content, timestamp);
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

    // Image picker dialog
    private void showImagePickerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Choose Image Source")
                .setItems(new CharSequence[]{"Gallery", "Camera"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, REQUEST_IMAGE_CAPTURE);
                    } else {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                            File photoFile = createImageFile();
                            if (photoFile != null) {
                                imageUri = Uri.fromFile(photoFile);
                                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                            }
                        }
                    }
                })
                .show();
    }

    // Create temp image file
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Handle image selection result
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                imageUri = data.getData();
            }
            if (imageUri != null) {
                uploadImageToImageKit();
            }
        }
    }

    // Upload to ImageKit
    private void uploadImageToImageKit() {
        try {
            String filePath = getRealPathFromURI(imageUri);
            File file = new File(filePath);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            ImageKitApiService service = new Retrofit.Builder()
                    .baseUrl("https://upload.imagekit.io/api/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ImageKitApiService.class);

            service.uploadImage(
                    RequestBody.create(MediaType.parse("text/plain"), "your_public_key"),
                    filePart,
                    RequestBody.create(MediaType.parse("text/plain"), file.getName()),
                    RequestBody.create(MediaType.parse("text/plain"), "true"),
                    RequestBody.create(MediaType.parse("text/plain"), "/uploads/")
            ).enqueue(new Callback<ImageKitResponse>() {
                @Override
                public void onResponse(Call<ImageKitResponse> call, Response<ImageKitResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        saveImageUrlToFirebase(response.body().url);
                        Toast.makeText(requireContext(), "Upload successful", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ImageKitResponse> call, Throwable t) {
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Helper to get file path from URI
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = requireActivity().getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    // Save URL to Firebase
    private void saveImageUrlToFirebase(String imageUrl) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("images")
                .child(cardId)
                .push();

        ref.setValue(new ImageData(currentUserId, imageUrl, System.currentTimeMillis()));
    }
}