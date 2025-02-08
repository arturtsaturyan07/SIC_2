package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();
        TextView tvVerifyEmailMessage = findViewById(R.id.tvVerifyEmailMessage);
        Button btnVerifyEmail = findViewById(R.id.btnVerifyEmail);

        String email = getIntent().getStringExtra("email");
        tvVerifyEmailMessage.setText("A verification email has been sent to " + email + ". Please verify your email and click the button below.");

        btnVerifyEmail.setOnClickListener(view -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.reload().addOnCompleteListener(reloadTask -> {
                    if (reloadTask.isSuccessful()) {
                        if (user.isEmailVerified()) {
                            Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to reload user data: " + reloadTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "User not found. Please register again.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, RegisterActivity.class));
                finish();
            }
        });
    }
}