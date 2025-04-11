package com.example.hugo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button signInButton = findViewById(R.id.btn_signIn);
        Button loginButton = findViewById(R.id.btn_login);


        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
<<<<<<< HEAD
                Intent signInIntent = new Intent(WelcomeActivity.this, SignInFirst.class);
=======
                // Go to SignInActivity
                Intent signInIntent = new Intent(WelcomeActivity.this, SignInActivity.class);
>>>>>>> 5cbdb4be63784f0f808166bfbf4aacc506e0590b
                startActivity(signInIntent);
            }
        });


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent loginIntent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });
    }
}
