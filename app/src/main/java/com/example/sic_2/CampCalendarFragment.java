package com.example.sic_2;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class CampCalendarFragment extends Fragment {
    private CalendarView calendarView;
    private RecyclerView eventRecyclerView;
    private Button addEventBtn;
    private String cardId;
    private String currentUserId;
    private Card card;
    private List<CampEvent> events = new ArrayList<>();
    private Map<Long, List<CampEvent>> eventMap = new HashMap<>();
    private DatabaseReference eventsRef;
    private boolean isAdmin = false;
    private CampEventAdapter adapter;
    private long lastSelectedDate = 0;
    private Uri pickedImageUri = null;

    // For image picking in dialog
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure Cloudinary is initialized (replace with your keys)
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "YOUR_CLOUD_NAME");
            config.put("api_key", "YOUR_API_KEY");
            config.put("api_secret", "YOUR_API_SECRET");
            MediaManager.init(requireContext().getApplicationContext(), config);
        } catch (IllegalStateException e) { /* Already initialized, ignore */ }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        pickedImageUri = result.getData().getData();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camp_calendar, container, false);
        calendarView = v.findViewById(R.id.calendarView);
        eventRecyclerView = v.findViewById(R.id.eventRecyclerView); // Change to RecyclerView in your XML!
        addEventBtn = v.findViewById(R.id.addEventBtn);

        // Setup recycler view
        adapter = new CampEventAdapter(new ArrayList<>());
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        eventRecyclerView.setAdapter(adapter);

        Bundle args = getArguments();
        cardId = args != null ? args.getString("cardId") : null;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;

        if (cardId == null || currentUserId == null) {
            Toast.makeText(requireContext(), "Card or user info missing!", Toast.LENGTH_SHORT).show();
            addEventBtn.setVisibility(View.GONE);
            return v;
        }

        DatabaseReference cardsRefUser = FirebaseDatabase.getInstance().getReference("cards").child(currentUserId).child(cardId);
        DatabaseReference cardsRefAll = FirebaseDatabase.getInstance().getReference("allCards").child(cardId);

        cardsRefUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                card = snapshot.getValue(Card.class);
                if (card != null && card.getAuthorId() != null) {
                    isAdmin = card.getAuthorId().equals(currentUserId);
                    setupCalendarBounds();
                    loadEvents();
                } else {
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
        adapter.setEvents(dayEvents);
    }

    private void showAddEventDialog(long date) {
        if (!isAdmin) return;
        pickedImageUri = null;
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

        final Button pickTimeBtn = new Button(requireContext());
        pickTimeBtn.setText("Pick Time");
        layout.addView(pickTimeBtn);

        final TextView pickedTimeView = new TextView(requireContext());
        pickedTimeView.setText("No time selected");
        layout.addView(pickedTimeView);

        final int[] pickedHour = { -1 };
        final int[] pickedMinute = { -1 };

        pickTimeBtn.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                pickedHour[0] = hourOfDay;
                pickedMinute[0] = minute;
                pickedTimeView.setText(String.format(Locale.getDefault(), "Selected time: %02d:%02d", hourOfDay, minute));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);
            dialog.show();
        });

        final ImageView imagePreview = new ImageView(requireContext());
        imagePreview.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
        imagePreview.setImageResource(R.drawable.uploadimg);
        imagePreview.setPadding(0, 24, 0, 24);
        imagePreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
        layout.addView(imagePreview);

        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pickedHour[0] == -1 || pickedMinute[0] == -1) {
                Toast.makeText(requireContext(), "Please pick a time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set event timestamp to selected date + picked hour/minute
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date);
            cal.set(Calendar.HOUR_OF_DAY, pickedHour[0]);
            cal.set(Calendar.MINUTE, pickedMinute[0]);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long eventTime = cal.getTimeInMillis();

            if (pickedImageUri != null) {
                uploadImageToCloudinary(pickedImageUri, imageUrl -> {
                    saveEvent(eventTime, title, desc, imageUrl);
                });
            } else {
                saveEvent(eventTime, title, desc, null);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveEvent(long dateTime, String title, String desc, String imageUrl) {
        String eventId = eventsRef.push().getKey();
        CampEvent event = new CampEvent(eventId, dateTime, title, desc, currentUserId, imageUrl);
        eventsRef.child(eventId).setValue(event);
    }

    private void uploadImageToCloudinary(Uri uri, OnImageUploadedListener listener) {
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        listener.onUploaded(url);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show();
                        listener.onUploaded(null);
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }
    private interface OnImageUploadedListener {
        void onUploaded(String imageUrl);
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

    // ====== Event Recycler Adapter for displaying event details ======
    public static class CampEventAdapter extends RecyclerView.Adapter<CampEventAdapter.ViewHolder> {
        private List<CampEvent> eventList;
        public CampEventAdapter(List<CampEvent> eventList) {
            this.eventList = eventList;
        }
        public void setEvents(List<CampEvent> list) {
            this.eventList = list;
            notifyDataSetChanged();
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camp_event, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CampEvent evt = eventList.get(position);
            holder.title.setText(evt.getTitle());
            holder.desc.setText(evt.getDescription());
            holder.time.setText(android.text.format.DateFormat.format("HH:mm", evt.getDate()));
            if (evt.getImageUrl() != null && !evt.getImageUrl().isEmpty()) {
                Glide.with(holder.img.getContext()).load(evt.getImageUrl()).into(holder.img);
            } else {
                holder.img.setImageResource(R.drawable.uploadimg);
            }
        }
        @Override
        public int getItemCount() {
            return eventList == null ? 0 : eventList.size();
        }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, desc, time;
            ImageView img;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.eventTitle);
                desc = v.findViewById(R.id.eventDesc);
                time = v.findViewById(R.id.eventTime);
                img = v.findViewById(R.id.eventImage);
            }
        }
    }
}