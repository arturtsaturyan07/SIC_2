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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Add Glide to your dependencies!
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MembersFragment extends Fragment {

    private static final String TAG = "MembersFragment";

    private RecyclerView recyclerView;
    private MembersAdapter adapter;
    private List<MemberInfo> membersList;
    private DatabaseReference membersRef;
    private DatabaseReference usersRef;
    private String cardId;
    private String currentUserId;
    private String authorId;

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
        authorId = requireArguments().getString("authorId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firebase refs
        membersRef = FirebaseDatabase.getInstance().getReference("allCards").child(cardId).child("campMembers");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        fetchMembers();

        // Debug log
        Log.d(TAG, "Current User ID: " + currentUserId);
        Log.d(TAG, "Author ID: " + authorId);
    }

    private void fetchMembers() {
        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersList.clear();
                final List<String> memberIds = new ArrayList<>();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    memberIds.add(memberId);
                }
                // Fetch user info (name, photo) for each member
                if (!memberIds.isEmpty()) {
                    for (String memberId : memberIds) {
                        usersRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                String name = userSnap.child("name").getValue(String.class);
                                String photoUrl = userSnap.child("photoUrl").getValue(String.class); // Assumes you save URLs under "photoUrl"
                                membersList.add(new MemberInfo(memberId, name, photoUrl));
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to fetch user info", error.toException());
                            }
                        });
                    }
                } else {
                    adapter.notifyDataSetChanged();
                }

                // Check if current user is the admin
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

    // MemberInfo class to hold user info
    public static class MemberInfo {
        public String userId;
        public String name;
        public String photoUrl;

        public MemberInfo(String userId, String name, String photoUrl) {
            this.userId = userId;
            this.name = name != null ? name : userId;
            this.photoUrl = photoUrl;
        }
    }

    // Adapter with Glide for profile images
    public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
        private List<MemberInfo> members;

        public MembersAdapter(List<MemberInfo> members) {
            this.members = members;
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
            return new MemberViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            MemberInfo member = members.get(position);
            holder.nameTextView.setText(member.name);
            if (member.photoUrl != null && !member.photoUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(member.photoUrl)
                        .placeholder(R.drawable.ic_profile_placeholder) // your placeholder image
                        .error(R.drawable.ic_profile_placeholder)
                        .into(holder.profileImageView);
            } else {
                holder.profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Open ProfileActivity when member item is clicked
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), ProfileActivity.class);
                intent.putExtra("userId", member.userId);
                holder.itemView.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            de.hdodenhof.circleimageview.CircleImageView profileImageView; // Use CircleImageView for rounded images
            TextView nameTextView;

            MemberViewHolder(View itemView) {
                super(itemView);
                profileImageView = itemView.findViewById(R.id.member_profile_pic);
                nameTextView = itemView.findViewById(R.id.member_name);
            }
        }
    }
}