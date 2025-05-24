package com.example.sic_2;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.database.*;
import java.util.*;

public class CardStoryViewerDialog extends DialogFragment {
    private static final int PICK_STORY_IMAGE = 2001;
    private static final long STORY_DURATION = 5000; // 5 seconds

    private String cardId, userId;
    private List<Story> stories = new ArrayList<>();
    private int currentIndex = 0;
    private ImageView storyImageView;
    private ProgressBar storyProgress;
    private Button addStoryButton;
    private TextView noStoriesText;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoAdvanceRunnable;

    public static CardStoryViewerDialog newInstance(String cardId, String userId) {
        CardStoryViewerDialog f = new CardStoryViewerDialog();
        Bundle args = new Bundle();
        args.putString("cardId", cardId);
        args.putString("userId", userId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_fullscreen_story_viewer, container, false);
        storyImageView = v.findViewById(R.id.storyImageView);
        storyProgress = v.findViewById(R.id.storyProgress);
        addStoryButton = v.findViewById(R.id.addStoryButton);
        noStoriesText = v.findViewById(R.id.noStoriesText);

        cardId = getArguments().getString("cardId");
        userId = getArguments().getString("userId");

        // Touch zones for tap navigation
        View prevZone = v.findViewById(R.id.prevZone);
        View nextZone = v.findViewById(R.id.nextZone);

        prevZone.setOnClickListener(view -> showStory(currentIndex - 1, true));
        nextZone.setOnClickListener(view -> showStory(currentIndex + 1, true));

        // Add story button click
        addStoryButton.setOnClickListener(view -> openImagePicker());

        // Allow long press on image for adding stories even if stories exist
        storyImageView.setOnLongClickListener(view -> {
            openImagePicker();
            return true;
        });

        // Initialize Cloudinary (only if not already done in Application)
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "disiijbpp");
            config.put("api_key", "265226997838638");
            config.put("api_secret", "RsPtut3zPunRm-8Hwh8zRqQ8uG8");
            MediaManager.init(requireContext().getApplicationContext(), config);
        } catch (IllegalStateException e) {
            // Already initialized
        }

        loadStories();

        return v;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Stories"), PICK_STORY_IMAGE);
    }

    private void loadStories() {
        long twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        DatabaseReference storiesRef = FirebaseDatabase.getInstance().getReference("stories").child(cardId);
        storiesRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                stories.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Story story = data.getValue(Story.class);
                    if (story != null) {
                        if (story.getTimestamp() < twentyFourHoursAgo) {
                            data.getRef().removeValue();
                        } else {
                            stories.add(story);
                        }
                    }
                }
                Log.d("CardStoryViewer", "Stories loaded: " + stories.size());
                showStory(0, false);
                updateEmptyState();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CardStoryViewer", "loadStories cancelled", error.toException());
            }
        });
    }

    // Show current story, or the "add new story" suggestion page after the last story
    private void showStory(int index, boolean manual) {
        // Clamp index to [0, stories.size()]
        if (index < 0) index = 0;
        if (index > stories.size()) index = stories.size();

        // If there are no stories, or user is at the "add story" suggestion page
        if (stories.isEmpty() || index == stories.size()) {
            showAddStorySuggestion();
            currentIndex = stories.size(); // so navigation works
            handler.removeCallbacksAndMessages(null);
            updateEmptyState();
            return;
        }

        currentIndex = index;
        String imgUrl = stories.get(currentIndex).getImageUrl();
        Log.d("CardStoryViewer", "Showing story at " + currentIndex + " with image: " + imgUrl);
        if (imgUrl == null || imgUrl.isEmpty()) {
            storyImageView.setImageResource(R.drawable.uploadimg);
        } else {
            Glide.with(requireContext())
                    .load(imgUrl)
                    .placeholder(R.drawable.uploadimg)
                    .error(R.drawable.uploadimg)
                    .into(storyImageView);
        }

        storyImageView.setVisibility(View.VISIBLE);
        storyProgress.setVisibility(View.VISIBLE);
        addStoryButton.setVisibility(View.GONE);
        noStoriesText.setVisibility(View.GONE);

        storyProgress.setProgress(0);
        handler.removeCallbacksAndMessages(null);

        if (!manual) {
            animateProgressBar();
            autoAdvanceRunnable = () -> showStory(currentIndex + 1, false);
            handler.postDelayed(autoAdvanceRunnable, STORY_DURATION);
        }
        updateEmptyState();
    }

    // Show a special "Add Story" suggestion page
    private void showAddStorySuggestion() {
        storyImageView.setVisibility(View.GONE);
        storyProgress.setVisibility(View.GONE);
        addStoryButton.setVisibility(View.VISIBLE);
        noStoriesText.setVisibility(View.VISIBLE);
        noStoriesText.setText("Add another story?");
    }

    private void animateProgressBar() {
        storyProgress.setProgress(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(storyProgress, "progress", 0, 1000);
        anim.setDuration(STORY_DURATION);
        anim.setInterpolator(new android.view.animation.LinearInterpolator());
        anim.start();
    }

    // Only show addStoryButton and noStoriesText when on the suggestion page
    private void updateEmptyState() {
        boolean onSuggestionPage = (stories.isEmpty() || currentIndex == stories.size());
        if (addStoryButton != null) addStoryButton.setVisibility(onSuggestionPage ? View.VISIBLE : View.GONE);
        if (noStoriesText != null) noStoriesText.setVisibility(onSuggestionPage ? View.VISIBLE : View.GONE);
        if (!onSuggestionPage) {
            if (storyImageView != null) storyImageView.setVisibility(View.VISIBLE);
            if (storyProgress != null) storyProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_STORY_IMAGE && resultCode == getActivity().RESULT_OK && data != null) {
            // Handle multiple images
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    uploadStoryImage(imageUri, i == count - 1); // last one triggers refresh
                }
            } else if (data.getData() != null) {
                // Single image selected
                uploadStoryImage(data.getData(), true);
            }
        }
    }

    private void uploadStoryImage(Uri uri, boolean refreshAfter) {
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String uploadedUrl = (String) resultData.get("secure_url");
                        String newStoryId = FirebaseDatabase.getInstance().getReference("stories").child(cardId).push().getKey();
                        Story story = new Story(newStoryId, cardId, uploadedUrl, System.currentTimeMillis(), userId);
                        FirebaseDatabase.getInstance().getReference("stories").child(cardId).child(newStoryId).setValue(story)
                                .addOnSuccessListener(aVoid -> {
                                    if (refreshAfter) loadStories();
                                });
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        Log.e("CardStoryViewer", "Cloudinary upload error: " + error.getDescription());
                        if (refreshAfter) loadStories();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }
}