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
import androidx.appcompat.app.AppCompatActivity;
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
        ImageButton backButton = findViewById(R.id.backbutton);
        Button addUserButton = findViewById(R.id.addUserButton);
        Button createEventButton = findViewById(R.id.create_event_button);
        calendarView = findViewById(R.id.calendar_view);

        if (cardId != null) {
            checkUserAccess();
        }

        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(Card.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        addUserButton.setOnClickListener(view -> {
            Intent intent = new Intent(Card.this, UserAddActivity.class);
            intent.putExtra("cardId", cardId);
            startActivity(intent);
        });

        createEventButton.setOnClickListener(view -> showCreateEventDialog());
    }

    private void checkUserAccess() {
        database.child(cardId).child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    loadCardData();
                } else {
                    Toast.makeText(Card.this, "Access denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to check access", error.toException());
            }
        });
    }

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

    private void showCreateEventDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Create Event");

        final androidx.appcompat.widget.AppCompatEditText eventNameInput = new androidx.appcompat.widget.AppCompatEditText(this);
        eventNameInput.setHint("Event Name");
        builder.setView(eventNameInput);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String eventName = eventNameInput.getText().toString();
            if (!eventName.isEmpty()) {
                saveEvent(eventName);
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
        }
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
            view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(5, android.graphics.Color.RED)); // Red dot for events
        }
    }
}