package com.example.sic_2;

import static com.example.sic_2.LoginActivity.email_;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText etRegEmail;
    TextInputEditText etRegPassword;
    TextInputEditText etRegName;
    TextInputEditText etRegSurname;
    TextView tvLoginHere;
    Button btnRegister;
    public static String name_;
    public static String surname_;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPass);
        etRegName = findViewById(R.id.etRegName);
        etRegSurname = findViewById(R.id.etRegSurname);
        tvLoginHere = findViewById(R.id.tvLoginHere);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(view -> createUser());
        tvLoginHere.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void createUser() {
        String email = Objects.requireNonNull(etRegEmail.getText()).toString();
        String password = Objects.requireNonNull(etRegPassword.getText()).toString();
        String name = Objects.requireNonNull(etRegName.getText()).toString();
        String surname = Objects.requireNonNull(etRegSurname.getText()).toString();

        // Input validation
        if (TextUtils.isEmpty(name)) {
            etRegName.setError("Name cannot be empty");
            etRegName.requestFocus();
        } else if (TextUtils.isEmpty(surname)) {
            etRegSurname.setError("Surname cannot be empty");
            etRegSurname.requestFocus();
        } else if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("Email cannot be empty");
            etRegEmail.requestFocus();
        } else if (TextUtils.isEmpty(password)) {
            etRegPassword.setError("Password cannot be empty");
            etRegPassword.requestFocus();
        } else {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        firebaseUser.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                            if (verificationTask.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Failed to send verification email: " + verificationTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static class User {
        private String username;
        private String name; // Added Name
        private String surname; // Added Surname
        private String email;

        public User(String username, String name, String surname, String email) {
            this.username = username;
            this.name = name; // Initialize Name
            this.surname = surname; // Initialize Surname
            this.email = email;
            name_ = this.name;
            surname_ = this.surname;
        }
    }
}