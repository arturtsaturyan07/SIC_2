package com.example.sic_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.sic_2.LoginActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    String email_ = LoginActivity.email_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkModeEnabled = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        FirebaseApp.initializeApp(this);


        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");

// Create a new user
        String userId = usersRef.push().getKey(); // Generate a unique ID
        User newUser = new User(userId, "John Doe", email_);

// Add the user to the database
        usersRef.child(userId).setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebase", "User added successfully");
                    } else {
                        Log.e("Firebase", "User addition failed", task.getException());
                    }
                });


        initializeViews();
        setupDrawerNavigation();
        setupBottomNavigationView();

        ImageView btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        btnOpenDrawer.setOnClickListener(view -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Load the default fragment
        loadFragment(new HomeFragment());
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                // Handle logout
                logout();
            } else {
                // Handle other navigation items
                Fragment selectedFragment = getFragmentById(item.getItemId());
                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private Fragment getFragmentById(int id) {
        if (id == R.id.home) return new HomeFragment();
        if (id == R.id.notification) return new NotificationFragment();
        if (id == R.id.settings) return new SettingsFragment();
        if (id == R.id.add_button) {
            Intent intent = new Intent(MainActivity.this, UserSearchActivity.class);
            startActivity(intent);
            return null; // Return null since we're starting a new activity
        }
        return null;
    }


    private void setupBottomNavigationView() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = getFragmentById(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();  // Sign out from Firebase
        Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Redirect to login screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();  // Finish the current activity to prevent going back to it
    }


}
