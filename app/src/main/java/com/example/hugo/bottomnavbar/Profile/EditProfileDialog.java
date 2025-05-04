package com.example.hugo.bottomnavbar.Profile;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hugo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EditProfileDialog extends Dialog {

    private static final String TAG = "EditProfileDialog";
    private TextInputEditText usernameInput, bioInput;
    private Button selectLocationButton, saveButton;
    private TextView selectedLocationText;
    private String currentUsername, currentBio, currentLocationName;
    private double currentLatitude, currentLongitude;
    private OnProfileUpdateListener updateListener;
    private OnSelectLocationListener selectLocationListener;

    public EditProfileDialog(Context context, String currentUsername, String currentBio, String currentLocationName,
                             double currentLatitude, double currentLongitude, OnProfileUpdateListener updateListener,
                             OnSelectLocationListener selectLocationListener) {
        super(context, R.style.CustomDialog);
        this.currentUsername = currentUsername;
        this.currentBio = currentBio;
        this.currentLocationName = currentLocationName;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.updateListener = updateListener;
        this.selectLocationListener = selectLocationListener;
        Log.d(TAG, "EditProfileDialog created: username=" + currentUsername + ", location=" + currentLocationName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        setContentView(view);

        usernameInput = view.findViewById(R.id.username_input);
        bioInput = view.findViewById(R.id.bio_input);
        selectLocationButton = view.findViewById(R.id.select_location_button);
        selectedLocationText = view.findViewById(R.id.selected_location_text);
        saveButton = view.findViewById(R.id.save_button);

        usernameInput.setText(currentUsername);
        bioInput.setText(currentBio);
        selectedLocationText.setText(currentLocationName.isEmpty() ? "No location selected" : currentLocationName);

        selectLocationButton.setOnClickListener(v -> {
            Log.d(TAG, "Select location button clicked");
            if (selectLocationListener != null) {
                selectLocationListener.onSelectLocation();
            }
        });

        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");
            String newUsername = usernameInput.getText().toString().trim();
            String newBio = bioInput.getText().toString().trim();

            if (newUsername.isEmpty()) {
                Log.w(TAG, "Username is empty");
                Toast.makeText(getContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.w(TAG, "No authenticated user found");
                Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            userRef.child("name").setValue(newUsername);
            userRef.child("bio").setValue(newBio);
            userRef.child("locationName").setValue(currentLocationName);
            userRef.child("latitude").setValue(currentLatitude);
            userRef.child("longitude").setValue(currentLongitude)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile updated successfully: username=" + newUsername + ", location=" + currentLocationName);
                        updateListener.onProfileUpdated(newUsername, newBio, currentLocationName,
                                currentLatitude, currentLongitude);
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update profile: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    public void updateLocation(double latitude, double longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;

        String placeName = getPlaceName(latitude, longitude);
        this.currentLocationName = placeName != null ? placeName : "Lat: " + latitude + ", Lng: " + longitude;
        selectedLocationText.setText(currentLocationName);
        Log.d(TAG, "Location updated: name=" + currentLocationName + ", lat=" + latitude + ", lng=" + longitude);
    }

    private String getPlaceName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder addressString = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) addressString.append(", ");
                    addressString.append(address.getAddressLine(i));
                }
                return addressString.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed: " + e.getMessage(), e);
        }
        return null;
    }

    public interface OnProfileUpdateListener {
        void onProfileUpdated(String username, String bio, String locationName, double latitude, double longitude);
    }

    public interface OnSelectLocationListener {
        void onSelectLocation();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }
}