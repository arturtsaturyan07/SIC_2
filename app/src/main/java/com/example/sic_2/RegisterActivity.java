package com.example.sic_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText etRegEmail;
    TextInputEditText etRegPassword;
    public static TextInputEditText etRegName;
    TextInputEditText etRegSurname;
    TextView tvLoginHere;
    Button btnRegister;
    FirebaseAuth mAuth;
    DatabaseReference usersRef;

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
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        btnRegister.setOnClickListener(view -> createUser());
        tvLoginHere.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void createUser() {
        String email = Objects.requireNonNull(etRegEmail.getText()).toString();
        String password = Objects.requireNonNull(etRegPassword.getText()).toString();
        String name = Objects.requireNonNull(etRegName.getText()).toString();
        String surname = Objects.requireNonNull(etRegSurname.getText()).toString();

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
                        // Create user profile in Realtime Database
                        createUserProfile(firebaseUser, name, surname, email);

                        // Send verification email
                        firebaseUser.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                            if (verificationTask.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this,
                                        "Verification email sent. Please check your inbox.",
                                        Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Failed to send verification email: " + verificationTask.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Registration Error: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void createUserProfile(FirebaseUser user, String name, String surname, String email) {
        String userId = user.getUid();

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("name", name);
        userProfile.put("surname", surname);
        userProfile.put("email", email);
        userProfile.put("timestamp", System.currentTimeMillis());

        usersRef.child(userId).setValue(userProfile)
                .addOnSuccessListener(aVoid -> Log.d("RegisterActivity", "User profile created"))
                .addOnFailureListener(e -> Log.e("RegisterActivity", "Failed to create user profile", e));
    }
}