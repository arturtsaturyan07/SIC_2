package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirectChatsFragment extends Fragment {

    private static final String TAG = "DirectChatsFragment";

    private RecyclerView recyclerView;
    private DirectChatAdapter adapter;
    private List<User> directChatUsers;
    private DatabaseReference directChatsRef;
    private DatabaseReference usersRef;
    private String currentUserId;

    private TextView emptyStateText;
    private ValueEventListener chatsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_direct_chats, container, false);
        setupViews(view);
        setupFirebase();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the listener to avoid memory leaks
        if (directChatsRef != null && chatsListener != null) {
            directChatsRef.removeEventListener(chatsListener);
        }
    }

    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.direct_chats_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        directChatUsers = new ArrayList<>();
        adapter = new DirectChatAdapter(requireContext(), directChatUsers, user -> {
            String otherUserId = user.getUserId();
            String chatId = "direct_chat_" + sortUids(currentUserId, otherUserId);
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("cardId", chatId);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    private void setupFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        currentUserId = currentUser.getUid();
        directChatsRef = FirebaseDatabase.getInstance().getReference("direct_chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Listen to chats where current user is a participant (real-time updates)
        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                directChatUsers.clear();
                Set<String> seenUserIds = new HashSet<>();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    if (chatId != null && chatId.startsWith("direct_chat_")) {
                        String otherUserId = extractOtherUserId(chatId, currentUserId);
                        if (otherUserId != null && !seenUserIds.contains(otherUserId)) {
                            seenUserIds.add(otherUserId);
                            fetchUserInfo(otherUserId);
                        }
                    }
                }
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load direct chats: " + error.getMessage());
                Toast.makeText(requireContext(), "Failed to load direct chats", Toast.LENGTH_SHORT).show();
            }
        };

        directChatsRef.orderByChild("participants/" + currentUserId).equalTo(true)
                .addValueEventListener(chatsListener);
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();

        if (directChatUsers.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private String extractOtherUserId(String chatId, String currentUserId) {
        String[] parts = chatId.replaceFirst("direct_chat_", "").split("_");
        if (parts.length >= 2) {
            String uid1 = parts[0];
            String uid2 = parts[1];

            if (uid1.equals(currentUserId)) {
                return uid2;
            } else if (uid2.equals(currentUserId)) {
                return uid1;
            }
        }
        return null;
    }

    private void fetchUserInfo(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setUserId(snapshot.getKey()); // Make sure ID is set
                        // Add only if not already in the list (in case of rapid updates)
                        boolean alreadyInList = false;
                        for (User u : directChatUsers) {
                            if (u.getUserId().equals(user.getUserId())) {
                                alreadyInList = true;
                                break;
                            }
                        }
                        if (!alreadyInList) {
                            directChatUsers.add(user);
                            updateUI();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch user info", error.toException());
            }
        });
    }

    // Utility method to create sorted chat ID
    private String sortUids(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}