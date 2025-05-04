package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<ChatPreview> chatPreviews = new ArrayList<>();
    private ChatPreviewAdapter adapter;
    private DatabaseReference userChatsRef;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        recyclerView = view.findViewById(R.id.recycler_view_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatPreviewAdapter(chatPreviews, getContext());
        recyclerView.setAdapter(adapter);

        userChatsRef = FirebaseDatabase.getInstance()
                .getReference("user_chats")
                .child(currentUserId);

        loadUnreadChats();

        return view;
    }

    private void loadUnreadChats() {
        userChatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatPreviews.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Boolean isRead = (Boolean) chatSnapshot.child("read").getValue();
                    if (isRead == null || !isRead) {
                        String cardId = chatSnapshot.getKey();
                        String lastMessage = (String) chatSnapshot.child("lastMessage").getValue();
                        long timestamp = (long) chatSnapshot.child("timestamp").getValue();

                        chatPreviews.add(new ChatPreview(cardId, lastMessage, timestamp));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load unread chats", error.toException());
            }
        });
    }
}