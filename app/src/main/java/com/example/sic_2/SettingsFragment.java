package com.example.sic_2;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Dark mode switch
        SwitchPreferenceCompat darkModeSwitch = findPreference("dark_mode");
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isDarkMode = (boolean) newValue;
                if (isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                return true;
            });
        }

        // User ID preference
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Preference userIdPref = findPreference("user_id");
            String userId = user.getUid();

            if (userIdPref != null) {
                userIdPref.setSummary(userId);
                userIdPref.setOnPreferenceClickListener(preference -> {
                    copyToClipboard(userId);
                    Toast.makeText(getContext(), "User ID copied!", Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }

        // Logout preference
        Preference logoutPref = findPreference("logout");
        if (logoutPref != null) {
            logoutPref.setOnPreferenceClickListener(preference -> {
                logout();
                return true;
            });
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("User ID", text);
        clipboard.setPrimaryClip(clip);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }
}