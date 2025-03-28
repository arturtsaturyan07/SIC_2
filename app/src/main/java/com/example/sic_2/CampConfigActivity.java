package com.example.sic_2;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.Collections;

public class CampConfigActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camp_config);

        calendarView = findViewById(R.id.calendar_view);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            String selectedDate = date.getYear() + "-" + (date.getMonth() + 1) + "-" + date.getDay();
            Toast.makeText(this, "Selected Date: " + selectedDate, Toast.LENGTH_SHORT).show();
        });

        Button createEventButton = findViewById(R.id.create_event_button);
        createEventButton.setOnClickListener(v -> showCreateEventDialog());
    }

    private void showCreateEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Event");

        final EditText eventNameInput = new EditText(this);
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
            calendarView.addDecorator(new EventDecorator(Color.RED, Collections.singleton(selectedDate)));
        }
    }
}