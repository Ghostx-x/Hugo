package com.example.hugo.bottomnavbar.Search;

public class Review {
    public String reviewerId;
    public String comment;
    public int rating;
    public long timestamp;

    public Review() {}

    public Review(String reviewerId, String comment, int rating, long timestamp) {
        this.reviewerId = reviewerId;
        this.comment = comment;
        this.rating = rating;
        this.timestamp = timestamp;
    }
}