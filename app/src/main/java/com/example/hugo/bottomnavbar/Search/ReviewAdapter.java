package com.example.hugo.bottomnavbar.Search;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private static final String TAG = "ReviewAdapter";
    private Context context;
    private List<Review> reviews;

    public ReviewAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = new ArrayList<>(reviews);
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        if (review.reviewerName != null && !review.reviewerName.equals("Anonymous")) {
            holder.reviewerName.setText(review.reviewerName);
            Log.d(TAG, "Binding review: " + review.reviewerName + ", Rating: " + review.rating);
        } else {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(review.reviewerId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name == null) {
                        name = snapshot.child("fullName").getValue(String.class);
                    }
                    if (name == null) {
                        name = snapshot.child("displayName").getValue(String.class);
                    }
                    if (name == null) {
                        name = "Anonymous";
                        Log.w(TAG, "No name found for reviewerId: " + review.reviewerId + ", snapshot: " + snapshot.getValue());
                    }
                    holder.reviewerName.setText(name);
                    Log.d(TAG, "Fetched reviewer name: " + name + " for reviewerId: " + review.reviewerId);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch reviewer name: " + error.getMessage());
                    holder.reviewerName.setText("Anonymous");
                }
            });
        }
        holder.reviewRating.setText(String.format("Rating: %d/5", review.rating));
        holder.reviewComment.setText(review.comment != null ? review.comment : "No comment");
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        holder.reviewDate.setText(sdf.format(review.timestamp));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void updateReviews(List<Review> newReviews) {
        this.reviews.clear();
        if (newReviews != null) {
            this.reviews.addAll(newReviews);
        }
        notifyDataSetChanged();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerName, reviewRating, reviewComment, reviewDate;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerName = itemView.findViewById(R.id.reviewer_name);
            reviewRating = itemView.findViewById(R.id.review_rating);
            reviewComment = itemView.findViewById(R.id.review_comment);
            reviewDate = itemView.findViewById(R.id.review_date);
        }
    }
}