package com.example.sic_2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
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
                View root = getActivity().findViewById(android.R.id.content);
                runDarkModeRevealAnimation(root, isDarkMode);
                return false; // We'll set the value manually after the animation.
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

    private void runDarkModeRevealAnimation(View root, boolean enableDark) {
        if (root == null) return;
        int cx = root.getWidth() / 2;
        int cy = root.getHeight() / 2;
        float finalRadius = (float) Math.hypot(cx, cy);

        int color = enableDark
                ? ContextCompat.getColor(getContext(), android.R.color.black)
                : ContextCompat.getColor(getContext(), android.R.color.white);

        View overlay = new View(getContext());
        overlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(color);
        ((ViewGroup) root).addView(overlay);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animator anim = ViewAnimationUtils.createCircularReveal(overlay, cx, cy, 0f, finalRadius);
            anim.setDuration(420);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (enableDark) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    // Remove overlay after a short delay for theme transition
                    overlay.postDelayed(() -> ((ViewGroup) root).removeView(overlay), 250);
                }
            });
            anim.start();
        } else {
            // Fallback for old devices
            if (enableDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            overlay.postDelayed(() -> ((ViewGroup) root).removeView(overlay), 300);
        }

        // Finally update the preference manually (to avoid double animation)
        SwitchPreferenceCompat darkModeSwitch = findPreference("dark_mode");
        if (darkModeSwitch != null) darkModeSwitch.setChecked(enableDark);
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