package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

public class DirectChatsFragment extends Fragment {

    private static final String TAG = "DirectChatsFragment";

    private RecyclerView recyclerView;
    private DirectChatAdapter adapter;
    private List<User> directChatUsers;
    private DatabaseReference userChatsRef;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_direct_chats, container, false);
        setupViews(view);
        setupFirebase();
        return view;
    }

    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.direct_chats_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        directChatUsers = new ArrayList<>();
        adapter = new DirectChatAdapter(requireContext(), directChatUsers);
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
        userChatsRef = FirebaseDatabase.getInstance().getReference("user_chats").child(currentUserId);

        userChatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                directChatUsers.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    if (chatId != null && chatId.startsWith("direct_chat_")) {
                        String otherUserId = extractOtherUserId(chatId, currentUserId);
                        if (otherUserId != null) {
                            fetchUserInfo(otherUserId);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load direct chats: " + error.getMessage());
                Toast.makeText(requireContext(), "Failed to load direct chats", Toast.LENGTH_SHORT).show();
            }
        });
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
                directChatUsers.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    if (chatId != null && chatId.startsWith("direct_chat_")) {
                        String otherUserId = extractOtherUserId(chatId, currentUserId);
                        if (otherUserId != null) {
                            fetchUserInfo(otherUserId);
                        }
                    }
                }

                // Optional: show empty state
                if (directChatUsers.isEmpty()) {
                    Toast.makeText(requireContext(), "No direct chats yet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch user info", error.toException());
            }
        });
    }
}