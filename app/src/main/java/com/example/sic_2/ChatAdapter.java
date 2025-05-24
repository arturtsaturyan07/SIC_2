package com.example.sic_2;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final String currentUserId;
    private final Map<String, String> userNames;
    private final Map<String, String> userProfilePics;
    private final Context context;
    private final OnMessageLongClickListener longClickListener;

    public interface OnReactionClickListener {
        void onReactionClicked(ChatMessage msg, String emoji, int position);
        void onReactionLongClicked(ChatMessage msg, String emoji, int position);
        void onAddReaction(ChatMessage msg, int position, View anchor);
    }
    private OnReactionClickListener reactionClickListener;
    public void setOnReactionClickListener(OnReactionClickListener l) { this.reactionClickListener = l; }

    public ChatAdapter(Context context, List<ChatMessage> chatMessages, String currentUserId,
                       Map<String, String> userNames, Map<String, String> userProfilePics,
                       OnMessageLongClickListener longClickListener) {
        this.context = context;
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
        this.userNames = userNames;
        this.userProfilePics = userProfilePics;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        String senderId = chatMessage.getSenderId();
        boolean isSentByCurrentUser = senderId.equals(currentUserId);

        // Hide all containers first
        holder.containerMe.setVisibility(View.GONE);
        holder.containerOther.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(chatMessage.getTimestamp()));

        // --- Show reply layout if present (ME) ---
        if (isSentByCurrentUser) {
            if (chatMessage.getReplyToMessageId() != null) {
                holder.replyContainerMe.setVisibility(View.VISIBLE);
                holder.replySenderNameMe.setText(chatMessage.getReplyToSenderName());
                holder.replyMessageTextMe.setText(chatMessage.getReplyToMessageText() == null || chatMessage.getReplyToMessageText().isEmpty()
                        ? "[Media]"
                        : chatMessage.getReplyToMessageText());
            } else {
                holder.replyContainerMe.setVisibility(View.GONE);
            }
        }

        // --- Show reply layout if present (OTHER) ---
        if (!isSentByCurrentUser) {
            if (chatMessage.getReplyToMessageId() != null) {
                holder.replyContainerOther.setVisibility(View.VISIBLE);
                holder.replySenderNameOther.setText(chatMessage.getReplyToSenderName());
                holder.replyMessageTextOther.setText(chatMessage.getReplyToMessageText() == null || chatMessage.getReplyToMessageText().isEmpty()
                        ? "[Media]"
                        : chatMessage.getReplyToMessageText());
            } else {
                holder.replyContainerOther.setVisibility(View.GONE);
            }
        }

        // Sent by me
        if (isSentByCurrentUser) {
            holder.containerMe.setVisibility(View.VISIBLE);
            holder.messageTextMe.setVisibility(View.GONE);
            holder.messageImageMe.setVisibility(View.GONE);
            holder.circleVideoMe.setVisibility(View.GONE);
            holder.audioLayoutMe.setVisibility(View.GONE);

            // Circle video message
            if (chatMessage.getCircleVideoUrl() != null && !chatMessage.getCircleVideoUrl().isEmpty()) {
                holder.circleVideoMe.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chatMessage.getCircleVideoUrl())
                        .placeholder(R.drawable.video_placeholder)
                        .thumbnail(0.1f)
                        .into(holder.circleVideoMe);
                holder.circleVideoMe.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(chatMessage.getCircleVideoUrl()));
                    intent.setDataAndType(Uri.parse(chatMessage.getCircleVideoUrl()), "video/*");
                    context.startActivity(intent);
                });
            }

            // Audio message
            if (chatMessage.getAudioUrl() != null && !chatMessage.getAudioUrl().isEmpty()) {
                holder.audioLayoutMe.setVisibility(View.VISIBLE);
                setAudioDuration(chatMessage.getAudioUrl(), holder.audioDurationMe);
                holder.playAudioButtonMe.setOnClickListener(v ->
                        ((ChatActivity) context).playAudio(chatMessage.getAudioUrl(), holder.playAudioButtonMe, position, holder.audioDurationMe)
                );
            }

            // Image message
            if (chatMessage.getImageUrl() != null && !chatMessage.getImageUrl().isEmpty()) {
                holder.messageImageMe.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chatMessage.getImageUrl())
                        .placeholder(R.drawable.image_placeholder)
                        .into(holder.messageImageMe);
                holder.messageImageMe.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(chatMessage, position);
                        return true;
                    }
                    return false;
                });
            }
            // Text message
            if (chatMessage.getMessage() != null && !chatMessage.getMessage().isEmpty()) {
                holder.messageTextMe.setVisibility(View.VISIBLE);
                holder.messageTextMe.setText(chatMessage.getMessage());
            }
            holder.timeTextMe.setText(formattedTime);

            // Advanced reactions (dynamic, Telegram-style)
            renderReactions(holder.reactionsLayoutMe, chatMessage, position, true);

            holder.statusIndicator.setText("");
        } else {
            // Received from others
            holder.containerOther.setVisibility(View.VISIBLE);
            holder.messageTextOther.setVisibility(View.GONE);
            holder.messageImageOther.setVisibility(View.GONE);
            holder.circleVideoOther.setVisibility(View.GONE);
            holder.audioLayoutOther.setVisibility(View.GONE);

            String name = userNames.getOrDefault(senderId, "User");
            holder.senderNameOther.setText(name);

            // Circle video message
            if (chatMessage.getCircleVideoUrl() != null && !chatMessage.getCircleVideoUrl().isEmpty()) {
                holder.circleVideoOther.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chatMessage.getCircleVideoUrl())
                        .placeholder(R.drawable.video_placeholder)
                        .thumbnail(0.1f)
                        .into(holder.circleVideoOther);
                holder.circleVideoOther.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(chatMessage.getCircleVideoUrl()));
                    intent.setDataAndType(Uri.parse(chatMessage.getCircleVideoUrl()), "video/*");
                    context.startActivity(intent);
                });
            }

            // Audio message
            if (chatMessage.getAudioUrl() != null && !chatMessage.getAudioUrl().isEmpty()) {
                holder.audioLayoutOther.setVisibility(View.VISIBLE);
                setAudioDuration(chatMessage.getAudioUrl(), holder.audioDurationOther);
                holder.playAudioButtonOther.setOnClickListener(v ->
                        ((ChatActivity) context).playAudio(chatMessage.getAudioUrl(), holder.playAudioButtonOther, position, holder.audioDurationOther)
                );
            }

            // Text message
            if (chatMessage.getMessage() != null && !chatMessage.getMessage().isEmpty()) {
                holder.messageTextOther.setVisibility(View.VISIBLE);
                holder.messageTextOther.setText(chatMessage.getMessage());
            }
            // Image message
            if (chatMessage.getImageUrl() != null && !chatMessage.getImageUrl().isEmpty()) {
                holder.messageImageOther.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(chatMessage.getImageUrl())
                        .placeholder(R.drawable.image_placeholder)
                        .into(holder.messageImageOther);
                holder.messageImageOther.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(chatMessage, position);
                        return true;
                    }
                    return false;
                });
            }
            holder.timeTextOther.setText(formattedTime);

            // Advanced reactions (dynamic, Telegram-style)
            renderReactions(holder.reactionsLayoutOther, chatMessage, position, false);

            String profileUrl = userProfilePics.get(senderId);
            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(context)
                        .load(profileUrl)
                        .placeholder(R.drawable.default_profile)
                        .into(holder.profileImageOther);
            } else {
                holder.profileImageOther.setImageResource(R.drawable.default_profile);
            }
        }

        // Long-press support for each bubble
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(chatMessage, position);
                return true;
            }
            return false;
        });
    }

    // Shows the audio duration in the provided TextView
    private void setAudioDuration(String url, TextView durationView) {
        // Run on a background thread to avoid blocking the UI
        new Thread(() -> {
            MediaPlayer player = new MediaPlayer();
            try {
                player.setDataSource(url);
                player.prepare();
                int duration = player.getDuration() / 1000;
                String durationText = String.format(Locale.getDefault(), "%d:%02d", duration / 60, duration % 60);
                new Handler(context.getMainLooper()).post(() -> durationView.setText(durationText));
            } catch (Exception e) {
                new Handler(context.getMainLooper()).post(() -> durationView.setText("0:00"));
            } finally {
                player.release();
            }
        }).start();
    }

    private void renderReactions(LinearLayout reactionsLayout, ChatMessage chatMessage, int position, boolean isMe) {
        reactionsLayout.removeAllViews();
        Map<String, Map<String, Boolean>> reactions = chatMessage.getReactions();

        // Render each emoji with count
        if (reactions != null && !reactions.isEmpty()) {
            for (Map.Entry<String, Map<String, Boolean>> entry : reactions.entrySet()) {
                String emoji = entry.getKey();
                Map<String, Boolean> users = entry.getValue();
                int count = users != null ? users.size() : 0;
                boolean highlighted = users != null && users.containsKey(currentUserId);

                TextView emojiView = new TextView(context);
                emojiView.setText(emoji + (count > 1 ? " " + count : ""));
                emojiView.setTextSize(18f);
                emojiView.setBackgroundResource(
                        highlighted
                                ? R.drawable.bg_reaction_highlighted
                                : R.drawable.bg_reaction_neutral
                );
                emojiView.setPadding(22, 6, 22, 6);
                emojiView.setGravity(Gravity.CENTER);
                emojiView.setClickable(true);
                emojiView.setFocusable(true);

                emojiView.setOnClickListener(v -> {
                    if (reactionClickListener != null)
                        reactionClickListener.onReactionClicked(chatMessage, emoji, position);
                });
                emojiView.setOnLongClickListener(v -> {
                    if (reactionClickListener != null) {
                        reactionClickListener.onReactionLongClicked(chatMessage, emoji, position);
                        return true;
                    }
                    return false;
                });

                // Animate pop-in
                emojiView.setScaleX(0.7f);
                emojiView.setScaleY(0.7f);
                emojiView.animate().scaleX(1f).scaleY(1f).setDuration(220)
                        .setInterpolator(new OvershootInterpolator()).start();

                reactionsLayout.addView(emojiView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        // Sent by me
        LinearLayout containerMe;
        LinearLayout replyContainerMe;
        TextView replySenderNameMe;
        TextView replyMessageTextMe;
        TextView messageTextMe;
        ImageView messageImageMe;
        ShapeableImageView circleVideoMe;
        LinearLayout audioLayoutMe;
        ImageButton playAudioButtonMe;
        TextView audioDurationMe;
        TextView timeTextMe;
        TextView statusIndicator;
        LinearLayout reactionsLayoutMe;

        // Received from others
        LinearLayout containerOther;
        LinearLayout replyContainerOther;
        TextView replySenderNameOther;
        TextView replyMessageTextOther;
        ImageView profileImageOther;
        TextView senderNameOther;
        TextView messageTextOther;
        ImageView messageImageOther;
        ShapeableImageView circleVideoOther;
        LinearLayout audioLayoutOther;
        ImageButton playAudioButtonOther;
        TextView audioDurationOther;
        TextView timeTextOther;
        LinearLayout reactionsLayoutOther;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            // Sent by me
            containerMe = itemView.findViewById(R.id.message_container_me);
            replyContainerMe = itemView.findViewById(R.id.reply_container_me);
            replySenderNameMe = itemView.findViewById(R.id.reply_sender_name_me);
            replyMessageTextMe = itemView.findViewById(R.id.reply_message_text_me);
            messageTextMe = itemView.findViewById(R.id.message_text_me);
            messageImageMe = itemView.findViewById(R.id.message_image_me);
            circleVideoMe = itemView.findViewById(R.id.circle_video_me);
            audioLayoutMe = itemView.findViewById(R.id.audio_layout_me);
            playAudioButtonMe = itemView.findViewById(R.id.play_audio_button_me);
            audioDurationMe = itemView.findViewById(R.id.audio_duration_me);
            timeTextMe = itemView.findViewById(R.id.time_text_me);
            statusIndicator = itemView.findViewById(R.id.status_indicator_me);
            reactionsLayoutMe = itemView.findViewById(R.id.reactions_layout_me);

            // Received from other
            containerOther = itemView.findViewById(R.id.message_container_other);
            replyContainerOther = itemView.findViewById(R.id.reply_container_other);
            replySenderNameOther = itemView.findViewById(R.id.reply_sender_name_other);
            replyMessageTextOther = itemView.findViewById(R.id.reply_message_text_other);
            profileImageOther = itemView.findViewById(R.id.profile_image_other);
            senderNameOther = itemView.findViewById(R.id.sender_name_other);
            messageTextOther = itemView.findViewById(R.id.message_text_other);
            messageImageOther = itemView.findViewById(R.id.message_image_other);
            circleVideoOther = itemView.findViewById(R.id.circle_video_other);
            audioLayoutOther = itemView.findViewById(R.id.audio_layout_other);
            playAudioButtonOther = itemView.findViewById(R.id.play_audio_button_other);
            audioDurationOther = itemView.findViewById(R.id.audio_duration_other);
            timeTextOther = itemView.findViewById(R.id.time_text_other);
            reactionsLayoutOther = itemView.findViewById(R.id.reactions_layout_other);
        }
    }
}