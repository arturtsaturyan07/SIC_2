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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText etRegEmail;
    TextInputEditText etRegPassword;
    TextInputEditText etRegName; // Added for Name
    TextInputEditText etRegSurname; // Added for Surname
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
        etRegName = findViewById(R.id.etRegName); // Initialize Name EditText
        etRegSurname = findViewById(R.id.etRegSurname); // Initialize Surname EditText
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
        String name = Objects.requireNonNull(etRegName.getText()).toString(); // Get Name
        String surname = Objects.requireNonNull(etRegSurname.getText()).toString(); // Get Surname

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
            // Create user with email and password
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // User registered successfully
                    String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                    String generatedUsername = "user_" + userId; // Generate username

                    // Create user object
                    User user = new User(generatedUsername, name, surname, email);

                    // Save user to Firestore
                    db.collection("users").document(userId).set(user)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                                // In RegisterActivity when starting MainActivity
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.putExtra("name", name_);
                                intent.putExtra("email", email_);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RegisterActivity.this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // Handle registration error
                    Toast.makeText(RegisterActivity.this, "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // User model class
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
