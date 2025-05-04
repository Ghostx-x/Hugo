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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ViewProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private ImageView profileImage, dogImage;
    private TextView profileName, profileBio, profileUserType, profileRanking;
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

        // Initialize UI
        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileBio = view.findViewById(R.id.profile_bio);
        profileUserType = view.findViewById(R.id.profile_user_type);
        profileRanking = view.findViewById(R.id.profile_ranking);
        dogInfoCard = view.findViewById(R.id.dog_info_card);
        dogImage = view.findViewById(R.id.dog_image);
        dogName = view.findViewById(R.id.dog_name);
        dogBreed = view.findViewById(R.id.dog_breed);
        dogAge = view.findViewById(R.id.dog_age);
        reviewsRecyclerView = view.findViewById(R.id.reviews_recycler_view);
        chatButton = view.findViewById(R.id.chat_button);
        bookButton = view.findViewById(R.id.book_button);

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        // Set up RecyclerView for reviews
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reviewAdapter = new ReviewAdapter(getContext(), new ArrayList<>());
        reviewsRecyclerView.setAdapter(reviewAdapter);

        // Get user ID from arguments
        String userId = getArguments() != null ? getArguments().getString(ARG_USER_ID) : null;
        if (userId == null) {
            Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        // Fetch user data
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        loadUserData();

        // Button click listeners
        chatButton.setOnClickListener(v -> {
            // TODO: Navigate to chat screen with userId
            Toast.makeText(getContext(), "Chat with " + profileName.getText(), Toast.LENGTH_SHORT).show();
        });

        bookButton.setOnClickListener(v -> {
            // TODO: Navigate to booking screen with userId
            Toast.makeText(getContext(), "Book with " + profileName.getText(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }

                // Display user info
                profileName.setText(user.name != null ? user.name : "No Name");
                profileBio.setText(user.bio != null ? user.bio : "");
                profileUserType.setText(user.userType != null ? user.userType : "");
                profileRanking.setText(user.ranking > 0 ? String.format("Rating: %.1f/5", user.ranking) : "Rating: N/A");

                if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                    Picasso.get()
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(profileImage, new com.squareup.picasso.Callback() {
                                @Override
                                public void onSuccess() {}
                                @Override
                                public void onError(Exception e) {
                                    Log.e("Picasso", "Failed to load profile image: " + user.profileImageUrl, e);
                                }
                            });
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile);
                }

                // Display dog info if available
                if (user.dog != null) {
                    dogInfoCard.setVisibility(View.VISIBLE);
                    dogName.setText(user.dog.name != null ? user.dog.name : "No Name");
                    dogBreed.setText(user.dog.breed != null ? user.dog.breed : "Unknown Breed");
                    dogAge.setText(user.dog.age > 0 ? user.dog.age + " years" : "Unknown Age");

                    if (user.dog.profileImageUrl != null && !user.dog.profileImageUrl.isEmpty()) {
                        Picasso.get()
                                .load(user.dog.profileImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(dogImage, new com.squareup.picasso.Callback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError(Exception e) {
                                        Log.e("Picasso", "Failed to load dog image: " + user.dog.profileImageUrl, e);
                                    }
                                });
                    } else {
                        dogImage.setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    dogInfoCard.setVisibility(View.GONE);
                }

                // Show/hide book button based on userType
                if (user.userType != null && user.userType.equalsIgnoreCase("Dog Owner")) {
                    bookButton.setVisibility(View.GONE);
                } else {
                    bookButton.setVisibility(View.VISIBLE);
                }

                // Load reviews
                loadReviews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });
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
                Log.e("ViewProfileFragment", "Failed to load reviews: " + error.getMessage());
            }
        });
    }
}