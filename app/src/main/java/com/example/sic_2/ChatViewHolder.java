package com.example.sic_2;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

class ChatViewHolder extends RecyclerView.ViewHolder {

    // Self message
    LinearLayout containerSelf;
    TextView messageTextSelf;
    TextView timeTextSelf;
    TextView statusIndicatorSelf;

    // Other message
    LinearLayout containerOther;
    CircleImageView profileImageOther;
    TextView senderNameOther;
    TextView messageTextOther;
    TextView timeTextOther;

    public ChatViewHolder(@NonNull View itemView) {
        super(itemView);

        // Self message views
        containerSelf = itemView.findViewById(R.id.message_container_me);
        messageTextSelf = itemView.findViewById(R.id.message_text_me);
        timeTextSelf = itemView.findViewById(R.id.time_text_me);
        statusIndicatorSelf = itemView.findViewById(R.id.status_indicator_me);

        // Other user's message views
        containerOther = itemView.findViewById(R.id.message_container_other);
        profileImageOther = itemView.findViewById(R.id.profile_image_other);
        senderNameOther = itemView.findViewById(R.id.sender_name_other);
        messageTextOther = itemView.findViewById(R.id.message_text_other);
        timeTextOther = itemView.findViewById(R.id.time_text_other);
    }
}