package com.example.sic_2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Switch;
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

                // Animate from the switch's thumb (the actual switch widget)
                getListView().post(() -> {
                    View switchWidget = findSwitchWidgetForPreference("dark_mode");
                    View root = requireActivity().findViewById(android.R.id.content);

                    if (switchWidget != null && root != null) {
                        // Get coordinates of the switch widget relative to root
                        int[] switchLocation = new int[2];
                        int[] rootLocation = new int[2];
                        switchWidget.getLocationOnScreen(switchLocation);
                        root.getLocationOnScreen(rootLocation);

                        int cx = switchLocation[0] - rootLocation[0] + switchWidget.getWidth() / 2;
                        int cy = switchLocation[1] - rootLocation[1] + switchWidget.getHeight() / 2;

                        runDarkModeRevealAnimation(root, isDarkMode, cx, cy);
                    } else {
                        // fallback: animate from screen center
                        runDarkModeRevealAnimation(root, isDarkMode, root.getWidth() / 2, root.getHeight() / 2);
                    }
                });
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
    }

    /**
     * Finds the Switch widget for a SwitchPreferenceCompat by key.
     */
    private View findSwitchWidgetForPreference(String key) {
        if (getListView() == null || key == null) return null;
        for (int i = 0; i < getListView().getChildCount(); i++) {
            View row = getListView().getChildAt(i);
            if (row == null) continue;
            Object tag = row.getTag();
            if (tag instanceof String && tag.equals(key)) {
                View switchWidget = row.findViewById(android.R.id.switch_widget);
                if (switchWidget instanceof Switch) {
                    return switchWidget;
                }
            }
            // fallback: try to find any switch
            View switchWidget = row.findViewById(android.R.id.switch_widget);
            if (switchWidget instanceof Switch) {
                return switchWidget;
            }
        }
        // Fallback: try to find any switch in the list
        if (getListView().findViewById(android.R.id.switch_widget) != null)
            return getListView().findViewById(android.R.id.switch_widget);
        return null;
    }

    private void runDarkModeRevealAnimation(View root, boolean enableDark, int cx, int cy) {
        if (root == null) return;
        float finalRadius = (float) Math.hypot(root.getWidth(), root.getHeight());

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

        // Update the switch state after animation
        SwitchPreferenceCompat darkModeSwitch = findPreference("dark_mode");
        if (darkModeSwitch != null) darkModeSwitch.setChecked(enableDark);
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("User ID", text);
        clipboard.setPrimaryClip(clip);
    }
}