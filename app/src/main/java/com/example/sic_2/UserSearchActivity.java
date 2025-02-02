package com.example.sic_2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserSearchActivity extends AppCompatActivity {

    private EditText userIdInput;
    private Button searchButton;
    private TextView resultTextView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        userIdInput = findViewById(R.id.userIdInput);
        searchButton = findViewById(R.id.searchButton);
        resultTextView = findViewById(R.id.resultTextView);

        searchButton.setOnClickListener(v -> searchUserById(userIdInput.getText().toString()));
    }

    private void searchUserById(String userId) {
        Log.d("UserSearchActivity", "Searching for user ID: " + userId);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Use a User model class instead of String
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        resultTextView.setText("User found: " + user.getName()); // Adjust field accordingly
                    } else {
                        resultTextView.setText("User data is null.");
                    }
                } else {
                    resultTextView.setText("User not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                resultTextView.setText("Error: " + databaseError.getMessage());
            }
        });
    }


}
