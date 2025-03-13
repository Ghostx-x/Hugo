package com.example.hugo;

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

public class SignInActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private RadioButton selectedRadioButton;
    private Button signInButton;
    private TextView questionText;
    private FrameLayout fragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

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

                    if (selectedRadioButton.getId() == R.id.radioOwner) {
                        Toast.makeText(SignInActivity.this, "You selected: Dog Owner", Toast.LENGTH_SHORT).show();

                        // Hide SignInActivity UI elements
                        questionText.setVisibility(View.GONE);
                        radioGroup.setVisibility(View.GONE);
                        signInButton.setVisibility(View.GONE);

                        // Show the fragment container
                        fragmentContainer.setVisibility(View.VISIBLE);

                        // Replace with DogOwnerDetailsFragmentOne
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new DogOwnerDetailsFragmentOne())
                                .addToBackStack(null)
                                .commit();
                    } else if (selectedRadioButton.getId() == R.id.radioWalker) {
                        Toast.makeText(SignInActivity.this, "You selected: Dog Walker", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignInActivity.this, DogWalkerActivity.class);
                        startActivity(intent);
                    } else if (selectedRadioButton.getId() == R.id.radioTrainer) {
                        Toast.makeText(SignInActivity.this, "You selected: Dog Trainer / Veterinarian", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignInActivity.this, DogTrainerActivity.class);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(SignInActivity.this, "Please select an option", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}