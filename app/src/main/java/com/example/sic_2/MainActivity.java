package com.example.sic_2; // Replace with your package name

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.ImageView;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnOpenDrawer;
    private Map<Integer, Fragment> fragmentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer);

        setupFragmentMap();
        setupDrawerNavigation(navigationView);
        setupBottomNavigation();

        // Open Drawer Button Listener
        btnOpenDrawer.setOnClickListener(view -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void setupFragmentMap() {
        fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_notification, new NotificationFragment());
        fragmentMap.put(R.id.nav_settings, new SettingsFragment());
    }

    private void setupDrawerNavigation(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            } else if (item.getItemId() == R.id.nav_logout) {
                handleLogout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            loadFragment(selectedFragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }

    private void handleLogout() {
        // Perform logout logic (e.g., clear user session, etc.)
        // Redirect to Login Activity
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish(); // Close MainActivity
    }
}
