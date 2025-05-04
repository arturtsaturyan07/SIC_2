package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<UnreadChatPreview> unreadChats = new ArrayList<>();
    private DatabaseReference userChatsRef;
    private String currentUserId;

    public NotificationFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter(unreadChats);
        recyclerView.setAdapter(adapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            userChatsRef = FirebaseDatabase.getInstance()
                    .getReference("user_chats")
                    .child(currentUserId);

            loadUnreadChats();
        }

        return view;
    }

    private void loadUnreadChats() {
        if (userChatsRef == null || currentUserId == null) return;

        userChatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                unreadChats.clear();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Boolean isRead = chatSnapshot.child("read").getValue(Boolean.class);
                    if (isRead == null || !isRead) {
                        String cardId = chatSnapshot.getKey();
                        String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                        Long timestamp = chatSnapshot.child("timestamp").getValue(Long.class);

                        if (cardId != null && lastMessage != null && timestamp != null) {
                            unreadChats.add(new UnreadChatPreview(cardId, lastMessage, timestamp));
                        }
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