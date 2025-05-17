package com.example.sic_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

/**
 * Adapter for displaying Card objects in a RecyclerView.
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    private List<Card> cardList;
    private final OnCardClickListener listener;
    private final boolean showSharedIndicator;

    public interface OnCardClickListener {
        void onCardClick(Card card);
        void onCardLongClick(Card card);
    }

    public CardAdapter(List<Card> cardList, OnCardClickListener listener, boolean showSharedIndicator) {
        this.cardList = cardList;
        this.listener = listener;
        this.showSharedIndicator = showSharedIndicator;
    }

    /**
     * Update the adapter's data and refresh the list.
     */
    public void updateData(List<Card> newCards) {
        this.cardList = newCards;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rec_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cardList.get(position);

        // Set card data
        holder.recTitle.setText(showSharedIndicator ? "[Shared] " + card.getTitle() : card.getTitle());
        holder.recDesc.setText(card.getDescription());
        holder.recPriority.setText(card.getPriority());
        holder.recImage.setImageResource(R.drawable.uploadimg);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardClick(card);
            }
        });

        // Set long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onCardLongClick(card);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return cardList != null ? cardList.size() : 0;
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView recImage;
        TextView recTitle;
        TextView recPriority;
        TextView recDesc;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            recImage = itemView.findViewById(R.id.recImage);
            recTitle = itemView.findViewById(R.id.recTitle);
            recPriority = itemView.findViewById(R.id.recPriority);
            recDesc = itemView.findViewById(R.id.recDesc);
        }
    }
}