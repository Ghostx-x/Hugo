package com.example.hugo.bottomnavbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.EditProfileDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE = 1;
    private ImageView profileImage;
    private TextView usernameText, bioText, locationText;
    private Button editProfileButton;
    private SharedPreferences sharedPreferences;
    private Uri selectedImageUri;
    private BottomNavigationView bottomNavigationView;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        profileImage = view.findViewById(R.id.profile_image);
        usernameText = view.findViewById(R.id.username_text);
        bioText = view.findViewById(R.id.bio_text);
        locationText = view.findViewById(R.id.location_text);
        editProfileButton = view.findViewById(R.id.edit_profile_button);

        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        loadUserProfile();


        loadUserName();

        editProfileButton.setOnClickListener(v -> openEditProfileDialog());
        profileImage.setOnClickListener(v -> selectProfileImage());
    }

    private void openEditProfileDialog() {
        EditProfileDialog dialog = new EditProfileDialog(requireContext(),
                usernameText.getText().toString(),
                bioText.getText().toString(),
                locationText.getText().toString(),
                (newUsername, newBio, newLocation) -> {
                    usernameText.setText(newUsername);
                    bioText.setText(newBio);
                    locationText.setText(newLocation);
                    saveUserProfile(newUsername, newBio, newLocation);
                });

        dialog.show();
    }

    private void selectProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImageUri);
                profileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveUserProfile(String username, String bio, String location) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("bio", bio);
        editor.putString("location", location);
        editor.apply();
    }

    private void loadUserProfile() {
        usernameText.setText(sharedPreferences.getString("username", "Username"));
        bioText.setText(sharedPreferences.getString("bio", "Bio goes here..."));
        locationText.setText(sharedPreferences.getString("location", "Location"));
    }

    private void loadUserName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            databaseRef.child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userName = snapshot.getValue(String.class);
                        usernameText.setText(userName);
                    } else {
                        usernameText.setText("Username");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(requireContext(), "Failed to load username", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}