package com.example.sic_2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.cloudinary.Transformation;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView profileImage;
    private TextView tvUsername, tvEmail, tvAbout;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean cloudinaryInitialized = false;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initCloudinary();
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "your_cloud_name");
            config.put("api_key", "your_api_key");
            config.put("api_secret", "your_api_secret");
            MediaManager.init(requireContext(), config);
            cloudinaryInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            cloudinaryInitialized = false;
            Toast.makeText(getContext(), "Cloudinary initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        view.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_in));

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        tvAbout = view.findViewById(R.id.tv_about);

        // Load user data
        loadUserData();

        // Set click listeners
        setClickListenersWithAnimation(view);

        return view;
    }

    private void setClickListenersWithAnimation(View view) {
        int[] clickableIds = {
                R.id.layout_change_photo,
                R.id.layout_edit_profile,
                R.id.layout_change_password,
                R.id.layout_settings,
                R.id.layout_logout
        };

        for (int id : clickableIds) {
            view.findViewById(id).setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.ripple));
                handleClick(id);
            });
        }
    }

    private void handleClick(int viewId) {
        if (viewId == R.id.layout_change_photo) {
            changeProfilePhoto();
        } else if (viewId == R.id.layout_edit_profile) {
            showEditProfileDialog();
        } else if (viewId == R.id.layout_change_password) {
            showChangePasswordDialog();
        } else if (viewId == R.id.layout_settings) {
            openSettingsWithTransition();
        } else if (viewId == R.id.layout_logout) {
            logout();
        }
    }

    private void loadUserData() {
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            tvUsername.setText(displayName != null && !displayName.isEmpty() ? displayName : "User");
            tvEmail.setText(currentUser.getEmail());
            tvAbout.setText("Tell us something about yourself...");

            if (currentUser.getPhotoUrl() != null) {
                // Consider using Glide/Picasso here for better image loading
                profileImage.setImageURI(currentUser.getPhotoUrl());
            }
        }
    }

    private void changeProfilePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void showEditProfileDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Initialize dialog views
        CircleImageView dialogProfileImage = dialogView.findViewById(R.id.dialog_profile_image);
        TextInputEditText etFirstName = dialogView.findViewById(R.id.et_first_name);
        TextInputEditText etLastName = dialogView.findViewById(R.id.et_last_name);
        TextInputEditText etEmail = dialogView.findViewById(R.id.et_email);
        TextInputEditText etAbout = dialogView.findViewById(R.id.et_about);
        Button btnChangePhoto = dialogView.findViewById(R.id.btn_change_photo);
        Button btnSave = dialogView.findViewById(R.id.btn_save_profile);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_profile);

        // Set current values
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null) {
                String[] names = displayName.split(" ");
                etFirstName.setText(names.length > 0 ? names[0] : "");
                etLastName.setText(names.length > 1 ? names[1] : "");
            }
            etEmail.setText(currentUser.getEmail());
            etAbout.setText(tvAbout.getText().toString());

            if (currentUser.getPhotoUrl() != null) {
                dialogProfileImage.setImageURI(currentUser.getPhotoUrl());
            }
        }

        btnChangePhoto.setOnClickListener(v -> {
            changeProfilePhoto();
            dialog.dismiss();
        });

        btnSave.setOnClickListener(v -> validateAndUpdateProfile(
                etFirstName, etLastName, etEmail, etAbout, dialog));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void validateAndUpdateProfile(TextInputEditText etFirstName, TextInputEditText etLastName,
                                          TextInputEditText etEmail, TextInputEditText etAbout, AlertDialog dialog) {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String about = etAbout.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }

        updateProfile(firstName + " " + lastName, email, about);
        dialog.dismiss();
    }

    private void updateProfile(String fullName, String email, String about) {
        if (currentUser == null) return;

        // Update display name
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tvUsername.setText(fullName);
                        tvAbout.setText(about);
                        showToast("Profile updated");
                    } else {
                        showToast("Failed to update profile");
                    }
                });

        // Update email if changed
        if (!email.equals(currentUser.getEmail())) {
            currentUser.updateEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            tvEmail.setText(email);
                            showToast("Email updated");
                        } else {
                            showToast("Failed to update email: " + task.getException().getMessage());
                        }
                    });
        }
    }

    private void showChangePasswordDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
        Button btnChangePassword = dialogView.findViewById(R.id.btn_change_password);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnChangePassword.setOnClickListener(v -> validateAndChangePassword(
                etCurrentPassword, etNewPassword, etConfirmPassword, dialog));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void validateAndChangePassword(TextInputEditText etCurrentPassword,
                                           TextInputEditText etNewPassword,
                                           TextInputEditText etConfirmPassword,
                                           AlertDialog dialog) {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Current password is required");
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("New password is required");
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            return;
        }

        changePassword(currentPassword, newPassword);
        dialog.dismiss();
    }

    private void changePassword(String currentPassword, String newPassword) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(passwordTask -> {
                                    if (passwordTask.isSuccessful()) {
                                        showToast("Password changed successfully");
                                    } else {
                                        showToast("Failed to change password");
                                    }
                                });
                    } else {
                        showToast("Authentication failed. Wrong current password.");
                    }
                });
    }

    private void openSettingsWithTransition() {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
        transaction.replace(R.id.container, new SettingsFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void logout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    showToast("Logged out successfully");
                    startActivity(new Intent(getActivity(), LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (imageUri == null || currentUser == null) {
            showToast("No image selected");
            return;
        }

        if (!cloudinaryInitialized) {
            showToast("Image upload service not available");
            return;
        }

        String publicId = "profile_" + currentUser.getUid() + "_" + System.currentTimeMillis();

        MediaManager.get()
                .upload(imageUri)
                .unsigned("your_upload_preset") // Replace with your upload preset
                .option("public_id", publicId)
                .option("folder", "user_profiles")
                .option("transformation", new Transformation()
                        .width(200)
                        .height(200)
                        .crop("fill")
                        .gravity("face"))
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        showToast("Uploading image...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Upload progress
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        if (secureUrl != null) {
                            updateProfilePicture(Uri.parse(secureUrl));
                        } else {
                            showToast("Upload failed: no URL returned");
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        showToast("Upload failed: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        showToast("Upload rescheduled");
                    }
                })
                .dispatch();
    }

    private void updateProfilePicture(Uri uri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        profileImage.setImageURI(uri);
                        showToast("Profile picture updated");
                    } else {
                        showToast("Failed to update profile");
                    }
                });
    }
}