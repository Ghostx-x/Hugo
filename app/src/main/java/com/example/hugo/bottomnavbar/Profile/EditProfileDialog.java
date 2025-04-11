package com.example.hugo.bottomnavbar.Profile;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.hugo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import android.os.Bundle;
import android.widget.Toast;

public class EditProfileDialog extends Dialog {

    private EditText usernameInput, bioInput, locationInput;
    private Button saveButton;

    private String currentUsername, currentBio, currentLocation;
    private OnProfileUpdateListener listener;

    public EditProfileDialog(Context context, String currentUsername, String currentBio, String currentLocation, OnProfileUpdateListener listener) {
        super(context);
        this.currentUsername = currentUsername;
        this.currentBio = currentBio;
        this.currentLocation = currentLocation;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the custom dialog layout
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        setContentView(view);

        // Initialize views
        usernameInput = view.findViewById(R.id.username_input);
        bioInput = view.findViewById(R.id.bio_input);
        locationInput = view.findViewById(R.id.location_input);
        saveButton = view.findViewById(R.id.save_button);

        // Set current values (loaded from Firebase)
        usernameInput.setText(currentUsername);
        bioInput.setText(currentBio);
        locationInput.setText(currentLocation);

        saveButton.setOnClickListener(v -> {
            String newUsername = usernameInput.getText().toString();
            String newBio = bioInput.getText().toString();
            String newLocation = locationInput.getText().toString();

            // Save the updated username to Firebase
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                FirebaseDatabase.getInstance().getReference("Users").child(userId).child("name").setValue(newUsername)
                        .addOnSuccessListener(aVoid -> {
                            // Notify the listener to update the UI
                            listener.onProfileUpdated(newUsername, newBio, newLocation);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update username", Toast.LENGTH_SHORT).show();
                        });
            }

            dismiss(); // Close the dialog
        });

    }

    // Listener interface to notify profile updates
    public interface OnProfileUpdateListener {
        void onProfileUpdated(String username, String bio, String location);
    }
}
