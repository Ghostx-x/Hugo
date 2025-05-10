package com.example.hugo.bottomnavbar.Profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ImageView profileImage;
    private TextView usernameText, bioText, locationText, availabilityText;
    private Button editProfileButton;
    private LinearLayout myDogsSection;
    private RecyclerView dogRecyclerView;
    private ProgressBar profileLoadingIndicator;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private DatabaseReference dogsRef;
    private ActivityResultLauncher<Intent> mapLocationLauncher;
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
                    if (profileImage != null) {
                        profileImage.setImageBitmap(bitmap);
                    }
                    uploadProfileImageToFirebase(imageUri);
                    Log.d(TAG, "Profile image selected: " + imageUri);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load profile image: " + e.getMessage(), e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mapLocationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Location selection canceled", Toast.LENGTH_SHORT).show();
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
        try {
            profileImage = view.findViewById(R.id.profile_image);
            usernameText = view.findViewById(R.id.profile_name);
            bioText = view.findViewById(R.id.profile_bio);
            locationText = view.findViewById(R.id.profile_location);
            availabilityText = view.findViewById(R.id.availability_text);
            editProfileButton = view.findViewById(R.id.edit_profile_button);
            myDogsSection = view.findViewById(R.id.my_dogs_section);
            dogRecyclerView = view.findViewById(R.id.dog_recycler_view);
            profileLoadingIndicator = view.findViewById(R.id.profile_loading_indicator);

            // Log specific null views
            StringBuilder nullViews = new StringBuilder();
            if (profileImage == null) nullViews.append("profile_image, ");
            if (usernameText == null) nullViews.append("profile_name, ");
            if (bioText == null) nullViews.append("profile_bio, ");
            if (locationText == null) nullViews.append("profile_location, ");
            if (availabilityText == null) nullViews.append("availability_text, ");
            if (editProfileButton == null) nullViews.append("edit_profile_button, ");
            if (myDogsSection == null) nullViews.append("my_dogs_section, ");
            if (dogRecyclerView == null) nullViews.append("dog_recycler_view, ");
            if (profileLoadingIndicator == null) nullViews.append("profile_loading_indicator, ");

            if (nullViews.length() > 0) {
                Log.e(TAG, "Missing views: " + nullViews.toString());
                Toast.makeText(getContext(), "Error initializing profile: Missing views", Toast.LENGTH_SHORT).show();
                if (usernameText != null && profileImage != null) {
                    Log.d(TAG, "Proceeding with partial UI");
                } else {
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize views: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error initializing profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        sharedPreferences = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE);

        // Initialize RecyclerView for dogs
        dogList = new ArrayList<>();
        dogAdapter = new DogAdapter(dogList);
        dogRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        dogRecyclerView.setAdapter(dogAdapter);

        // Check Google Play Services in background
        executorService.submit(() -> {
            try {
                int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
                if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
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
        } else {
            Log.w(TAG, "Activity is null in onViewCreated");
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
                Log.d(TAG, "My Dogs section clicked");
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
        loadDogs();
    }

    private void loadDogs() {
        Log.d(TAG, "Loading dogs for UID: " + userId);
        showLoadingIndicator();
        new LoadDogsTask().execute();
    }

    private class LoadDogsTask extends AsyncTask<Void, Void, List<Dog>> {
        @Override
        protected List<Dog> doInBackground(Void... voids) {
            final List<Dog> dogs = new ArrayList<>();
            try {
                dogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        synchronized (dogs) {
                            for (DataSnapshot dogSnapshot : snapshot.getChildren()) {
                                String name = dogSnapshot.child("name").getValue(String.class);
                                String breed = dogSnapshot.child("breed").getValue(String.class);
                                String age = dogSnapshot.child("age").getValue(String.class);
                                String imageUrl = dogSnapshot.child("imageUrl").getValue(String.class);
                                if (name != null) {
                                    dogs.add(new Dog(name, breed, age, imageUrl));
                                }
                            }
                            Log.d(TAG, "Fetched " + dogs.size() + " dogs from Firebase");
                            dogs.notify();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        synchronized (dogs) {
                            Log.e(TAG, "Failed to load dogs: " + error.getMessage());
                            dogs.notify();
                        }
                    }
                });

                synchronized (dogs) {
                    dogs.wait(5000);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while loading dogs: " + e.getMessage(), e);
            }
            return dogs;
        }

        @Override
        protected void onPostExecute(List<Dog> dogs) {
            if (getActivity() == null) {
                Log.w(TAG, "Activity is null, cannot update UI");
                hideLoadingIndicator();
                return;
            }

            if (dogs.isEmpty()) {
                Toast.makeText(getContext(), "No dogs found", Toast.LENGTH_SHORT).show();
            } else {
                dogList.clear();
                dogList.addAll(dogs);
                dogAdapter.notifyDataSetChanged();
                Log.d(TAG, "Updated UI with " + dogs.size() + " dogs");
            }
            hideLoadingIndicator();
        }
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
                                if (usernameText != null) {
                                    usernameText.setText(newUsername);
                                }
                                if (bioText != null) {
                                    bioText.setText(newBio);
                                }
                                if (locationText != null) {
                                    locationText.setText(newLocationName);
                                }
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
                                mapLocationLauncher.launch(intent);
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

    private void uploadProfileImageToFirebase(Uri imageUri) {
        if (imageUri == null || userId == null) {
            Log.e(TAG, "Invalid imageUri or userId");
            Toast.makeText(getContext(), "Error uploading image", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoadingIndicator();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + userId + "_" + System.currentTimeMillis() + ".jpg");
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    databaseRef.child("profileImageUrl").setValue(downloadUrl)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Profile image URL saved: " + downloadUrl);
                                Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to save profile image URL: " + e.getMessage());
                                Toast.makeText(getContext(), "Failed to save image URL", Toast.LENGTH_SHORT).show();
                            });
                    hideLoadingIndicator();
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload profile image: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    hideLoadingIndicator();
                });
    }

    private void saveUserProfile(String username, String bio, String locationName, double latitude, double longitude, Map<String, List<String>> availability) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("bio", bio);
        editor.putString("locationName", locationName);
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("longitude", (float) longitude);
        editor.apply();
        Log.d(TAG, "User profile saved locally: " + username + ", " + locationName);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", username);
        updates.put("bio", bio);
        updates.put("locationName", locationName);
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        updates.put("userId", userId);
        updates.put("availability", availability != null ? availability : new HashMap<>());
        showLoadingIndicator();
        databaseRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile saved to Firebase");
                    hideLoadingIndicator();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save profile to Firebase: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to save profile", Toast.LENGTH_SHORT).show();
                    hideLoadingIndicator();
                });
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
        new LoadUserProfileTask().execute();
    }

    private class LoadUserProfileTask extends AsyncTask<Void, Void, Map<String, Object>> {
        @Override
        protected Map<String, Object> doInBackground(Void... voids) {
            final Map<String, Object> profileData = new HashMap<>();
            try {
                databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        synchronized (profileData) {
                            profileData.put("username", snapshot.child("name").getValue(String.class));
                            profileData.put("bio", snapshot.child("bio").getValue(String.class));
                            profileData.put("locationName", snapshot.child("locationName").getValue(String.class));
                            profileData.put("profileImageUrl", snapshot.child("profileImageUrl").getValue(String.class));
                            profileData.put("userType", snapshot.child("userType").getValue(String.class));
                            profileData.put("availability", snapshot.child("availability").getValue(Map.class));
                            profileData.put("ranking", snapshot.child("ranking").getValue(Double.class));
                            Log.d(TAG, "Profile data fetched in background");
                            profileData.notify();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        synchronized (profileData) {
                            Log.e(TAG, "Failed to load profile in background: " + error.getMessage());
                            profileData.put("error", error.getMessage());
                            profileData.notify();
                        }
                    }
                });

                synchronized (profileData) {
                    profileData.wait(5000);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while loading profile: " + e.getMessage(), e);
                profileData.put("error", e.getMessage());
            }
            return profileData;
        }

        @Override
        protected void onPostExecute(Map<String, Object> profileData) {
            if (getActivity() == null) {
                Log.w(TAG, "Activity is null, cannot update UI");
                hideLoadingIndicator();
                return;
            }

            if (profileData.containsKey("error")) {
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                hideLoadingIndicator();
                return;
            }

            String username = (String) profileData.get("username");
            String bio = (String) profileData.get("bio");
            String locationName = (String) profileData.get("locationName");
            String profileImageUrl = (String) profileData.get("profileImageUrl");
            String userType = (String) profileData.get("userType");
            Map<String, List<String>> availability = (Map<String, List<String>>) profileData.get("availability");
            Double ranking = (Double) profileData.get("ranking");

            if (usernameText != null) {
                usernameText.setText(username != null ? username : "Username");
            }
            if (bioText != null) {
                bioText.setText(bio != null ? bio : "Bio goes here...");
            }
            if (locationText != null) {
                locationText.setText(locationName != null ? locationName : "Location");
            }

            if (profileImage != null && profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Picasso.get()
                        .load(profileImageUrl)
                        .resize(200, 200)
                        .centerCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImage);
            } else if (profileImage != null) {
                profileImage.setImageResource(R.drawable.ic_profile);
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

    // Dog class for the RecyclerView
    public static class Dog {
        private String name;
        private String breed;
        private String age;
        private String imageUrl;

        public Dog(String name, String breed, String age, String imageUrl) {
            this.name = name;
            this.breed = breed;
            this.age = age;
            this.imageUrl = imageUrl;
        }

        public String getName() { return name; }
        public String getBreed() { return breed; }
        public String getAge() { return age; }
        public String getImageUrl() { return imageUrl; }
    }

    // DogAdapter for the RecyclerView
    public static class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {
        private List<Dog> dogList;

        public DogAdapter(List<Dog> dogList) {
            this.dogList = dogList;
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
            holder.ageText.setText(dog.getAge());
            if (dog.getImageUrl() != null && !dog.getImageUrl().isEmpty()) {
                Picasso.get()
                        .load(dog.getImageUrl())
                        .resize(100, 100)
                        .centerCrop()
                        .placeholder(R.drawable.ic_dog_placeholder)
                        .error(R.drawable.ic_dog_placeholder)
                        .into(holder.dogImage);
            } else {
                holder.dogImage.setImageResource(R.drawable.ic_dog_placeholder);
            }
        }

        @Override
        public int getItemCount() {
            return dogList.size();
        }

        static class DogViewHolder extends RecyclerView.ViewHolder {
            ImageView dogImage;
            TextView nameText, breedText, ageText;

            DogViewHolder(@NonNull View itemView) {
                super(itemView);
                dogImage = itemView.findViewById(R.id.dog_image);
                nameText = itemView.findViewById(R.id.dog_name);
                breedText = itemView.findViewById(R.id.dog_breed);
                ageText = itemView.findViewById(R.id.dog_age);
            }
        }
    }
}