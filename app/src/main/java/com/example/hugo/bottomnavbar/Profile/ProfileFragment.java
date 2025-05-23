package com.example.hugo.bottomnavbar.Profile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hugo.MainActivity;
import com.example.hugo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private ShapeableImageView profileImage;
    private TextView profileName, profileBio, profileLocation, availabilityText;
    private Button editProfileButton;
    private LinearLayout myDogsSection, myBookingsSection, myOrdersSection;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private ActivityResultLauncher<Intent> locationPickerLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private BottomNavigationView bottomNavigationView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String currentUsername, currentBio, currentLocationName, userType;
    private double currentLatitude, currentLongitude;
    private Map<String, List<String>> currentAvailability;
    private EditProfileDialog currentDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }
        databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

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
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        locationPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                double latitude = data.getDoubleExtra("latitude", 0.0);
                double longitude = data.getDoubleExtra("longitude", 0.0);
                String locationName = data.getStringExtra("locationName");
                currentLatitude = latitude;
                currentLongitude = longitude;
                currentLocationName = locationName;
                if (currentDialog != null) {
                    currentDialog.updateLocation(latitude, longitude);
                }
                if (profileLocation != null) {
                    profileLocation.setText(currentLocationName);
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);
            if (fineLocationGranted || coarseLocationGranted) {
                Intent intent = new Intent(requireContext(), MapLocationActivity.class);
                locationPickerLauncher.launch(intent);
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
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
        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileBio = view.findViewById(R.id.profile_bio);
        profileLocation = view.findViewById(R.id.profile_location);
        availabilityText = view.findViewById(R.id.availability_text);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        myDogsSection = view.findViewById(R.id.my_dogs_section);
        myBookingsSection = view.findViewById(R.id.my_bookings_section);
        myOrdersSection = view.findViewById(R.id.my_orders_section);

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        myBookingsSection.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MyBookingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        myOrdersSection.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MyOrdersFragment())
                    .addToBackStack(null)
                    .commit();
        });

        if (profileImage != null) {
            profileImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                profileImagePickerLauncher.launch(intent);
            });
        }

        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(v -> showEditProfileDialog());
        }

        if (myDogsSection != null) {
            myDogsSection.setOnClickListener(v -> {
                if (getActivity() == null || !isAdded() || isDetached()) {
                    Toast.makeText(getContext(), "Navigation error: Fragment or activity not ready", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showLoadingIndicator();
                        ((MainActivity) getActivity()).hideBottomNavigationBar();
                    }
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    if (fragmentManager.isStateSaved()) {
                        Toast.makeText(getContext(), "Cannot navigate: App state is saved", Toast.LENGTH_SHORT).show();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).hideLoadingIndicator();
                        }
                        return;
                    }
                    DogFragment dogFragment = new DogFragment();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.fragment_container, dogFragment);
                    transaction.addToBackStack("DogFragment");
                    transaction.commit();
                    // Pass callback to hide loading indicator
                    dogFragment.setLoadingCallback(() -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).hideLoadingIndicator();
                            ((MainActivity) getActivity()).showBottomNavigationBar();
                        }
                    });
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error navigating to My Dogs: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).hideLoadingIndicator();
                    }
                }
            });
        }

        loadUserProfile();
    }

    private void showEditProfileDialog() {
        if (currentUsername == null) currentUsername = profileName != null ? profileName.getText().toString() : "";
        if (currentBio == null) currentBio = profileBio != null ? profileBio.getText().toString() : "";
        if (currentLocationName == null) currentLocationName = profileLocation != null ? profileLocation.getText().toString() : "";
        if (currentAvailability == null) currentAvailability = new HashMap<>();
        if (userType == null) userType = "Dog Owner";

        EditProfileDialog.OnProfileUpdateListener updateListener = (username, bio, locationName, latitude, longitude, availability) -> {
            this.currentUsername = username;
            this.currentBio = bio;
            this.currentLocationName = locationName;
            this.currentLatitude = latitude;
            this.currentLongitude = longitude;
            this.currentAvailability = availability;
            if (profileName != null) {
                profileName.setText(username);
            }
            if (profileBio != null) {
                profileBio.setText(bio);
            }
            if (profileLocation != null) {
                profileLocation.setText(locationName);
            }
            if (availabilityText != null) {
                if (availability != null && !availability.isEmpty()) {
                    availabilityText.setText("Availability: " + availability.toString());
                    availabilityText.setVisibility(View.VISIBLE);
                } else {
                    availabilityText.setVisibility(View.GONE);
                }
            }
        };

        EditProfileDialog.OnSelectLocationListener selectLocationListener = () -> {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(requireContext(), MapLocationActivity.class);
                locationPickerLauncher.launch(intent);
            } else {
                permissionLauncher.launch(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        };

        currentDialog = new EditProfileDialog(
                requireContext(),
                currentUsername,
                currentBio,
                currentLocationName,
                currentLatitude,
                currentLongitude,
                userType,
                currentAvailability,
                updateListener,
                selectLocationListener
        );
        currentDialog.show();
    }

    private void loadUserProfile() {
        if (databaseRef == null) {
            return;
        }
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    String username = snapshot.child("name").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);
                    String location = snapshot.child("locationName").getValue(String.class);
                    String base64Image = snapshot.child("profileImageBase64").getValue(String.class);
                    GenericTypeIndicator<Map<String, List<String>>> availabilityType =
                            new GenericTypeIndicator<Map<String, List<String>>>() {};
                    Map<String, List<String>> availability = snapshot.child("availability").getValue(availabilityType);
                    String userTypeFromDB = snapshot.child("userType").getValue(String.class);
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);

                    currentUsername = username != null ? username : "Unknown";
                    currentBio = bio != null ? bio : "No bio set";
                    currentLocationName = location != null ? location : "No location set";
                    currentAvailability = availability != null ? availability : new HashMap<>();
                    userType = userTypeFromDB != null ? userTypeFromDB : "Dog Owner";
                    currentLatitude = latitude != null ? latitude : 0.0;
                    currentLongitude = longitude != null ? longitude : 0.0;

                    if (profileName != null) {
                        profileName.setText(currentUsername);
                    }
                    if (profileBio != null) {
                        profileBio.setText(currentBio);
                    }
                    if (profileLocation != null) {
                        profileLocation.setText(currentLocationName);
                    }
                    if (availabilityText != null) {
                        if (currentAvailability != null && !currentAvailability.isEmpty()) {
                            availabilityText.setText("Availability: " + currentAvailability.toString());
                            availabilityText.setVisibility(View.VISIBLE);
                        } else {
                            availabilityText.setVisibility(View.GONE);
                        }
                    }
                    if (profileImage != null) {
                        if (base64Image != null && !base64Image.isEmpty()) {
                            Bitmap bitmap = base64ToBitmap(base64Image);
                            if (bitmap != null) {
                                profileImage.setImageBitmap(bitmap);
                            } else {
                                profileImage.setImageResource(R.drawable.ic_profile);
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile);
                        }
                    }
                    if (myOrdersSection != null && userType != null &&
                            (userType.equalsIgnoreCase("Dog Walker") ||
                                    userType.equalsIgnoreCase("Trainer") ||
                                    userType.equalsIgnoreCase("Veterinarian"))) {
                        myOrdersSection.setVisibility(View.VISIBLE);
                    } else {
                        myOrdersSection.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Database error", Toast.LENGTH_SHORT).show();
            }
        });
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
            return null;
        }
    }

    private void saveUserProfile(String username, String bio, String locationName, double latitude, double longitude, Map<String, List<String>> availability) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", username);
        userData.put("bio", bio);
        userData.put("locationName", locationName);
        userData.put("latitude", latitude);
        userData.put("longitude", longitude);
        userData.put("availability", availability);
        userData.put("userType", userType);
        databaseRef.updateChildren(userData)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> {});
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}