package com.example.sic_2;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ReactionPopupFragment extends DialogFragment {
    public interface ReactionSelectListener {
        void onReactionSelected(String emoji);
    }

    private static final String[] EMOJIS = {"👍", "❤️", "😂", "😮", "😢", "😡"};
    private ReactionSelectListener listener;

    public void setReactionSelectListener(ReactionSelectListener l) { this.listener = l; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(30, 30, 30, 30);

        for (String emoji : EMOJIS) {
            TextView tv = new TextView(getContext());
            tv.setText(emoji);
            tv.setTextSize(32);
            tv.setPadding(24, 24, 24, 24);
            tv.setScaleX(0.6f);
            tv.setScaleY(0.6f);
            tv.setOnClickListener(v -> {
                if (listener != null) listener.onReactionSelected(emoji);
                dismiss();
            });
            root.addView(tv);
            // Animate pop-in
            tv.animate().scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(new OvershootInterpolator()).start();
        }
        root.setBackgroundResource(R.drawable.bg_reaction_popup);
        return root;
    }
}