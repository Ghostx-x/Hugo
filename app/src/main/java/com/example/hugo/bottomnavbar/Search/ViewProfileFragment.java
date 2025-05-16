package com.example.hugo.bottomnavbar.Search;

import android.os.Bundle;
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
import com.example.hugo.bottomnavbar.Search.Dog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            Toast.makeText(getContext(), "Chat with " + profileName.getText(), Toast.LENGTH_SHORT).show();
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
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    Log.w(TAG, "User not found");
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }


                profileName.setText(user.name != null ? user.name : "No Name");
                profileBio.setText(user.bio != null ? user.bio : "No bio");
                profileUserType.setText(user.userType != null ? user.userType : "Unknown");
                profileRanking.setText(user.ranking != null && user.ranking > 0 ? String.format("Rating: %.1f/5", user.ranking) : "Rating: N/A");

                if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                    Picasso.get()
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(profileImage, new com.squareup.picasso.Callback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Profile image loaded: " + user.profileImageUrl);
                                }
                                @Override
                                public void onError(Exception e) {
                                    Log.e(TAG, "Failed to load profile image: " + user.profileImageUrl, e);
                                }
                            });
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile);
                }


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
                    }
                } else {
                    Log.w(TAG, "Skipping availability display: profileAvailability is null");
                }

                userRef.child("dogs").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dogsSnapshot) {
                        Dog dog = null;

                        for (DataSnapshot dogSnapshot : dogsSnapshot.getChildren()) {
                            dog = dogSnapshot.getValue(Dog.class);
                            break;
                        }

                        if (dog != null) {
                            dogInfoCard.setVisibility(View.VISIBLE);
                            dogName.setText(dog.name != null ? dog.name : "No Name");
                            dogBreed.setText(dog.breed != null ? dog.breed : "Unknown Breed");
                            dogAge.setText(dog.age > 0 ? dog.age + " years" : "Unknown Age");

                            final String dogImageUrl = dog.profileImageUrl;
                            if (dogImageUrl != null && !dogImageUrl.isEmpty()) {
                                Picasso.get()
                                        .load(dogImageUrl)
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile)
                                        .into(dogImage, new com.squareup.picasso.Callback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "Dog image loaded: " + dogImageUrl);
                                            }
                                            @Override
                                            public void onError(Exception e) {
                                                Log.e(TAG, "Failed to load dog image: " + dogImageUrl, e);
                                            }
                                        });
                            } else {
                                dogImage.setImageResource(R.drawable.ic_profile);
                            }
                        } else {
                            dogInfoCard.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load dog data: " + error.getMessage());
                        dogInfoCard.setVisibility(View.GONE);
                    }
                });


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
}