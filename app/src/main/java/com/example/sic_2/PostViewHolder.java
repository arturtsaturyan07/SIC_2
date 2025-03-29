package com.example.sic_2;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PostViewHolder extends RecyclerView.ViewHolder {
    LinearLayout cardView;
    TextView postText;
    ImageView postImage;
    TextView postTime;

    public PostViewHolder(@NonNull View itemView) {
        super(itemView);
        cardView = itemView.findViewById(R.id.cardContainer);
        postText = itemView.findViewById(R.id.post_text);
        postImage = itemView.findViewById(R.id.post_image);
        postTime = itemView.findViewById(R.id.post_time);
    }
}