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

public class CardActivity extends AppCompatActivity {

    private String cardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);

        // Retrieve card ID from intent
        cardId = getIntent().getStringExtra("cardId");

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
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).onCardDeleted(cardId); // Notify HomeFragment to remove the card
        }
    }


    public void reloadHomeFragmentData() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).reloadData(); // Reload data in HomeFragment
        }
    }

}