package com.example.sic_2;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import java.util.*;

public class CardStoryViewerDialog extends DialogFragment {
    private static final int PICK_STORY_IMAGE = 2001;
    private String cardId, userId;
    private List<Story> stories = new ArrayList<>();
    private int currentIndex = 0;
    private ImageView imageView;
    private Button addButton, nextButton, prevButton;
    private ProgressBar storyLoading;

    public static CardStoryViewerDialog newInstance(String cardId, String userId) {
        CardStoryViewerDialog f = new CardStoryViewerDialog();
        Bundle args = new Bundle();
        args.putString("cardId", cardId);
        args.putString("userId", userId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_story_card_viewer, container, false); // Use your actual layout name here!
        imageView = v.findViewById(R.id.storyImageView);
        addButton = v.findViewById(R.id.addStoryButton);
        nextButton = v.findViewById(R.id.nextStoryButton);
        prevButton = v.findViewById(R.id.prevStoryButton);
        storyLoading = v.findViewById(R.id.storyLoading);

        cardId = getArguments().getString("cardId");
        userId = getArguments().getString("userId");

        loadStories();

        addButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_STORY_IMAGE);
        });

        nextButton.setOnClickListener(vw -> showStory(currentIndex + 1));
        prevButton.setOnClickListener(vw -> showStory(currentIndex - 1));

        return v;
    }

    private void loadStories() {
        long now = System.currentTimeMillis();
        long twentyFourHoursAgo = now - 24 * 60 * 60 * 1000;
        DatabaseReference storiesRef = FirebaseDatabase.getInstance().getReference("stories").child(cardId);
        storiesRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                stories.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Story story = data.getValue(Story.class);
                    if (story != null) {
                        if (story.getTimestamp() < twentyFourHoursAgo) {
                            // Optionally: delete expired stories
                            data.getRef().removeValue();
                        } else {
                            stories.add(story);
                        }
                    }
                }
                showStory(0);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showStory(int index) {
        if (stories.isEmpty()) {
            imageView.setImageResource(R.drawable.uploadimg);
            currentIndex = 0;
            nextButton.setEnabled(false);
            prevButton.setEnabled(false);
            return;
        }
        currentIndex = Math.max(0, Math.min(index, stories.size() - 1));
        Glide.with(requireContext()).load(stories.get(currentIndex).getImageUrl()).into(imageView);
        nextButton.setEnabled(currentIndex < stories.size() - 1);
        prevButton.setEnabled(currentIndex > 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_STORY_IMAGE && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadStoryImage(imageUri);
        }
    }

    private void uploadStoryImage(Uri uri) {
        // TODO: Replace with your Cloudinary upload logic. On success, use the Cloudinary URL.
        String uploadedUrl = uri.toString(); // Replace with actual Cloudinary URL after upload
        String newStoryId = FirebaseDatabase.getInstance().getReference("stories").child(cardId).push().getKey();
        Story story = new Story(newStoryId, cardId, uploadedUrl, System.currentTimeMillis(), userId);
        FirebaseDatabase.getInstance().getReference("stories").child(cardId).child(newStoryId).setValue(story)
                .addOnSuccessListener(aVoid -> loadStories());
    }
}