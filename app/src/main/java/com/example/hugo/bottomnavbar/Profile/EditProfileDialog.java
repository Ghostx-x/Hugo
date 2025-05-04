package com.example.hugo.bottomnavbar.Profile;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hugo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditProfileDialog extends Dialog {

    private static final String TAG = "EditProfileDialog";
    private TextInputEditText usernameInput, bioInput;
    private Button selectLocationButton, saveButton;
    private TextView selectedLocationText;
    private LinearLayout availabilityContainer;
    private String currentUsername, currentBio, currentLocationName, userType;
    private double currentLatitude, currentLongitude;
    private Map<String, List<String>> availability;
    private OnProfileUpdateListener updateListener;
    private OnSelectLocationListener selectLocationListener;
    private static final List<String> DAYS = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );

    public EditProfileDialog(Context context, String currentUsername, String currentBio, String currentLocationName,
                             double currentLatitude, double currentLongitude, String userType,
                             Map<String, List<String>> availability, OnProfileUpdateListener updateListener,
                             OnSelectLocationListener selectLocationListener) {
        super(context, R.style.CustomDialog);
        this.currentUsername = currentUsername;
        this.currentBio = currentBio;
        this.currentLocationName = currentLocationName;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.userType = userType;
        this.availability = availability != null ? new HashMap<>(availability) : new HashMap<>();
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
        availabilityContainer = view.findViewById(R.id.availability_container);

        usernameInput.setText(currentUsername);
        bioInput.setText(currentBio);
        selectedLocationText.setText(currentLocationName.isEmpty() ? "No location selected" : currentLocationName);

        // Setup availability UI for service providers
        if (isServiceProvider(userType)) {
            setupAvailabilityUI();
            availabilityContainer.setVisibility(View.VISIBLE);
        } else {
            availabilityContainer.setVisibility(View.GONE);
        }

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

            // Use the class-level availability map
            Map<String, List<String>> newAvailability = isServiceProvider(userType) ? availability : new HashMap<>();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.w(TAG, "No authenticated user found");
                Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newUsername);
            updates.put("bio", newBio);
            updates.put("locationName", currentLocationName);
            updates.put("latitude", currentLatitude);
            updates.put("longitude", currentLongitude);
            updates.put("availability", newAvailability);

            userRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile updated successfully: username=" + newUsername + ", availability=" + newAvailability);
                        updateListener.onProfileUpdated(newUsername, newBio, currentLocationName,
                                currentLatitude, currentLongitude, newAvailability);
                        Toast.makeText(getContext(), "Profile saved", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update profile: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private boolean isServiceProvider(String userType) {
        return userType != null && (
                userType.equalsIgnoreCase("Dog Walker") ||
                        userType.equalsIgnoreCase("Trainer") ||
                        userType.equalsIgnoreCase("Veterinarian")
        );
    }

    private void setupAvailabilityUI() {
        availabilityContainer.removeAllViews();
        for (String day : DAYS) {
            View dayView = LayoutInflater.from(getContext()).inflate(R.layout.item_availability_day, null);
            TextView dayText = dayView.findViewById(R.id.day_text);
            Button addSlotButton = dayView.findViewById(R.id.add_slot_button);
            TextView slotsText = dayView.findViewById(R.id.slots_text);

            dayText.setText(day);
            List<String> slots = availability.get(day);
            slotsText.setText(slots != null && !slots.isEmpty() ? String.join(", ", slots) : "No slots");
            addSlotButton.setOnClickListener(v -> showTimeSlotDialog(day, slotsText));
            availabilityContainer.addView(dayView);
        }
    }

    private void showTimeSlotDialog(String day, TextView slotsText) {
        // Start time picker
        TimePickerDialog startTimePicker = new TimePickerDialog(
                getContext(),
                (view, hour, minute) -> {
                    String startTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    // End time picker
                    TimePickerDialog endTimePicker = new TimePickerDialog(
                            getContext(),
                            (endView, endHour, endMinute) -> {
                                String endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute);
                                String timeSlot = startTime + "-" + endTime;
                                List<String> currentSlots = availability.computeIfAbsent(day, k -> new ArrayList<>());
                                currentSlots.add(timeSlot);
                                slotsText.setText(String.join(", ", currentSlots));
                                Toast.makeText(getContext(), "Added slot: " + timeSlot + " for " + day, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Added slot: " + timeSlot + " for " + day);
                            },
                            hour, minute, true);
                    endTimePicker.setTitle("Select End Time");
                    endTimePicker.show();
                },
                9, 0, true);
        startTimePicker.setTitle("Select Start Time");
        startTimePicker.show();
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
        void onProfileUpdated(String username, String bio, String locationName, double latitude, double longitude, Map<String, List<String>> availability);
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