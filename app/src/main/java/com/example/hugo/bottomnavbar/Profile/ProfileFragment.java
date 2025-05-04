package com.example.hugo.bottomnavbar.Profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment implements EditProfileDialog.OnProfileUpdateListener {

    private static final String TAG = "ProfileFragment";
    private ShapeableImageView profileImage;
    private TextView profileName, profileBio, profileLocation, availabilityText;
    private Button editProfileButton, dogPictureButton, saveDogButton;
    private TextInputEditText dogNameInput, dogBirthdayInput, dogBreedInput, dogSpecialCareInput;
    private LinearLayout dogFormContainer;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private String userId;
    private String dogImageBase64;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user");
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }
        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();
                    dogImageBase64 = Base64.encodeToString(data, Base64.DEFAULT);
                    profileImage.setImageBitmap(bitmap); // Preview
                    Log.d(TAG, "Dog image selected and encoded");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load dog image: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Initialize UI
        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileBio = view.findViewById(R.id.profile_bio);
        profileLocation = view.findViewById(R.id.profile_location);
        availabilityText = view.findViewById(R.id.availability_text);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        dogFormContainer = view.findViewById(R.id.dog_form_container);
        dogNameInput = view.findViewById(R.id.dog_name_input);
        dogBirthdayInput = view.findViewById(R.id.dog_birthday_input);
        dogBreedInput = view.findViewById(R.id.dog_breed_input);
        dogPictureButton = view.findViewById(R.id.dog_picture_button);
        dogSpecialCareInput = view.findViewById(R.id.dog_special_care_input);
        saveDogButton = view.findViewById(R.id.save_dog_button);

        // Load user data
        loadUserData();

        // Edit profile
        editProfileButton.setOnClickListener(v -> {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        EditProfileDialog dialog = new EditProfileDialog(
                                getContext(),
                                user.name != null ? user.name : "",
                                user.bio != null ? user.bio : "",
                                user.locationName != null ? user.locationName : "",
                                user.latitude,
                                user.longitude,
                                user.userType != null ? user.userType : "",
                                user.availability,
                                ProfileFragment.this,
                                () -> {
                                    // Launch location picker (implement as needed)
                                    Toast.makeText(getContext(), "Location picker not implemented", Toast.LENGTH_SHORT).show();
                                }
                        );
                        dialog.show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user data: " + error.getMessage());
                }
            });
        });

        // Dog birthday picker
        dogBirthdayInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(
                    getContext(),
                    (view1, year, month, day) -> {
                        String date = String.format(Locale.US, "%02d/%02d/%04d", month + 1, day, year);
                        dogBirthdayInput.setText(date);
                        Log.d(TAG, "Dog birthday selected: " + date);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });

        // Dog picture picker
        dogPictureButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Save dog info
        saveDogButton.setOnClickListener(v -> saveDogInfo());
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    Log.w(TAG, "User not found");
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Display user info
                profileName.setText(user.name != null ? user.name : "No Name");
                profileBio.setText(user.bio != null ? user.bio : "No bio");
                profileLocation.setText(user.locationName != null && !user.locationName.isEmpty() ? user.locationName : "No location");

                if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                    Picasso.get()
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile);
                }

                // Display availability for service providers
                if (isServiceProvider(user.userType) && user.availability != null && !user.availability.isEmpty()) {
                    StringBuilder availabilityString = new StringBuilder();
                    for (Map.Entry<String, List<String>> entry : user.availability.entrySet()) {
                        String day = entry.getKey();
                        List<String> slots = entry.getValue();
                        if (slots != null && !slots.isEmpty()) {
                            availabilityString.append(day).append(": ").append(String.join(", ", slots)).append("\n");
                        }
                    }
                    availabilityText.setText(availabilityString.length() > 0 ? availabilityString.toString() : "Availability: Not set");
                    availabilityText.setVisibility(View.VISIBLE);
                    dogFormContainer.setVisibility(View.GONE);
                } else {
                    availabilityText.setVisibility(View.GONE);
                    // Show dog form for dog owners
                    if (user.userType != null && user.userType.equalsIgnoreCase("Dog Owner")) {
                        dogFormContainer.setVisibility(View.VISIBLE);
                        if (user.dog != null) {
                            dogNameInput.setText(user.dog.name != null ? user.dog.name : "");
                            dogBirthdayInput.setText(user.dog.birthday != null ? user.dog.birthday : "");
                            dogBreedInput.setText(user.dog.breed != null ? user.dog.breed : "");
                            dogSpecialCareInput.setText(user.dog.specialCare != null ? user.dog.specialCare : "");
                        }
                    } else {
                        dogFormContainer.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user data: " + error.getMessage());
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDogInfo() {
        String dogName = dogNameInput.getText().toString().trim();
        String dogBirthday = dogBirthdayInput.getText().toString().trim();
        String dogBreed = dogBreedInput.getText().toString().trim();
        String dogSpecialCare = dogSpecialCareInput.getText().toString().trim();

        if (dogName.isEmpty() || dogBirthday.isEmpty() || dogBreed.isEmpty()) {
            Log.w(TAG, "Dog name, birthday, or breed is empty");
            Toast.makeText(getContext(), "Please fill in name, birthday, and breed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate age from birthday
        int dogAge = calculateAge(dogBirthday);
        if (dogAge < 0) {
            Log.w(TAG, "Invalid birthday: " + dogBirthday);
            Toast.makeText(getContext(), "Invalid birthday format (MM/DD/YYYY)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload dog image if selected
        if (dogImageBase64 != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("dog_images/" + userId + "_" + System.currentTimeMillis() + ".jpg");
            storageRef.putBytes(Base64.decode(dogImageBase64, Base64.DEFAULT))
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveDogToFirebase(dogName, dogBreed, dogAge, uri.toString(), dogBirthday, dogSpecialCare);
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload dog image: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Use existing image or null
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    String existingImageUrl = user != null && user.dog != null ? user.dog.profileImageUrl : null;
                    saveDogToFirebase(dogName, dogBreed, dogAge, existingImageUrl, dogBirthday, dogSpecialCare);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user data: " + error.getMessage());
                }
            });
        }
    }

    private void saveDogToFirebase(String name, String breed, int age, String imageUrl, String birthday, String specialCare) {
        User.Dog dog = new User.Dog(name, breed, age, imageUrl, birthday, specialCare);
        userRef.child("dog").setValue(dog)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dog info saved: name=" + name);
                    Toast.makeText(getContext(), "Dog info saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save dog info: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to save dog info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private int calculateAge(String birthday) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            Calendar birthDate = Calendar.getInstance();
            birthDate.setTime(sdf.parse(birthday));
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse birthday: " + birthday, e);
            return -1;
        }
    }

    private boolean isServiceProvider(String userType) {
        return userType != null && (
                userType.equalsIgnoreCase("Dog Walker") ||
                        userType.equalsIgnoreCase("Trainer") ||
                        userType.equalsIgnoreCase("Veterinarian")
        );
    }

    @Override
    public void onProfileUpdated(String username, String bio, String locationName, double latitude, double longitude,
                                 Map<String, List<String>> availability) {
        profileName.setText(username);
        profileBio.setText(bio);
        profileLocation.setText(locationName.isEmpty() ? "No location" : locationName);
        if (availability != null && !availability.isEmpty()) {
            StringBuilder availabilityString = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : availability.entrySet()) {
                String day = entry.getKey();
                List<String> slots = entry.getValue();
                if (slots != null && !slots.isEmpty()) {
                    availabilityString.append(day).append(": ").append(String.join(", ", slots)).append("\n");
                }
            }
            availabilityText.setText(availabilityString.length() > 0 ? availabilityString.toString() : "Availability: Not set");
            availabilityText.setVisibility(View.VISIBLE);
            dogFormContainer.setVisibility(View.GONE);
        } else {
            availabilityText.setVisibility(View.GONE);
            if (currentUser != null) {
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.userType != null && user.userType.equalsIgnoreCase("Dog Owner")) {
                            dogFormContainer.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load user data: " + error.getMessage());
                    }
                });
            }
        }
    }
}