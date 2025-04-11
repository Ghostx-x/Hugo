package com.example.hugo;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInFirst extends AppCompatActivity {

    private EditText nameField, emailField, passwordField;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private boolean isRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_first);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        registerButton = findViewById(R.id.btn_signIn_first);

        registerButton.setOnClickListener(v -> {
            if (isRegistered) {
                checkEmailVerification();
            } else {
                registerUser();
            }
        });

        // Password toggle logic
        ImageView passwordToggle = findViewById(R.id.passwordToggle);
        passwordToggle.setOnClickListener(v -> {
            if (passwordField.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_eye_off);
            } else {
                passwordField.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_eye);
            }
            passwordField.setSelection(passwordField.getText().length());
        });
    }

    private void registerUser() {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendEmailVerification(user);
                            storeUserData(user.getUid(), new User(name, email));
                            isRegistered = true;
                        }
                    } else {
                        Toast.makeText(SignInFirst.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SignInFirst.this, "Verification email sent! Please check your inbox.", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            } else {
                Toast.makeText(SignInFirst.this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkEmailVerification() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(SignInFirst.this, "Email Verified! Redirecting to SignIn...", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInFirst.this, SignInActivity.class));
                            finish();
                        } else {
                            Toast.makeText(SignInFirst.this, "Please verify your email before proceeding.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        Toast.makeText(SignInFirst.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void storeUserData(String userId, User user) {
        databaseRef.child(userId).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SignInFirst.this, "Registration Successful. Please verify your email.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(SignInFirst.this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public static class User {
        public String name, email;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
