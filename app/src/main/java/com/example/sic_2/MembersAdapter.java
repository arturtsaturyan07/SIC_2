package com.example.sic_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private Context context;
    private List<MemberInfo> membersList;

    public MembersAdapter(List<MemberInfo> membersList) {
        this.membersList = membersList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberInfo member = membersList.get(position);
        holder.memberNameTextView.setText(member.name);

        // Show the organizer badge if this member is the organizer
        if (holder.organizerBadge != null) {
            if (member.name != null && member.name.toLowerCase().contains("organizer")) {
                holder.organizerBadge.setVisibility(View.VISIBLE);
            } else {
                holder.organizerBadge.setVisibility(View.GONE);
            }
        }

        // Optionally show the member's ID/email if needed
        if (holder.memberIdTextView != null) {
            holder.memberIdTextView.setText(member.userId);
            holder.memberIdTextView.setVisibility(View.VISIBLE);
        }

        // Set profile image if provided, else use placeholder
        if (holder.profileImageView != null) {
            if (member.photoUrl != null && !member.photoUrl.isEmpty()) {
                // Use Glide or Picasso if you want to load from URL
                // Glide.with(holder.profileImageView.getContext())
                //        .load(member.photoUrl)
                //        .placeholder(R.drawable.ic_profile_placeholder)
                //        .into(holder.profileImageView);
                // For this template, just use placeholder
                holder.profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            } else {
                holder.profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView memberNameTextView;
        TextView organizerBadge;
        TextView memberIdTextView;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.member_profile_pic);
            memberNameTextView = itemView.findViewById(R.id.member_name);
            organizerBadge = itemView.findViewById(R.id.member_organizer_badge);
            memberIdTextView = itemView.findViewById(R.id.member_id);
        }
    }

    // Helper model class for member info
    public static class MemberInfo {
        public String userId;
        public String name;
        public String photoUrl;

        public MemberInfo(String userId, String name, String photoUrl) {
            this.userId = userId;
            this.name = name;
            this.photoUrl = photoUrl;
        }
    }
}