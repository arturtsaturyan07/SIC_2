package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SharedCardsAdapter extends RecyclerView.Adapter<SharedCardsAdapter.SharedCardViewHolder> {

    private List<SharedCard> sharedCardList;

    public SharedCardsAdapter(List<SharedCard> sharedCardList) {
        this.sharedCardList = sharedCardList;
    }

    @NonNull
    @Override
    public SharedCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_card, parent, false);
        return new SharedCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SharedCardViewHolder holder, int position) {
        SharedCard sharedCard = sharedCardList.get(position);
        holder.cardMessage.setText(sharedCard.getMessage());
        holder.sharedBy.setText("Shared by: " + sharedCard.getSharedBy());
    }

    @Override
    public int getItemCount() {
        return sharedCardList.size();
    }

    static class SharedCardViewHolder extends RecyclerView.ViewHolder {
        TextView cardMessage, sharedBy;

        public SharedCardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMessage = itemView.findViewById(R.id.card_message);
            sharedBy = itemView.findViewById(R.id.shared_by);
        }
    }
}