package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CardActivity extends AppCompatActivity {

    private String cardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);

        // Retrieve card ID from intent
        cardId = getIntent().getStringExtra("cardId");
        String originalOwnerId = getIntent().getStringExtra("originalOwnerId");

        String cardsPath = (originalOwnerId != null) ? originalOwnerId : FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cardRef = FirebaseDatabase.getInstance()
                .getReference("cards")
                .child(cardsPath)
                .child(cardId);

        // Log the received cardId
        Log.d("CardActivity", "Received Card ID: " + cardId);

        if (cardId == null || cardId.isEmpty()) {
            Toast.makeText(this, "Card ID is missing", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if cardId is missing
            return;
        }

        // Initialize back button
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish()); // Close the activity and go back

        // Load the default fragment (CardFragment)
        loadFragment(CardFragment.newInstance(cardId), false);

        // Bottom navigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_main) {
                    selectedFragment = CardFragment.newInstance(cardId); // CardFragment
                } else if (itemId == R.id.nav_chat) {
                    selectedFragment = ChatFragment.newInstance(cardId); // ChatFragment
                } else if (itemId == R.id.nav_parameters) {
                    selectedFragment = ParametersFragment.newInstance("param1", "param2", cardId); // ParametersFragment
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment, true);
                }
                return true;
            });

            // Set the default selected item (e.g., main fragment)
            bottomNavigationView.setSelectedItemId(R.id.nav_main);
        } else {
            Log.e("CardActivity", "BottomNavigationView is null. Check your layout file.");
        }
    }

    /**
     * Loads a fragment into the fragment container.
     *
     * @param fragment       The fragment to load.
     * @param addToBackStack Whether to add the transaction to the back stack.
     */
    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void onCardDeleted(String cardId) {
        // Option 1: If you want to refresh the entire HomeFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        // OR Option 2: If you want to notify the existing fragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null && fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).onCardDeleted(cardId);
        }

        // Close the activity after deletion
        finish();
    }


    public void reloadHomeFragmentData() {
        try {
            // First try to find by container ID
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            // If not found by ID, try finding by tag (more reliable when fragments are added with tags)
            if (fragment == null) {
                fragment = getSupportFragmentManager().findFragmentByTag("HomeFragment");
            }

            // If we found the fragment and it's the right type
            if (fragment instanceof HomeFragment) {
                // Post the reload to ensure it runs on UI thread
                Fragment finalFragment = fragment;
                fragment.getView().post(() -> {
                    try {
                        ((HomeFragment) finalFragment).reloadData();
                    } catch (Exception e) {
                        Log.e("CardActivity", "Error reloading HomeFragment data", e);
                    }
                });
            } else {
                Log.d("CardActivity", "HomeFragment not found in fragment manager");
            }
        } catch (IllegalStateException e) {
            Log.e("CardActivity", "Fragment manager not ready", e);
        }
    }

}