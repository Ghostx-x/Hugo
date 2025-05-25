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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RatingFragment extends Fragment {

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
                Toast.makeText(getContext(), "Invalid appointment", Toast.LENGTH_SHORT).show();
                return;
            }
            float rating = ratingBar.getRating();
            String review = reviewInput.getText().toString().trim();
            DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
            appointmentRef.child("rating").setValue(rating);
            if (!review.isEmpty()) {
                appointmentRef.child("review").setValue(review);
            }

            // Update service provider's ranking
            updateServiceProviderRanking(appointmentId, rating);

            Toast.makeText(getContext(), "Rating submitted", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });

        return view;
    }

    private void updateServiceProviderRanking(String appointmentId, float newRating) {
        DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        appointmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String walkerId = snapshot.child("dogWalkerId").getValue(String.class);
                if (walkerId != null) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId);
                    DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
                    appointmentsRef.orderByChild("dogWalkerId").equalTo(walkerId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            double totalRating = 0.0;
                            int ratingCount = 0;
                            for (DataSnapshot apptSnapshot : snapshot.getChildren()) {
                                Float rating = apptSnapshot.child("rating").getValue(Float.class);
                                if (rating != null) {
                                    totalRating += rating;
                                    ratingCount++;
                                }
                            }
                            if (ratingCount > 0) {
                                double averageRating = totalRating / ratingCount;
                                userRef.child("ranking").setValue(averageRating).addOnSuccessListener(aVoid -> {
                                    Log.d("RatingFragment", "Updated ranking for user " + walkerId + ": " + averageRating);
                                }).addOnFailureListener(e -> {
                                    Log.e("RatingFragment", "Failed to update ranking: " + e.getMessage());
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("RatingFragment", "Failed to fetch appointments: " + error.getMessage());
                        }
                    });
                } else {
                    Log.w("RatingFragment", "No dogWalkerId found for appointment: " + appointmentId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RatingFragment", "Failed to fetch appointment: " + error.getMessage());
            }
        });
    }
}