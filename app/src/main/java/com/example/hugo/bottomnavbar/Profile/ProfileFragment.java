package com.example.hugo.bottomnavbar.Profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.MainActivity;
import com.example.hugo.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ShapeableImageView profileImage;
    private TextView usernameText, bioText, locationText, availabilityText;
    private Button editProfileButton;
    private LinearLayout myDogsSection;
    private RecyclerView dogRecyclerView;
    private ProgressBar profileLoadingIndicator;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private DatabaseReference dogsRef;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private EditProfileDialog editProfileDialog;
    private String userId;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private List<Dog> dogList;
    private DogAdapter dogAdapter;

    public ProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user, redirecting to login");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(getContext(), com.example.hugo.LoginActivity.class);
                startActivity(loginIntent);
            }
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }
        userId = user.getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        dogsRef = databaseRef.child("dogs");

        profileImagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    int maxSize = 400;
                    bitmap = Bitmap.createScaledBitmap(bitmap, maxSize, maxSize, true);
                    if (profileImage != null) {
                        profileImage.setImageBitmap(bitmap);
                    }
                    String base64Image = bitmapToBase64(bitmap);
                    databaseRef.child("profileImageBase64").setValue(base64Image)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Profile image saved as Base64 to Firebase");
                                Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to save profile image Base64: " + e.getMessage());
                                Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                            });
                    Log.d(TAG, "Profile image selected and saved as Base64: " + imageUri);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load profile image: " + e.getMessage(), e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        usernameText = view.findViewById(R.id.profile_name);
        bioText = view.findViewById(R.id.profile_bio);
        locationText = view.findViewById(R.id.profile_location);
        availabilityText = view.findViewById(R.id.availability_text);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        myDogsSection = view.findViewById(R.id.my_dogs_section);
        dogRecyclerView = view.findViewById(R.id.dog_recycler_view);
        profileLoadingIndicator = view.findViewById(R.id.profile_loading_indicator);

        // Initialize RecyclerView for dogs (though not used here, kept for consistency)
        dogList = new ArrayList<>();
        dogAdapter = new DogAdapter(dogList, this::base64ToBitmap);
        dogRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        dogRecyclerView.setAdapter(dogAdapter);

        // Check Google Play Services
        executorService.submit(() -> {
            try {
                int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
                if (resultCode != ConnectionResult.SUCCESS) {
                    Log.e(TAG, "Google Play Services error: " + resultCode);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Google Play Services not available", Toast.LENGTH_LONG).show());
                    }
                    return;
                }
                Log.d(TAG, "Google Play Services available");
            } catch (Exception e) {
                Log.e(TAG, "Google Play Services check failed: " + e.getMessage(), e);
            }
        });

        // Initialize BottomNavigationView
        if (getActivity() != null) {
            BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNavigationView != null) {
                bottomNavigationView.setVisibility(View.VISIBLE);
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            } else {
                Log.w(TAG, "BottomNavigationView not found");
            }
        }

        // Set click listeners
        if (profileImage != null) {
            profileImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                profileImagePickerLauncher.launch(intent);
                Log.d(TAG, "Profile image clicked");
            });
        }

        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(v -> {
                Log.d(TAG, "Edit profile button clicked");
                openEditProfileDialog();
            });
        }

        if (myDogsSection != null) {
            myDogsSection.setOnClickListener(v -> {
                Log.d(TAG, "My Dogs section clicked, navigating to DogFragment");
                if (getActivity() == null) {
                    Log.e(TAG, "Activity is null, cannot navigate to DogFragment");
                    Toast.makeText(getContext(), "Navigation error: Activity not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showLoadingIndicator();
                    }
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new DogFragment())
                            .addToBackStack("DogFragment")
                            .commit();
                    Log.d(TAG, "Fragment transaction committed for DogFragment");
                    myDogsSection.postDelayed(() -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).hideLoadingIndicator();
                        }
                    }, 500);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to navigate to DogFragment: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Error navigating to My Dogs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).hideLoadingIndicator();
                    }
                }
            });
        }

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Log.d(TAG, "Loading profile for UID: " + user.getUid());
        showLoadingIndicator();
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getActivity() == null) {
                    Log.w(TAG, "Activity is null, cannot update UI");
                    hideLoadingIndicator();
                    return;
                }

                String username = snapshot.child("name").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);
                String locationName = snapshot.child("locationName").getValue(String.class);
                String base64Image = snapshot.child("profileImageBase64").getValue(String.class);
                String userType = snapshot.child("userType").getValue(String.class);
                Map<String, List<String>> availability = snapshot.child("availability").getValue(Map.class);
                Double ranking = snapshot.child("ranking").getValue(Double.class);

                if (usernameText != null) usernameText.setText(username != null ? username : "Username");
                if (bioText != null) bioText.setText(bio != null ? bio : "Bio goes here...");
                if (locationText != null) locationText.setText(locationName != null ? locationName : "Location");

                if (profileImage != null) {
                    if (base64Image != null && !base64Image.isEmpty()) {
                        Log.d(TAG, "Loading profile image from Base64");
                        Bitmap bitmap = base64ToBitmap(base64Image);
                        if (bitmap != null) {
                            profileImage.setImageBitmap(bitmap);
                        } else {
                            Log.w(TAG, "Failed to decode Base64 to Bitmap");
                            profileImage.setImageResource(R.drawable.ic_profile);
                        }
                    } else {
                        Log.w(TAG, "Profile image Base64 is null or empty");
                        profileImage.setImageResource(R.drawable.ic_profile);
                    }
                }

                if (availabilityText != null) {
                    if (isServiceProvider(userType) && availability != null && !availability.isEmpty()) {
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
                    } else {
                        availabilityText.setVisibility(View.GONE);
                    }
                }

                Log.d(TAG, "User profile UI updated: username=" + username + ", location=" + locationName + ", ranking=" + ranking);
                hideLoadingIndicator();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load profile: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                hideLoadingIndicator();
            }
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
        showLoadingIndicator();
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoadingIndicator();
                String username = snapshot.child("name").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);
                String locationName = snapshot.child("locationName").getValue(String.class);
                Double latitude = snapshot.child("latitude").getValue(Double.class);
                Double longitude = snapshot.child("longitude").getValue(Double.class);
                String userType = snapshot.child("userType").getValue(String.class);
                Map<String, List<String>> availability = snapshot.child("availability").getValue(Map.class);

                Log.d(TAG, "Profile data loaded: username=" + username + ", location=" + locationName);

                try {
                    editProfileDialog = new EditProfileDialog(
                            requireContext(),
                            username != null ? username : "",
                            bio != null ? bio : "",
                            locationName != null ? locationName : "",
                            latitude != null ? latitude : 0.0,
                            longitude != null ? longitude : 0.0,
                            userType != null ? userType : "",
                            availability != null ? availability : new HashMap<>(),
                            (newUsername, newBio, newLocationName, newLatitude, newLongitude, newAvailability) -> {
                                if (usernameText != null) usernameText.setText(newUsername);
                                if (bioText != null) bioText.setText(newBio);
                                if (locationText != null) locationText.setText(newLocationName);
                                if (availabilityText != null && newAvailability != null && !newAvailability.isEmpty()) {
                                    StringBuilder availabilityString = new StringBuilder();
                                    for (Map.Entry<String, List<String>> entry : newAvailability.entrySet()) {
                                        String day = entry.getKey();
                                        List<String> slots = entry.getValue();
                                        if (slots != null && !slots.isEmpty()) {
                                            availabilityString.append(day).append(": ").append(String.join(", ", slots)).append("\n");
                                        }
                                    }
                                    availabilityText.setText(availabilityString.length() > 0 ? availabilityString.toString() : "Availability: Not set");
                                    availabilityText.setVisibility(isServiceProvider(userType) ? View.VISIBLE : View.GONE);
                                }
                                saveUserProfile(newUsername, newBio, newLocationName, newLatitude, newLongitude, newAvailability);
                                Log.d(TAG, "Profile updated: username=" + newUsername + ", location=" + newLocationName);
                            },
                            () -> {
                                Log.d(TAG, "Launching MapLocationActivity");
                                Intent intent = new Intent(requireContext(), MapLocationActivity.class);
                                startActivity(intent);
                            }
                    );
                    editProfileDialog.show();
                    Log.d(TAG, "EditProfileDialog shown");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create EditProfileDialog: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Error opening edit profile dialog", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingIndicator();
                Log.e(TAG, "Failed to load profile: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile(String username, String bio, String locationName, double latitude, double longitude, Map<String, List<String>> availability) {
        // Implementation remains the same as before, omitted for brevity
    }

    private boolean isServiceProvider(String userType) {
        return userType != null && (
                userType.equalsIgnoreCase("Dog Walker") ||
                        userType.equalsIgnoreCase("Trainer") ||
                        userType.equalsIgnoreCase("Veterinarian")
        );
    }

    private void showLoadingIndicator() {
        if (profileLoadingIndicator != null) {
            profileLoadingIndicator.setVisibility(View.VISIBLE);
            Log.d(TAG, "Showing profile loading indicator");
        }
    }

    private void hideLoadingIndicator() {
        if (profileLoadingIndicator != null) {
            profileLoadingIndicator.setVisibility(View.GONE);
            Log.d(TAG, "Hiding profile loading indicator");
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        Log.d(TAG, "ProfileFragment destroyed");
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode Base64 to Bitmap: " + e.getMessage(), e);
            return null;
        }
    }

    public static class Dog {
        private String name;
        private String breed;
        private String gender;
        private String birthday;
        private String imageBase64;

        public Dog(String name, String breed, String gender, String birthday, String imageBase64) {
            this.name = name;
            this.breed = breed;
            this.gender = gender;
            this.birthday = birthday;
            this.imageBase64 = imageBase64;
        }

        public String getName() { return name; }
        public String getBreed() { return breed; }
        public String getGender() { return gender; }
        public String getBirthday() { return birthday; }
        public String getImageBase64() { return imageBase64; }
    }

    public static class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {
        private List<Dog> dogList;
        private java.util.function.Function<String, Bitmap> base64ToBitmapFunction;

        public DogAdapter(List<Dog> dogList, java.util.function.Function<String, Bitmap> base64ToBitmapFunction) {
            this.dogList = dogList;
            this.base64ToBitmapFunction = base64ToBitmapFunction;
        }

        @NonNull
        @Override
        public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dog, parent, false);
            return new DogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DogViewHolder holder, int position) {
            Dog dog = dogList.get(position);
            holder.nameText.setText(dog.getName());
            holder.breedText.setText(dog.getBreed());
            holder.genderText.setText(dog.getGender());
            holder.birthdayText.setText(dog.getBirthday());
            if (dog.getImageBase64() != null && !dog.getImageBase64().isEmpty()) {
                Bitmap bitmap = base64ToBitmapFunction.apply(dog.getImageBase64());
                if (bitmap != null) {
                    holder.dogImage.setImageBitmap(bitmap);
                } else {
                    holder.dogImage.setImageResource(R.drawable.ic_dog_placeholder);
                }
            } else {
                holder.dogImage.setImageResource(R.drawable.ic_dog_placeholder);
            }
        }

        @Override
        public int getItemCount() {
            return dogList.size();
        }

        static class DogViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView dogImage;
            TextView nameText, breedText, genderText, birthdayText;

            DogViewHolder(@NonNull View itemView) {
                super(itemView);
                dogImage = itemView.findViewById(R.id.dog_image);
                nameText = itemView.findViewById(R.id.dog_name);
                breedText = itemView.findViewById(R.id.dog_breed);
                genderText = itemView.findViewById(R.id.dog_gender);
                birthdayText = itemView.findViewById(R.id.dog_birthday);
                dogImage.setShapeAppearanceModel(
                        dogImage.getShapeAppearanceModel()
                                .toBuilder()
                                .setAllCorners(CornerFamily.ROUNDED, 50f)
                                .build());
            }
        }
    }
}