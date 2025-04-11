package com.example.hugo;

<<<<<<< HEAD
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SignInActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private RadioButton selectedRadioButton;
    private Button signInButton;
    private TextView questionText;
    private FrameLayout fragmentContainer;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        radioGroup = findViewById(R.id.radioGroup);
        signInButton = findViewById(R.id.btn_signIn);
        questionText = findViewById(R.id.questionText);
        fragmentContainer = findViewById(R.id.fragment_container);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = radioGroup.getCheckedRadioButtonId();

                if (selectedId != -1) {
                    selectedRadioButton = findViewById(selectedId);
                    String userType = "";

                    if (selectedRadioButton.getId() == R.id.radioOwner) {
                        Toast.makeText(SignInActivity.this, "You selected: Dog Owner", Toast.LENGTH_SHORT).show();
                        userType = "Dog Owner";
                        questionText.setVisibility(View.GONE);
                        radioGroup.setVisibility(View.GONE);
                        signInButton.setVisibility(View.GONE);

                        fragmentContainer.setVisibility(View.VISIBLE);

                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new DogOwnerDetailsFragmentOne())
                                .addToBackStack(null)
                                .commit();
                    } else if (selectedRadioButton.getId() == R.id.radioWalker) {
                        Toast.makeText(SignInActivity.this, "You selected: Dog Walker", Toast.LENGTH_SHORT).show();
                        userType = "Dog Walker";
                        questionText.setVisibility(View.GONE);
                        radioGroup.setVisibility(View.GONE);
                        signInButton.setVisibility(View.GONE);

                        fragmentContainer.setVisibility(View.VISIBLE);


                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new DogWalkerDetailsFragment())
                                .addToBackStack(null)
                                .commit();
                    } else if (selectedRadioButton.getId() == R.id.radioTrainer) {
                        Toast.makeText(SignInActivity.this, "You selected: Dog Trainer / Veterinarian", Toast.LENGTH_SHORT).show();
                        userType = "Trainer/Veterinarian";
                        questionText.setVisibility(View.GONE);
                        radioGroup.setVisibility(View.GONE);
                        signInButton.setVisibility(View.GONE);

                        fragmentContainer.setVisibility(View.VISIBLE);


                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new TrainerDetailsFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                    saveUserTypeToFirebase(userType);
                } else {
                    Toast.makeText(SignInActivity.this, "Please select an option", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void saveUserTypeToFirebase(String userType) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("userType", userType);

            databaseRef.child(userId).updateChildren(userMap)
                    .addOnSuccessListener(aVoid -> Toast.makeText(SignInActivity.this, "User type saved!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(SignInActivity.this, "Failed to save user type.", Toast.LENGTH_SHORT).show());
        }
    }
=======
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
>>>>>>> 5cbdb4be63784f0f808166bfbf4aacc506e0590b
}