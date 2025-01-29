package com.example.sic_2;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FloatingActionButton fab;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout cardContainer; // Container for CardViews

    // Firebase
    private DatabaseReference database;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupDrawerNavigation();
        setupBottomNavigationView();
        setupFloatingActionButton();

        // Firebase setup
        database = FirebaseDatabase.getInstance().getReference("cards");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get user ID

        // Load saved cards
        loadCards();

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
        fab = findViewById(R.id.fab);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        cardContainer = findViewById(R.id.card_container); // Initialize the CardView container
    }

    private void setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = getFragmentById(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            } else if (item.getItemId() == R.id.nav_logout) {
                handleLogout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private Fragment getFragmentById(int id) {
        Map<Integer, Fragment> fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.home, new HomeFragment());
        fragmentMap.put(R.id.notification, new NotificationFragment());
        fragmentMap.put(R.id.settings, new SettingsFragment());
        return fragmentMap.get(id);
    }

    private void setupBottomNavigationView() {
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.notification);
        badgeDrawable.setVisible(true);
        badgeDrawable.setNumber(8);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = getFragmentById(item.getItemId());
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
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                saveCardToFirebase(message);
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveCardToFirebase(String message) {
        String cardId = database.child(userId).push().getKey(); // Generate unique card ID
        if (cardId != null) {
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("cardId", cardId);
            cardData.put("message", message);

            database.child(userId).child(cardId).setValue(cardData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    createCardView(cardId, message);
                } else {
                    Toast.makeText(this, "Failed to save card", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadCards() {
        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    String cardId = data.child("cardId").getValue(String.class);
                    String message = data.child("message").getValue(String.class);

                    if (cardId != null && message != null) {
                        createCardView(cardId, message);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Error loading cards", error.toException());
            }
        });
    }

    private void createCardView(String cardId, String message) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_view_layout, cardContainer, false);

        TextView cardMessage = cardView.findViewById(R.id.card_message);
        ImageButton backButton = cardView.findViewById(R.id.backbutton);
        Button deleteButton = new Button(this);

        cardMessage.setText(message);
        cardContainer.addView(cardView);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Card.class);
            intent.putExtra("cardId", cardId); // Pass cardId to CardActivity
            startActivity(intent);
        });

        deleteButton.setText("Delete");
        deleteButton.setOnClickListener(v -> deleteCard(cardId, cardView));
        ((LinearLayout) cardView).addView(deleteButton);

        backButton.setOnClickListener(v -> getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new HomeFragment())
                .commit());
    }

    private void deleteCard(String cardId, View cardView) {
        database.child(userId).child(cardId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                cardContainer.removeView(cardView);
            } else {
                Toast.makeText(this, "Failed to delete card", Toast.LENGTH_SHORT).show();
            }
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
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}
