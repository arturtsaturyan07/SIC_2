package com.example.sic_2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class CampCalendarFragment extends Fragment {
    private CalendarView calendarView;
    private ListView eventListView;
    private Button addEventBtn;
    private String cardId;
    private String currentUserId;
    private Card card;
    private List<CampEvent> events = new ArrayList<>();
    private Map<Long, List<CampEvent>> eventMap = new HashMap<>();
    private DatabaseReference eventsRef;
    private boolean isAdmin = false;
    private ArrayAdapter<String> adapter;
    private long lastSelectedDate = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camp_calendar, container, false);
        calendarView = v.findViewById(R.id.calendarView);
        eventListView = v.findViewById(R.id.eventListView);
        addEventBtn = v.findViewById(R.id.addEventBtn);

        // Fix: get cardId from arguments and currentUserId safely
        Bundle args = getArguments();
        cardId = args != null ? args.getString("cardId") : null;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;

        if (cardId == null || currentUserId == null) {
            Toast.makeText(requireContext(), "Card or user info missing!", Toast.LENGTH_SHORT).show();
            addEventBtn.setVisibility(View.GONE);
            return v;
        }

        // Fix: The card may be in "cards/<userId>/<cardId>" or "allCards/<cardId>" if shareable
        // Try both locations
        DatabaseReference cardsRefUser = FirebaseDatabase.getInstance().getReference("cards").child(currentUserId).child(cardId);
        DatabaseReference cardsRefAll = FirebaseDatabase.getInstance().getReference("allCards").child(cardId);

        // Try user cards node first, then allCards if not found
        cardsRefUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                card = snapshot.getValue(Card.class);
                if (card != null && card.getAuthorId() != null) {
                    isAdmin = card.getAuthorId().equals(currentUserId);
                    setupCalendarBounds();
                    loadEvents();
                } else {
                    // Try allCards/...
                    cardsRefAll.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            card = snapshot.getValue(Card.class);
                            if (card != null && card.getAuthorId() != null) {
                                isAdmin = card.getAuthorId().equals(currentUserId);
                                setupCalendarBounds();
                                loadEvents();
                            } else {
                                Toast.makeText(requireContext(), "Card or author info missing!", Toast.LENGTH_SHORT).show();
                                addEventBtn.setVisibility(View.GONE);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            lastSelectedDate = cal.getTimeInMillis();
            showEventsForDate(lastSelectedDate);
            addEventBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        });

        addEventBtn.setOnClickListener(v1 -> {
            if (lastSelectedDate != 0) showAddEventDialog(lastSelectedDate);
            else showAddEventDialog(calendarView.getDate());
        });

        return v;
    }

    private void setupCalendarBounds() {
        if (card == null) return;
        if (card.getCampStartDate() > 0) calendarView.setMinDate(card.getCampStartDate());
        if (card.getCampEndDate() > 0) calendarView.setMaxDate(card.getCampEndDate());
    }

    private void loadEvents() {
        eventsRef = FirebaseDatabase.getInstance().getReference("events").child(cardId);
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                events.clear(); eventMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CampEvent event = ds.getValue(CampEvent.class);
                    if (event != null) {
                        events.add(event);
                        long day = getStartOfDay(event.getDate());
                        if (!eventMap.containsKey(day)) eventMap.put(day, new ArrayList<>());
                        eventMap.get(day).add(event);
                    }
                }
                if (lastSelectedDate != 0) showEventsForDate(lastSelectedDate);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEventsForDate(long date) {
        long day = getStartOfDay(date);
        List<CampEvent> dayEvents = eventMap.getOrDefault(day, new ArrayList<>());
        List<String> eventTitles = new ArrayList<>();
        for (CampEvent e : dayEvents) {
            eventTitles.add(e.getTitle() + (e.getDescription() != null ? ("\n" + e.getDescription()) : ""));
        }
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, eventTitles);
        eventListView.setAdapter(adapter);

        eventListView.setOnItemClickListener((parent, view, position, id) -> {
            CampEvent evt = dayEvents.get(position);
            new AlertDialog.Builder(requireContext())
                    .setTitle(evt.getTitle())
                    .setMessage(evt.getDescription())
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void showAddEventDialog(long date) {
        if (!isAdmin) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Event");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText titleInput = new EditText(requireContext());
        titleInput.setHint("Event Title");
        layout.addView(titleInput);

        final EditText descInput = new EditText(requireContext());
        descInput.setHint("Event Description");
        descInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(descInput);

        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();
            if (!title.isEmpty()) {
                String eventId = eventsRef.push().getKey();
                CampEvent event = new CampEvent(eventId, date, title, desc, currentUserId);
                eventsRef.child(eventId).setValue(event);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private long getStartOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static CampCalendarFragment newInstance(String cardId) {
        CampCalendarFragment fragment = new CampCalendarFragment();
        Bundle args = new Bundle();
        args.putString("cardId", cardId);
        fragment.setArguments(args);
        return fragment;
    }
}