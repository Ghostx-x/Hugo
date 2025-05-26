package com.example.hugo.bottomnavbar.Search;

public class Review {
    public String reviewerId;
    public String reviewerName;
    public String comment;
    public int rating;
    public long timestamp;

    public Review() {
    }

    public Review(String reviewerId, String comment, int rating, long timestamp, String reviewerName) {
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.comment = comment;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}