package com.example.sic_2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private ArrayList<String> messages = new ArrayList<>();
    private TextView messageTextView; // TextView to display messages
    private EditText editTextMessage; // EditText for inputting messages
    private Button buttonSend; // Button to send messages
    private Button buttonAddPhoto; // Button to add photos
    private ImageView imageViewPhoto1; // ImageView for the first image
    private ImageView imageViewPhoto2; // ImageView for the second image

    private ActivityResultLauncher<Intent> getContentLauncher;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        messageTextView = view.findViewById(R.id.messageTextView);
        editTextMessage = view.findViewById(R.id.editTextMessage);
        buttonSend = view.findViewById(R.id.buttonSend);
        buttonAddPhoto = view.findViewById(R.id.buttonAddPhoto);
        imageViewPhoto1 = view.findViewById(R.id.imageViewPhoto1);
        imageViewPhoto2 = view.findViewById(R.id.imageViewPhoto2);

        // Set up button listeners
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString();
            if (!message.isEmpty()) {
                addMessage(message);
                editTextMessage.setText(""); // Clear the input field
            }
        });

        buttonAddPhoto.setOnClickListener(v -> openGallery());

        // Initialize the ActivityResultLauncher
        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            if (imageViewPhoto1.getVisibility() == View.GONE) {
                                imageViewPhoto1.setImageURI(imageUri);
                                imageViewPhoto1.setVisibility(View.VISIBLE);
                            } else if (imageViewPhoto2.getVisibility() == View.GONE) {
                                imageViewPhoto2.setImageURI(imageUri);
                                imageViewPhoto2.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });

        return view;
    }

    public void addMessage(String message) {
        messages.add(message);
        updateMessageDisplay();
    }

    private void updateMessageDisplay() {
        StringBuilder displayText = new StringBuilder();
        for (String msg : messages) {
            displayText.append(msg).append("\n");
        }
        messageTextView.setText(displayText.toString());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getContentLauncher.launch(intent);
    }



}
