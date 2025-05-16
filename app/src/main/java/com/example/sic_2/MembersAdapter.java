package com.example.sic_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private Context context;
    private List<String> membersList;

    public MembersAdapter(List<String> membersList) {
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
        String memberId = membersList.get(position);
        holder.memberIdTextView.setText(memberId);
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView memberIdTextView;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            memberIdTextView = itemView.findViewById(R.id.member_name);
        }
    }
}