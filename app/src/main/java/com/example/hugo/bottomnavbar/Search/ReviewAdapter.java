package com.example.hugo.bottomnavbar.Search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;
    private Context context;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList != null ? reviewList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        if (review == null) return;

        holder.ratingText.setText(String.format(Locale.getDefault(), "Rating: %d/5", review.rating));
        holder.commentText.setText(review.comment != null ? review.comment : "No comment");

        // Format timestamp
        if (review.timestamp > 0) {
            Date date = new Date(review.timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.dateText.setText(sdf.format(date));
        } else {
            holder.dateText.setText("Unknown date");
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void updateReviews(List<Review> reviews) {
        this.reviewList = reviews != null ? reviews : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView ratingText, commentText, dateText;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ratingText = itemView.findViewById(R.id.review_rating);
            commentText = itemView.findViewById(R.id.review_comment);
            dateText = itemView.findViewById(R.id.review_date);
        }
    }
}