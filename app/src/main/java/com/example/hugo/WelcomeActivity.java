package com.example.hugo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "WelcomeActivity";
    private FirebaseAuth auth;
    private DatabaseReference databaseRef;
    private static final String TEST_USER_EMAIL = "nataligvrgn@gmail.com";
    private static final String TEST_USER_PASSWORD = "TestUser123!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        Button signInButton = findViewById(R.id.btn_signIn);
        Button loginButton = findViewById(R.id.btn_login);
        TextView testUserLink = findViewById(R.id.test_user_link);

        if (signInButton == null || loginButton == null || testUserLink == null) {
            Log.e(TAG, "One or more UI elements not found");
            Toast.makeText(this, "UI initialization failed", Toast.LENGTH_SHORT).show();
            return;
        }

        signInButton.setOnClickListener(v -> {
            Log.d(TAG, "Sign-in button clicked");
            Intent signInIntent = new Intent(WelcomeActivity.this, SignInFirst.class);
            startActivity(signInIntent);
        });

        loginButton.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            Intent loginIntent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(loginIntent);
        });

        testUserLink.setOnClickListener(v -> {
            Log.d(TAG, "Test user link clicked, attempting sign-in with test account");
            Toast.makeText(WelcomeActivity.this, "Attempting test user login...", Toast.LENGTH_SHORT).show();
            auth.signInWithEmailAndPassword(TEST_USER_EMAIL, TEST_USER_PASSWORD)
                    .addOnCompleteListener(WelcomeActivity.this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Test account sign-in successful");
                            if (auth.getCurrentUser() != null) {
                                String userId = auth.getCurrentUser().getUid();
                                Log.d(TAG, "Test user ID: " + userId);
                                HashMap<String, Object> userMap = new HashMap<>();
                                userMap.put("userType", "Dog Owner");
                                userMap.put("name", "Test User");

                                databaseRef.child(userId).updateChildren(userMap)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Test user data saved successfully for UID: " + userId);
                                            Toast.makeText(WelcomeActivity.this, "Logged in as Test User (Dog Owner)", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to save test user data: " + e.getMessage(), e);
                                            Toast.makeText(WelcomeActivity.this, "Failed to save test user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            } else {
                                Log.e(TAG, "Current user is null after successful sign-in");
                                Toast.makeText(WelcomeActivity.this, "Test user login failed: User not found", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Log.e(TAG, "Test account sign-in failed: " + errorMessage, task.getException());
                            Toast.makeText(WelcomeActivity.this, "Test user login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}