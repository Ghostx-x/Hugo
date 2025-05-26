package com.example.hugo.bottomnavbar.Profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RatingFragment extends Fragment {

    private static final String TAG = "RatingFragment";
    private RatingBar ratingBar;
    private EditText reviewInput;
    private Button submitRatingButton;
    private String appointmentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rating, container, false);

        ratingBar = view.findViewById(R.id.rating_bar);
        reviewInput = view.findViewById(R.id.review_input);
        submitRatingButton = view.findViewById(R.id.submit_rating_button);
        appointmentId = getArguments() != null ? getArguments().getString("appointmentId") : null;

        submitRatingButton.setOnClickListener(v -> {
            if (appointmentId == null) {
                Log.w(TAG, "Invalid appointmentId: null");
                Toast.makeText(getContext(), "Invalid appointment", Toast.LENGTH_SHORT).show();
                return;
            }
            float rating = ratingBar.getRating();
            String review = reviewInput.getText().toString().trim();
            DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
            appointmentRef.child("rating").setValue(rating)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save rating: " + e.getMessage()));
            if (!review.isEmpty()) {
                appointmentRef.child("review").setValue(review)
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to save review: " + e.getMessage()));
            }

            updateServiceProviderRanking(appointmentId, rating, review);

            Toast.makeText(getContext(), "Rating submitted", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void updateServiceProviderRanking(String appointmentId, float newRating, String review) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user");
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }
        String reviewerId = currentUser.getUid();
        Log.d(TAG, "Fetching data for reviewerId: " + reviewerId);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(reviewerId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String reviewerName = snapshot.child("name").getValue(String.class);
                if (reviewerName == null) {
                    reviewerName = snapshot.child("fullName").getValue(String.class);
                }
                if (reviewerName == null) {
                    reviewerName = snapshot.child("displayName").getValue(String.class);
                }
                if (reviewerName == null && currentUser.getDisplayName() != null) {
                    reviewerName = currentUser.getDisplayName();
                }
                if (reviewerName == null) {
                    reviewerName = "Anonymous";
                    Log.w(TAG, "No name found for reviewer: " + reviewerId + ", snapshot: " + snapshot.getValue());
                } else {
                    Log.d(TAG, "Reviewer name fetched: " + reviewerName);
                }
                fetchWalkerIdAndUpdateRanking(appointmentId, newRating, review, reviewerId, reviewerName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch reviewer data: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWalkerIdAndUpdateRanking(String appointmentId, float newRating, String review, String reviewerId, String reviewerName) {
        DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        appointmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String walkerId = snapshot.child("dogWalkerId").getValue(String.class);
                if (walkerId == null) {
                    Log.w(TAG, "No dogWalkerId found for appointment: " + appointmentId);
                    Toast.makeText(getContext(), "Cannot update rating: Walker ID not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateRankingAndReviews(walkerId, newRating, review, reviewerId, reviewerName);
                updateExistingReviews(walkerId, reviewerId, reviewerName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch appointment: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to update rating", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRankingAndReviews(String walkerId, float newRating, String review, String reviewerId, String reviewerName) {
        DatabaseReference providerRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId);
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        appointmentsRef.orderByChild("dogWalkerId").equalTo(walkerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalRating = 0.0;
                int ratingCount = 0;
                for (DataSnapshot apptSnapshot : snapshot.getChildren()) {
                    Float rating = apptSnapshot.child("rating").getValue(Float.class);
                    if (rating != null && rating > 0) {
                        totalRating += rating;
                        ratingCount++;
                    }
                }

                if (ratingCount > 0) {
                    double averageRating = totalRating / ratingCount;
                    providerRef.child("ranking").setValue(averageRating).addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Updated ranking for user " + walkerId + ": " + averageRating);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update ranking: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to update rating", Toast.LENGTH_SHORT).show();
                    });

                    if (!review.isEmpty()) {
                        long timestamp = System.currentTimeMillis();
                        Review newReview = new Review(reviewerId, review, (int) newRating, timestamp, reviewerName);
                        DatabaseReference reviewsRef = providerRef.child("reviews").push();
                        reviewsRef.setValue(newReview).addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Review added for user " + walkerId + ": " + review + ", reviewerName: " + reviewerName);
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to add review: " + e.getMessage());
                            Toast.makeText(getContext(), "Failed to add review", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.w(TAG, "No valid ratings found for walker: " + walkerId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch appointments: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to update rating", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateExistingReviews(String walkerId, String reviewerId, String reviewerName) {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId).child("reviews");
        reviewsRef.orderByChild("reviewerId").equalTo(reviewerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                    if (reviewSnapshot.child("reviewerName").getValue(String.class) == null) {
                        reviewSnapshot.getRef().child("reviewerName").setValue(reviewerName)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated reviewerName for review: " + reviewSnapshot.getKey()))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update reviewerName: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch reviews for update: " + error.getMessage());
            }
        });
    }
}