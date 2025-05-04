package com.example.hugo.bottomnavbar.Profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE = 1;
    private static final String TAG = "ProfileFragment";
    private ImageView profileImage;
    private TextView usernameText, bioText, locationText;
    private Button editProfileButton;
    private SharedPreferences sharedPreferences;
    private Uri selectedImageUri;
    private BottomNavigationView bottomNavigationView;
    private EditProfileDialog editProfileDialog;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private ActivityResultLauncher<Intent> mapLocationLauncher;

    public ProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Check Google Play Services
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services error: " + resultCode);
            Toast.makeText(getContext(), "Google Play Services not available", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "Google Play Services available");

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        profileImage = view.findViewById(R.id.profile_image);
        usernameText = view.findViewById(R.id.username_text);
        bioText = view.findViewById(R.id.bio_text);
        locationText = view.findViewById(R.id.location_text);
        editProfileButton = view.findViewById(R.id.edit_profile_button);

        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);


        mapLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Map location result received: resultCode=" + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        double latitude = result.getData().getDoubleExtra("latitude", 0.0);
                        double longitude = result.getData().getDoubleExtra("longitude", 0.0);
                        if (editProfileDialog != null && editProfileDialog.isShowing()) {
                            editProfileDialog.updateLocation(latitude, longitude);
                            Log.d(TAG, "Location updated in dialog: lat=" + latitude + ", lng=" + longitude);
                        } else {
                            Log.w(TAG, "EditProfileDialog is null or not showing");
                        }
                    } else {
                        Log.d(TAG, "Map location selection canceled");
                        Toast.makeText(getContext(), "Location selection canceled", Toast.LENGTH_SHORT).show();
                    }
                });

        loadUserProfile();

        editProfileButton.setOnClickListener(v -> {
            Log.d(TAG, "Edit profile button clicked");
            openEditProfileDialog();
        });
        profileImage.setOnClickListener(v -> {
            Log.d(TAG, "Profile image clicked");
            selectProfileImage();
        });
    }

    private void openEditProfileDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found");
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Fetching user profile from Firebase for UID: " + user.getUid());
        databaseRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("name").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);
                String locationName = snapshot.child("locationName").getValue(String.class);
                Double latitude = snapshot.child("latitude").getValue(Double.class);
                Double longitude = snapshot.child("longitude").getValue(Double.class);

                Log.d(TAG, "Profile data loaded: username=" + username + ", location=" + locationName);

                editProfileDialog = new EditProfileDialog(
                        requireContext(),
                        username != null ? username : "",
                        bio != null ? bio : "",
                        locationName != null ? locationName : "",
                        latitude != null ? latitude : 0.0,
                        longitude != null ? longitude : 0.0,
                        (newUsername, newBio, newLocationName, newLatitude, newLongitude) -> {
                            usernameText.setText(newUsername);
                            bioText.setText(newBio);
                            locationText.setText(newLocationName);
                            saveUserProfile(newUsername, newBio, newLocationName, newLatitude, newLongitude);
                            Log.d(TAG, "Profile updated: " + newUsername + ", " + newLocationName);
                        },
                        () -> {
                            Log.d(TAG, "Launching MapLocationActivity");
                            Intent intent = new Intent(requireContext(), MapLocationActivity.class);
                            mapLocationLauncher.launch(intent);
                        }
                );

                editProfileDialog.show();
                Log.d(TAG, "EditProfileDialog shown");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load profile: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
        Log.d(TAG, "Image picker intent launched");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                profileImage.setImageBitmap(bitmap);
                uploadProfileImageToFirebase(selectedImageUri);
                Log.d(TAG, "Profile image selected: " + selectedImageUri);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load image: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadProfileImageToFirebase(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
            String base64Image = encodeImageToBase64(bitmap);
            saveBase64ImageToFirebase(base64Image);
        } catch (IOException e) {
            Log.e(TAG, "Failed to encode image: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to encode image", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    private void saveBase64ImageToFirebase(String base64Image) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            databaseRef.child(user.getUid()).child("profileImageBase64").setValue(base64Image)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile image updated successfully");
                        Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save image: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.w(TAG, "No authenticated user for image upload");
        }
    }


    public void uploadProfileImage(Uri imageUri, String userId) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile_images/" + userId + ".jpg");
        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                FirebaseDatabase.getInstance().getReference("Users")
                        .child(userId)
                        .child("profileImageUrl")
                        .setValue(downloadUrl);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private void saveUserProfile(String username, String bio, String locationName, double latitude, double longitude) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("bio", bio);
        editor.putString("locationName", locationName);
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("longitude", (float) longitude);
        editor.apply();
        Log.d(TAG, "User profile saved locally: " + username + ", " + locationName);
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found");
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading profile for UID: " + user.getUid());
        databaseRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("name").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);
                String locationName = snapshot.child("locationName").getValue(String.class);
                String base64Image = snapshot.child("profileImageBase64").getValue(String.class);

                usernameText.setText(username != null ? username : "Username");
                bioText.setText(bio != null ? bio : "Bio goes here...");
                locationText.setText(locationName != null ? locationName : "Location");

                if (base64Image != null) {
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    profileImage.setImageBitmap(decodedBitmap);
                }
                Log.d(TAG, "User profile loaded: username=" + username + ", location=" + locationName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load profile: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
    }
}