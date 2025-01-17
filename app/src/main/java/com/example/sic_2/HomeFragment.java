package com.example.sic_2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;


import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private ArrayList<String> messages = new ArrayList<>();
    private TextView messageTextView; // Assuming you have a TextView to display messages

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        messageTextView = view.findViewById(R.id.messageTextView); // Initialize your TextView
        return view;
    }

    public void addMessage(String message) {
        messages.add(message);
        updateMessageDisplay();
    }

    private void updateMessageDisplay() {
        StringBuilder displayText = new StringBuilder();
        for (String msg : messages) {
            displayText.append(msg).append("\n");
        }
        messageTextView.setText(displayText.toString());
    }
}
