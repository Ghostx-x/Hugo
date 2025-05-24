package com.example.hugo.bottomnavbar.Profile;

import android.os.Bundle;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
            float rating = ratingBar.getRating();
            String review = reviewInput.getText().toString().trim();
            DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
            appointmentRef.child("rating").setValue(rating);
            if (!review.isEmpty()) {
                appointmentRef.child("review").setValue(review);
            }
            Toast.makeText(getContext(), "Rating submitted", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });

        return view;
    }
}