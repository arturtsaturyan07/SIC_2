package com.example.sic_2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.Collection;
import java.util.Collections;

public class BlankFragment extends Fragment {

    private MaterialCalendarView calendarView;

    public BlankFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_blank, container, false);

        // Initialize the calendar view
        calendarView = view.findViewById(R.id.calendar_view);

        // Handle date selection
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            String selectedDate = date.getYear() + "-" + (date.getMonth() + 1) + "-" + date.getDay();
            Toast.makeText(getContext(), "Selected Date: " + selectedDate, Toast.LENGTH_SHORT).show();
        });

        // Initialize the "Create Event" button
        Button createEventButton = view.findViewById(R.id.create_event_button);
        createEventButton.setOnClickListener(v -> showCreateEventDialog());

        return view;
    }

    private void showCreateEventDialog() {
        // Create a simple dialog for event creation
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Create Event");

        // Input fields for event details
        final androidx.appcompat.widget.AppCompatEditText eventNameInput = new androidx.appcompat.widget.AppCompatEditText(requireContext());
        eventNameInput.setHint("Event Name");
        builder.setView(eventNameInput);

        // Add buttons to the dialog
        builder.setPositiveButton("Save", (dialog, which) -> {
            String eventName = eventNameInput.getText().toString();
            if (!eventName.isEmpty()) {
                saveEvent(eventName);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // Show the dialog
        builder.show();
    }

    private void saveEvent(String eventName) {
        // Save the event (e.g., to a database or memory)
        Toast.makeText(requireContext(), "Event Saved: " + eventName, Toast.LENGTH_SHORT).show();

        // Optionally, mark the selected date on the calendar
        CalendarDay selectedDate = calendarView.getSelectedDate();
        if (selectedDate != null) {
            calendarView.addDecorator(new EventDecorator(selectedDate));
        }
    }

    // Custom decorator to mark dates with events
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