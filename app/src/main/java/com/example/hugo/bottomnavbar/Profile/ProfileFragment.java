package com.example.hugo.bottomnavbar.Profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private LinearLayout myDogsSection;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private BottomNavigationView bottomNavigationView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user in onCreate");
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
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Profile image saved as Base64");
                                Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to save profile image: " + e.getMessage(), e);
                                Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                            });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load profile image: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated started");

        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileBio = view.findViewById(R.id.profile_bio);
        profileLocation = view.findViewById(R.id.profile_location);
        availabilityText = view.findViewById(R.id.availability_text);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        myDogsSection = view.findViewById(R.id.my_dogs_section);


        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        LinearLayout myBookingsSection = view.findViewById(R.id.my_bookings_section);
        myBookingsSection.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MyBookingsFragment())
                    .addToBackStack(null)
                    .commit();
        });


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
                Toast.makeText(getContext(), "Edit profile not implemented", Toast.LENGTH_SHORT).show();
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
                if (!isAdded() || isDetached()) {
                    Log.e(TAG, "Fragment is not attached or is detached, cannot navigate");
                    Toast.makeText(getContext(), "Navigation error: Fragment not attached", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Log.d(TAG, "Attempting to navigate to DogFragment");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showLoadingIndicator();
                        ((MainActivity) getActivity()).hideBottomNavigationBar();
                    }
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    if (fragmentManager.isStateSaved()) {
                        Log.e(TAG, "FragmentManager state is saved, cannot commit transaction");
                        Toast.makeText(getContext(), "Cannot navigate: App state is saved", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DogFragment dogFragment = new DogFragment();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.fragment_container, dogFragment);
                    transaction.addToBackStack("DogFragment");
                    transaction.commit();
                    Log.d(TAG, "Fragment transaction committed for DogFragment");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to navigate to DogFragment: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Error navigating to My Dogs: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).hideLoadingIndicator();
                    }
                } finally {
                    handler.postDelayed(() -> {
                        if (getActivity() instanceof MainActivity && getActivity() != null) {
                            ((MainActivity) getActivity()).hideLoadingIndicator();
                            Log.d(TAG, "Loading indicator hidden");
                        }
                    }, 3000);
                }
            });
        }

        loadUserProfile();
    }

    private void loadUserProfile() {
        if (databaseRef == null) {
            Log.e(TAG, "Database reference is null, cannot load profile");
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
                    String userType = snapshot.child("userType").getValue(String.class);

                    Log.d(TAG, "User profile loaded: username=" + username + ", bio=" + bio +
                            ", location=" + location + ", availability=" + availability +
                            ", userType=" + userType);

                    if (profileName != null) {
                        profileName.setText(username != null ? username : "Unknown");
                    }
                    if (profileBio != null) {
                        profileBio.setText(bio != null ? bio : "No bio set");
                    }
                    if (profileLocation != null) {
                        profileLocation.setText(location != null ? location : "No location set");
                    }
                    if (availabilityText != null) {
                        if (availability != null && !availability.isEmpty()) {
                            availabilityText.setText("Availability: " + availability.toString());
                            availabilityText.setVisibility(View.VISIBLE);
                        } else {
                            availabilityText.setVisibility(View.GONE);
                        }
                    }
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
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load user profile: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
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
            Log.e(TAG, "Failed to decode Base64 to Bitmap: " + e.getMessage(), e);
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
        userData.put("userType", "Dog Owner");
        databaseRef.updateChildren(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save user profile: " + e.getMessage(), e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "onDestroyView called");
    }
}