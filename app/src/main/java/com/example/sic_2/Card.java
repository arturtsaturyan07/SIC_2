package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

public class Card extends AppCompatActivity {
    private DatabaseReference database;
    private TextView cardMessage;
    private MaterialCalendarView calendarView;
    private String cardId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_view_layout);

        database = FirebaseDatabase.getInstance().getReference("cards");
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        cardId = getIntent().getStringExtra("cardId");

        cardMessage = findViewById(R.id.card_message);
        ImageButton backButton = findViewById(R.id.back_button);
        Button addUserButton = findViewById(R.id.addUserButton);
        Button createEventButton = findViewById(R.id.create_event_button);
        //Button deleteButton = findViewById(R.id.delete_button);

        if (cardId == null) {
            Toast.makeText(this, "Card ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkUserAccess();

        backButton.setOnClickListener(v -> finish());
        addUserButton.setOnClickListener(v -> {
            Intent intent = new Intent(Card.this, UserAddActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });
        createEventButton.setOnClickListener(v -> showCreateEventDialog());
        //deleteButton.setOnClickListener(v -> deleteCard());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_main) {
                    // Navigate to Main Activity
                    Intent mainIntent = new Intent(Card.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_chat) {
                    // Navigate to Chat Activity
                    Intent chatIntent = new Intent(Card.this, ChatActivity.class);
                    startActivity(chatIntent);
                    finish();
                    return true;
                }
                return false;
            });
        } else {
            Log.e("CardActivity", "BottomNavigationView is null. Check your layout file.");
        }
    }

    private void checkUserAccess() {
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    loadCardData();
                } else {
                    Toast.makeText(Card.this, "Access denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to check access", error.toException());
                Toast.makeText(Card.this, "Failed to check access", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadCardData() {
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);
        cardRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String message = snapshot.child("message").getValue(String.class);
                if (message != null) {
                    cardMessage.setText(message);
                } else {
                    Toast.makeText(Card.this, "Card data is incomplete", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load card", error.toException());
                Toast.makeText(Card.this, "Failed to load card", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showCreateEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Event");

        final androidx.appcompat.widget.AppCompatEditText eventNameInput = new androidx.appcompat.widget.AppCompatEditText(this);
        eventNameInput.setHint("Event Name");
        builder.setView(eventNameInput);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String eventName = eventNameInput.getText().toString().trim();
            if (!eventName.isEmpty()) {
                saveEvent(eventName);
            } else {
                Toast.makeText(this, "Event name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveEvent(String eventName) {
        Toast.makeText(this, "Event Saved: " + eventName, Toast.LENGTH_SHORT).show();

        CalendarDay selectedDate = calendarView.getSelectedDate();
        if (selectedDate != null) {
            calendarView.addDecorator(new EventDecorator(selectedDate));
        } else {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCard() {
        DatabaseReference cardRef = database.child(currentUserId).child(cardId);
        cardRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to delete card", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
            view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(5, android.graphics.Color.RED));
        }
    }
}