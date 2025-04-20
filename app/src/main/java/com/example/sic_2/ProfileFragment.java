package com.example.sic_2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView profileImage;
    private TextView tvUsername, tvEmail, tvAbout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        tvAbout = view.findViewById(R.id.tv_about);

        // Set click listeners
        view.findViewById(R.id.layout_change_photo).setOnClickListener(v -> changeProfilePhoto());
        view.findViewById(R.id.layout_edit_profile).setOnClickListener(v -> editProfile());
        view.findViewById(R.id.layout_change_password).setOnClickListener(v -> changePassword());
        view.findViewById(R.id.layout_settings).setOnClickListener(v -> openSettings());
        view.findViewById(R.id.layout_logout).setOnClickListener(v -> logout());

        // Load user data (you would replace this with your actual data loading logic)
        loadUserData();

        return view;
    }

    private void loadUserData() {
        // TODO: Replace with your actual user data loading logic
        // This is just a placeholder
        tvUsername.setText("John Doe");
        tvEmail.setText("john.doe@example.com");
        tvAbout.setText("Android developer passionate about creating great user experiences.");
    }

    private void changeProfilePhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void editProfile() {
        // TODO: Implement edit profile functionality
        Toast.makeText(getContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show();
        // You would typically start a new activity or show a dialog to edit profile
    }

    private void changePassword() {
        // TODO: Implement change password functionality
        Toast.makeText(getContext(), "Change Password clicked", Toast.LENGTH_SHORT).show();
        // You would typically show a dialog with current password, new password fields
    }

    private void openSettings() {
        // Load the SettingsFragment
        FragmentManager fragmentManager = getParentFragmentManager(); // Use getParentFragmentManager() for fragments
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.container, new SettingsFragment()); // Replace with your SettingsFragment
        transaction.addToBackStack(null); // Optional: Add to back stack to allow back navigation
        transaction.commit();
    }

    private void logout() {
        // TODO: Implement logout functionality
        Toast.makeText(getContext(), "Logout clicked", Toast.LENGTH_SHORT).show();
        // You would typically clear user session and navigate to login screen
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            // TODO: Upload the image to your server if needed
        }
    }
}