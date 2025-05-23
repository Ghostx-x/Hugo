package com.example.hugo.bottomnavbar.Search;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Home.ConversationFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ViewProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String TAG = "ViewProfileFragment";
    private ImageView profileImage, dogImage;
    private TextView profileName, profileBio, profileUserType, profileRanking, profileAvailability;
    private TextView dogName, dogBreed, dogAge;
    private CardView dogInfoCard;
    private RecyclerView reviewsRecyclerView;
    private Button chatButton, bookButton;
    private ReviewAdapter reviewAdapter;
    private DatabaseReference userRef;
    private BottomNavigationView bottomNavigationView;

    public static ViewProfileFragment newInstance(String userId) {
        ViewProfileFragment fragment = new ViewProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        try {
            profileImage = view.findViewById(R.id.profile_image);
            profileName = view.findViewById(R.id.profile_name);
            profileBio = view.findViewById(R.id.profile_bio);
            profileUserType = view.findViewById(R.id.profile_user_type);
            profileRanking = view.findViewById(R.id.profile_ranking);
            profileAvailability = view.findViewById(R.id.profile_availability);
            dogInfoCard = view.findViewById(R.id.dog_info_card);
            dogImage = view.findViewById(R.id.dog_image);
            dogName = view.findViewById(R.id.dog_name);
            dogBreed = view.findViewById(R.id.dog_breed);
            dogAge = view.findViewById(R.id.dog_age);
            reviewsRecyclerView = view.findViewById(R.id.reviews_recycler_view);
            chatButton = view.findViewById(R.id.chat_button);
            bookButton = view.findViewById(R.id.book_button);

            if (profileAvailability == null) {
                Log.e(TAG, "profileAvailability TextView is null. Check R.id.profile_availability in fragment_view_profile.xml");
                Toast.makeText(getContext(), "UI error: Availability not found", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
                return;
            }

            bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNavigationView != null) {
                bottomNavigationView.setVisibility(View.VISIBLE);
            } else {
                Log.w(TAG, "BottomNavigationView not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI: " + e.getMessage(), e);
            Toast.makeText(getContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reviewAdapter = new ReviewAdapter(getContext(), new ArrayList<>());
        reviewsRecyclerView.setAdapter(reviewAdapter);

        String userId = getArguments() != null ? getArguments().getString(ARG_USER_ID) : null;
        if (userId == null) {
            Log.w(TAG, "Invalid user ID");
            Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        loadUserData();

        chatButton.setOnClickListener(v -> {
            String chatUserId = userId;
            String chatUserName = profileName.getText() != null ? profileName.getText().toString() : "Unknown";
            Log.d(TAG, "Chat button clicked: userId=" + chatUserId + ", userName=" + chatUserName);

            if (chatUserId == null || chatUserId.isEmpty()) {
                Log.w(TAG, "Cannot start chat: Invalid user ID");
                Toast.makeText(getContext(), "Cannot start chat: Invalid user", Toast.LENGTH_SHORT).show();
                return;
            }

            if (chatUserName.isEmpty() || chatUserName.equals("No Name")) {
                chatUserName = "Unknown User";
                Log.w(TAG, "Using fallback user name: " + chatUserName);
            }

            try {
                ConversationFragment conversationFragment = ConversationFragment.newInstance(chatUserId, chatUserName);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, conversationFragment)
                        .addToBackStack(null)
                        .commit();
                Log.d(TAG, "Navigating to ConversationFragment");
            } catch (Exception e) {
                Log.e(TAG, "Failed to navigate to ConversationFragment: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Failed to open chat", Toast.LENGTH_SHORT).show();
            }
        });

        bookButton.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, BookingFragment.newInstance(userId))
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "User data snapshot: " + snapshot.toString());
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    Log.w(TAG, "User not found");
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }

                // Load user profile data
                profileName.setText(user.name != null ? user.name : "No Name");
                profileBio.setText(user.bio != null ? user.bio : "No bio");
                profileUserType.setText(user.userType != null ? user.userType : "Unknown");
                profileRanking.setText(user.ranking != null && user.ranking > 0 ? String.format("Rating: %.1f/5", user.ranking) : "Rating: N/A");

                // Handle profile image (prefer Base64, fallback to URL if available)
                if (user.profileImageBase64 != null && !user.profileImageBase64.isEmpty()) {
                    loadImageFromBase64(profileImage, user.profileImageBase64, "Profile");
                } else if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                    loadProfileImageFromUrl(user.profileImageUrl);
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile);
                    Log.w(TAG, "No profile image found for user: " + user.name);
                }

                // Handle availability
                if (profileAvailability != null) {
                    if (isServiceProvider(user.userType) && user.availability != null && !user.availability.isEmpty()) {
                        StringBuilder availabilityString = new StringBuilder();
                        for (Map.Entry<String, List<String>> entry : user.availability.entrySet()) {
                            String day = entry.getKey();
                            List<String> slots = entry.getValue();
                            if (slots != null && !slots.isEmpty()) {
                                availabilityString.append(day).append(": ").append(String.join(", ", slots)).append("\n");
                            }
                        }
                        profileAvailability.setText(availabilityString.length() > 0 ? availabilityString.toString() : "Availability: Not set");
                        profileAvailability.setVisibility(View.VISIBLE);
                    } else {
                        profileAvailability.setVisibility(View.GONE);
                        Log.w(TAG, "Availability not displayed: Not a service provider or no slots");
                    }
                }

                // Load dog data (first valid dog)
                DataSnapshot dogsSnapshot = snapshot.child("dogs");
                Log.d(TAG, "Dog data snapshot: " + dogsSnapshot.toString());
                Log.d(TAG, "Does dogs node exist? " + dogsSnapshot.exists());
                Log.d(TAG, "Number of dogs: " + dogsSnapshot.getChildrenCount());
                User.Dog dog = null;
                for (DataSnapshot dogSnapshot : dogsSnapshot.getChildren()) {
                    Log.d(TAG, "Dog snapshot: " + dogSnapshot.toString());
                    try {
                        // Manually map fields to handle potential mismatches
                        dog = new User.Dog();
                        dog.name = dogSnapshot.child("name").getValue(String.class);
                        dog.breed = dogSnapshot.child("breed").getValue(String.class);
                        dog.age = dogSnapshot.child("age").getValue(String.class);
                        dog.profileImageUrl = dogSnapshot.child("profileImageUrl").getValue(String.class);
                        dog.imageBase64 = dogSnapshot.child("imageBase64").getValue(String.class);
                        dog.birthday = dogSnapshot.child("birthday").getValue(String.class);
                        if (dog.birthday == null) {
                            dog.birthday = dogSnapshot.child("birthDate").getValue(String.class); // Fallback to birthDate
                        }
                        dog.gender = dogSnapshot.child("gender").getValue(String.class);
                        dog.specialCare = dogSnapshot.child("specialCare").getValue(String.class);

                        Log.d(TAG, "Parsed dog: name=" + dog.name + ", breed=" + dog.breed + ", age=" + dog.age + ", birthday=" + dog.birthday);

                        // Convert age to int and calculate if needed
                        int parsedAge = -1;
                        if (dog.age != null) {
                            try {
                                parsedAge = Integer.parseInt(dog.age);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Failed to parse age as int: " + dog.age, e);
                                parsedAge = -1;
                            }
                        }
                        if (parsedAge <= 0 && dog.birthday != null) {
                            try {
                                parsedAge = calculateAge(dog.birthday);
                                Log.d(TAG, "Calculated age for dog " + dog.name + ": " + parsedAge);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to calculate dog age: " + e.getMessage(), e);
                                parsedAge = -1;
                            }
                        }

                        // Only proceed if dog has a name and valid data
                        if (dog.name != null && !dog.name.isEmpty()) {
                            dog.age = String.valueOf(parsedAge); // Store back as String for consistency
                            break; // Found a valid dog, stop processing
                        } else {
                            dog = null; // Invalid dog, continue to next
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse dog data: " + e.getMessage(), e);
                        dog = null; // Continue to next dog on failure
                    }
                }

                if (dog != null && dog.name != null) {
                    dogInfoCard.setVisibility(View.VISIBLE);
                    dogName.setText(dog.name);
                    dogBreed.setText(dog.breed != null ? dog.breed : "Unknown Breed");
                    int displayAge = -1;
                    try {
                        displayAge = Integer.parseInt(dog.age);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Failed to parse display age: " + dog.age, e);
                    }
                    dogAge.setText(displayAge >= 0 ? displayAge + " years" : "Unknown Age");
                    Log.d(TAG, "Setting dog info: Name=" + dog.name + ", Breed=" + dog.breed + ", Age=" + dogAge.getText());

                    // Handle dog image (prefer Base64, fallback to URL)
                    if (dog.imageBase64 != null && !dog.imageBase64.isEmpty()) {
                        loadImageFromBase64(dogImage, dog.imageBase64, "Dog");
                    } else if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
                        loadDogImageFromUrl(dog.profileImageUrl);
                    } else {
                        dogImage.setImageResource(R.drawable.ic_profile);
                        Log.w(TAG, "No image found for dog: " + dog.name);
                    }
                } else {
                    dogInfoCard.setVisibility(View.GONE);
                    Log.w(TAG, "No valid dog data found or parsing failed");
                }

                if (user.userType != null && user.userType.equalsIgnoreCase("Dog Owner")) {
                    bookButton.setVisibility(View.GONE);
                } else {
                    bookButton.setVisibility(View.VISIBLE);
                }

                loadReviews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user data: " + error.getMessage());
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void loadImageFromBase64(ImageView imageView, String base64, String imageType) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    Bitmap circularBitmap = getCircularBitmap(bitmap);
                    getActivity().runOnUiThread(() -> {
                        imageView.setImageBitmap(circularBitmap);
                        Log.d(TAG, imageType + " image loaded (Base64) for " + (imageType.equals("Dog") ? "dog: " + ((TextView) getView().findViewById(R.id.dog_name)).getText() : "user: " + profileName.getText()));
                    });
                } else {
                    getActivity().runOnUiThread(() -> {
                        imageView.setImageResource(R.drawable.ic_profile);
                        Log.w(TAG, "Failed to decode " + imageType + " image (Base64) - Bitmap is null");
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    imageView.setImageResource(R.drawable.ic_profile);
                    Log.e(TAG, "Failed to load " + imageType + " image (Base64): " + e.getMessage(), e);
                });
            }
        });
    }

    private void loadProfileImageFromUrl(String url) {
        if (url != null && !url.isEmpty()) {
            try {
                Picasso.get().load(url).placeholder(R.drawable.ic_profile).into(profileImage);
                Log.d(TAG, "Profile image loaded from URL: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load profile image from URL: " + e.getMessage(), e);
                profileImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            profileImage.setImageResource(R.drawable.ic_profile);
        }
    }

    private void loadDogImageFromUrl(String url) {
        if (url != null && !url.isEmpty()) {
            try {
                Picasso.get().load(url).placeholder(R.drawable.ic_profile).into(dogImage);
                Log.d(TAG, "Dog image loaded from URL: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load dog image from URL: " + e.getMessage(), e);
                dogImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            dogImage.setImageResource(R.drawable.ic_profile);
        }
    }

    private int calculateAge(String birthday) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        Date date = sdf.parse(birthday);
        if (date == null) {
            throw new ParseException("Invalid birth date format", 0);
        }
        Calendar birthCal = Calendar.getInstance();
        birthCal.setTime(date);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private boolean isServiceProvider(String userType) {
        return userType != null && (
                userType.equalsIgnoreCase("Dog Walker") ||
                        userType.equalsIgnoreCase("Trainer") ||
                        userType.equalsIgnoreCase("Veterinarian")
        );
    }

    private void loadReviews() {
        userRef.child("reviews").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Review> reviews = new ArrayList<>();
                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    Review review = reviewSnap.getValue(Review.class);
                    if (review != null) {
                        reviews.add(review);
                    }
                }
                reviewAdapter.updateReviews(reviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load reviews: " + error.getMessage());
            }
        });
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(android.graphics.Color.WHITE);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}