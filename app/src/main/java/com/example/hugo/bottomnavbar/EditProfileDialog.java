package com.example.hugo.bottomnavbar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.hugo.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

        // Set up save button listener
        saveButton.setOnClickListener(v -> {
            String newUsername = usernameInput.getText().toString();
            String newBio = bioInput.getText().toString();
            String newLocation = locationInput.getText().toString();

            // Notify listener of updates
            listener.onProfileUpdated(newUsername, newBio, newLocation);

            dismiss(); // Close the dialog
        });
    }

    // Listener interface to notify profile updates
    public interface OnProfileUpdateListener {
        void onProfileUpdated(String username, String bio, String location);
    }
}
