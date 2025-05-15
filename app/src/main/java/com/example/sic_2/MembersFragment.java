package com.example.sic_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

public class MembersFragment extends Fragment {

    private static final String TAG = "MembersFragment";

    private RecyclerView recyclerView;
    private MembersAdapter adapter;
    private List<String> membersList;
    private DatabaseReference membersRef;
    private String cardId;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_members, container, false);
        setupViews(view);
        return view;
    }

    private void setupViews(View view) {
        recyclerView = view.findViewById(R.id.members_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        membersList = new ArrayList<>();
        adapter = new MembersAdapter(membersList);
        recyclerView.setAdapter(adapter);

        // Get cardId and authorId from arguments
        cardId = requireArguments().getString("cardId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch members
        fetchMembers();

        // Debug log
        Log.d(TAG, "Current User ID: " + currentUserId);
        Log.d(TAG, "Author ID: " + requireArguments().getString("authorId"));
    }

    private void fetchMembers() {
        membersRef = FirebaseDatabase.getInstance().getReference("allCards").child(cardId).child("campMembers");

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersList.clear();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    membersList.add(memberId);
                }

                adapter.notifyDataSetChanged();

                // Check if current user is the admin
                String authorId = requireArguments().getString("authorId");
                if (currentUserId != null && authorId != null && currentUserId.equals(authorId)) {
                    // Admin can see the list
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    // Non-admins see a message
                    recyclerView.setVisibility(View.GONE);
                    TextView messageView = requireView().findViewById(R.id.no_access_message);
                    messageView.setVisibility(View.VISIBLE);
                    messageView.setText("You are not authorized to view this list.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch members", error.toException());
                Toast.makeText(requireContext(), "Failed to load members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static MembersFragment newInstance(String cardId, String authorId) {
        MembersFragment fragment = new MembersFragment();
        Bundle args = new Bundle();
        args.putString("cardId", cardId);
        args.putString("authorId", authorId); // Ensure authorId is passed here
        fragment.setArguments(args);
        return fragment;
    }
}