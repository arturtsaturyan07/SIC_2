package com.example.sic_2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FloatingActionButton fab;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Map<Integer, Fragment> fragmentMap;
    private LinearLayout cardContainer; // Container for CardViews

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupDrawerNavigation();
        setupBottomNavigationView();
        setupFloatingActionButton();

//        TextView cardMessage = findViewById(R.id.card_message);
//        cardMessage.setText("Your dynamic message here!");


        ImageView btnOpenDrawer = findViewById(R.id.btnOpenDrawer);

        // Open Drawer Button Listener
        btnOpenDrawer.setOnClickListener(view -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Load the default fragment
        loadFragment(new HomeFragment());
    }

    private void initializeViews() {
        fab = findViewById(R.id.fab);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        cardContainer = findViewById(R.id.card_container); // Initialize the CardView container
    }

    private void setupDrawerNavigation() {
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

    private void setupBottomNavigationView() {
        fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.home, new HomeFragment());
        fragmentMap.put(R.id.notification, new NotificationFragment());
        fragmentMap.put(R.id.settings, new SettingsFragment());

        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.notification);
        badgeDrawable.setVisible(true);
        badgeDrawable.setNumber(8);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void setupFloatingActionButton() {
        fab.setOnClickListener(view -> {
            Log.d("FAB Click", "FAB was clicked");
            showInputDialog();
        });
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create a Card");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String message = input.getText().toString();
            if (!message.isEmpty()) {
                createCardView(message);
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createCardView(String message) {
        // Inflate the LinearLayout layout
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_view_layout, cardContainer, false);

        // Set the message to a TextView in LinearLayout (assume there's a TextView with id 'card_message')
        TextView cardMessage = cardView.findViewById(R.id.card_message);
        cardMessage.setText(message);

        // Add the LinearLayout to the container
        cardContainer.addView(cardView);

        // Set an OnClickListener for the LinearLayout
        cardView.setOnClickListener(v -> {
            Log.d("LinearLayout Click", "LinearLayout was clicked");
            startActivity(new Intent(MainActivity.this, BlankActivity.class));
            finish();
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
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}
