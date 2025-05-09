package com.example.hugo.bottomnavbar.Home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;

public class HomeFragment extends Fragment {

    private TextView welcomeText;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private BottomNavigationView bottomNavigationView;
    private ImageView smallProfileIcon;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        ShapeableImageView profileButton = view.findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            navigateToProfileFragment();
        });


        CardView cardView1 = view.findViewById(R.id.card_best_foods);
        CardView cardView2 = view.findViewById(R.id.card_behavior_training);
        CardView cardView3 = view.findViewById(R.id.card_health_aid);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");


        cardView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Integer> trainingImages = new ArrayList<>(Arrays.asList(
                        R.drawable.story1, R.drawable.story2,
                        R.drawable.story3, R.drawable.story4, R.drawable.story5
                ));
                openStoryFragment(trainingImages);
            }
        });

        cardView2.setOnClickListener(v -> {
            ArrayList<Integer> trainingImages = new ArrayList<>(Arrays.asList(
                    R.drawable.train1, R.drawable.train2,
                    R.drawable.train3, R.drawable.train4
            ));
            openStoryFragment(trainingImages);
        });

        cardView3.setOnClickListener(v -> {
            ArrayList<Integer> healthImages = new ArrayList<>(Arrays.asList(
                    R.drawable.health1, R.drawable.health2,
                    R.drawable.health3, R.drawable.health4
            ));
            openStoryFragment(healthImages);
        });
        welcomeText = view.findViewById(R.id.welcomeText);
        loadUserName();


        return view;
    }

    private void openStoryFragment(ArrayList<Integer> images) {
        if (images != null && !images.isEmpty()) {

//            bottomNavigationView.setVisibility(View.VISIBLE);

            StoryFragment storyFragment = StoryFragment.newInstance(images);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, storyFragment)
                    .addToBackStack(null)
                    .commit();
        } else {

        }
    }

    private void navigateToProfileFragment() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProfileFragment())
                .addToBackStack(null)
                .commit();
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    private void loadUserName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            databaseRef.child(userId).child("name").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userName = snapshot.getValue(String.class);
                        welcomeText.setText("Welcome back, " + userName);
                    } else {
                        welcomeText.setText("Welcome back, User");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    welcomeText.setText("Welcome back, User");
                }
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        smallProfileIcon = view.findViewById(R.id.profileButton);

        loadProfileImage();
    }
    private void loadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            databaseRef.child(user.getUid()).child("profileImageBase64")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String base64Image = snapshot.getValue(String.class);
                                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                smallProfileIcon.setImageBitmap(decodedBitmap);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Failed to load profile icon", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    @Override
    public void onPause() {
        super.onPause();

        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }
}
