package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import java.util.Objects;

public class CardDetailedFragment extends Fragment {

    private DatabaseReference database;
    private TextView cardMessage;
    private MaterialCalendarView calendarView;
    private String cardId;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.card_view_layout, container, false);

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("cards");
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Get card ID from arguments
        if (getArguments() != null) {
            cardId = getArguments().getString("cardId");
        }

        // Initialize views
        cardMessage = view.findViewById(R.id.card_message);
        ImageButton backButton = view.findViewById(R.id.backbutton);
        Button addUserButton = view.findViewById(R.id.addUserButton);
        Button createEventButton = view.findViewById(R.id.create_event_button);
        calendarView = view.findViewById(R.id.calendar_view);

        // Check user access
        if (cardId != null) {
            checkUserAccess();
        }

        // Back button: Navigate back
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Add User button: Open UserAddActivity
        addUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UserAddActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });

        // Create Event button: Show dialog to create an event
        createEventButton.setOnClickListener(v -> showCreateEventDialog());

        // Bottom Navigation Menu
        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_main) {
                selectedFragment = new MainFragment();
            } else if (item.getItemId() == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (item.getItemId() == R.id.nav_team_leader) {
                selectedFragment = new TeamLeaderFragment();
            }

            if (selectedFragment != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Load the default fragment (Main)
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_main);
        }

        return view;
    }

    /**
     * Check if the current user has access to this card.
     */
    private void checkUserAccess() {
        database.child(cardId).child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    loadCardData();
                } else {
                    Toast.makeText(requireContext(), "Access denied", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed(); // Go back if access is denied
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to check access", error.toException());
            }
        });
    }

    /**
     * Load card data from Firebase.
     */
    private void loadCardData() {
        database.child(cardId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String message = snapshot.child("message").getValue(String.class);
                    if (message != null) {
                        cardMessage.setText(message);
                    }
                } else {
                    Log.e("Firebase", "Card not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load card", error.toException());
            }
        });
    }

    /**
     * Show a dialog to create an event.
     */
    private void showCreateEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create Event");

        final androidx.appcompat.widget.AppCompatEditText eventNameInput = new androidx.appcompat.widget.AppCompatEditText(requireContext());
        eventNameInput.setHint("Event Name");
        builder.setView(eventNameInput);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String eventName = eventNameInput.getText().toString();
            if (!eventName.isEmpty()) {
                saveEvent(eventName);
            } else {
                Toast.makeText(requireContext(), "Event name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Save the event and mark it on the calendar.
     */
    private void saveEvent(String eventName) {
        Toast.makeText(requireContext(), "Event Saved: " + eventName, Toast.LENGTH_SHORT).show();

        CalendarDay selectedDate = calendarView.getSelectedDate();
        if (selectedDate != null) {
            calendarView.addDecorator(new EventDecorator(selectedDate));
        } else {
            Toast.makeText(requireContext(), "Please select a date first", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Decorator to add a red dot on the selected date.
     */
    private static class EventDecorator implements com.prolificinteractive.materialcalendarview.DayViewDecorator {
        private final CalendarDay date;

        public EventDecorator(CalendarDay date) {
            this.date = date;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.equals(date);
        }

        @Override
        public void decorate(com.prolificinteractive.materialcalendarview.DayViewFacade view) {
            view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(5, android.graphics.Color.RED)); // Red dot for events
        }
    }
}