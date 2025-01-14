package com.example.sic_2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private Button btnLogOut;
    private FirebaseAuth mAuth;

    private final HomeFragment homeFragment = new HomeFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private final NotificationFragment notificationFragment = new NotificationFragment();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnLogOut = findViewById(R.id.btnLogout);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set default fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFragment).commit();

        // Setup Bottom Navigation View
        setupBottomNavigationView();

        // Logout Button Listener
        btnLogOut.setOnClickListener(view -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupBottomNavigationView() {
        // Add badge to notification tab
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.notification);
        badgeDrawable.setVisible(true);
        badgeDrawable.setNumber(8);

        // Map menu item IDs to fragments
        SparseArray<Fragment> fragmentMap = new SparseArray<>();
        fragmentMap.put(R.id.home, homeFragment);
        fragmentMap.put(R.id.notification, notificationFragment);
        fragmentMap.put(R.id.settings, settingsFragment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, selectedFragment).commit();
                return true;
            }
            return false;
        });
    }
}
