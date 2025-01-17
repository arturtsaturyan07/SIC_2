package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;


public class RegisterActivity extends AppCompatActivity {

    TextInputEditText etRegEmail;
    TextInputEditText etRegPassword;
    TextView tvLoginHere;
    Button btnRegister;

    FirebaseAuth mAuth;
    private int verificationCode; // Store the generated verification code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPass);
        tvLoginHere = findViewById(R.id.tvLoginHere);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(view -> {
            createUser();
        });

        tvLoginHere.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    private void createUser() {
        String email = etRegEmail.getText().toString();
        String password = etRegPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("Email cannot be empty");
            etRegEmail.requestFocus();
        } else if (TextUtils.isEmpty(password)) {
            etRegPassword.setError("Password cannot be empty");
            etRegPassword.requestFocus();
        } else {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();

                        // Generate and send verification code
                        verificationCode = generateVerificationCode();
                        sendVerificationEmail(email, verificationCode);

                        // Show input dialog for verification code
                        showVerificationDialog(email);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private int generateVerificationCode() {
        Random rand = new Random();
        return rand.nextInt(999999); // Generates a number between 0 and 999999
    }

    private void sendVerificationEmail(String email, int verificationCode) {
        // Create a Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://<your-region>-<your-project-id>.cloudfunctions.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        EmailService emailService = retrofit.create(EmailService.class);

        // Create the email request object
        EmailRequest emailRequest = new EmailRequest(email, verificationCode);

        // Make the API call
        emailService.sendEmail(emailRequest).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Verification email sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Failed to send email", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVerificationDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Verification Code");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String enteredCode = input.getText().toString();
            if (enteredCode.equals(String.valueOf(verificationCode))) {
                // Verification successful, proceed to login or main activity
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            } else {
                // Show error message
                Toast.makeText(RegisterActivity.this, "Invalid code, please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Retrofit service interface
    public interface EmailService {
        // Adjust this to your function's endpoint
        Call<Void> sendEmail(@Body EmailRequest emailRequest);
    }

    // Email request class
    public static class EmailRequest {
        private String email;
        private int verificationCode;

        public EmailRequest(String email, int verificationCode) {
            this.email = email;
            this.verificationCode = verificationCode;
        }
    }
}
