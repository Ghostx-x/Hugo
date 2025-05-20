package com.example.hugo.bottomnavbar.Home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

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

    private static final String TAG = "HomeFragment";
    private TextView welcomeText;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private BottomNavigationView bottomNavigationView;
    private ShapeableImageView smallProfileIcon, chatButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        ShapeableImageView profileButton = view.findViewById(R.id.profileButton);
        profileButton.setShapeAppearanceModel(
                profileButton.getShapeAppearanceModel()
                        .toBuilder()
                        .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, 50f)
                        .build());
        profileButton.setOnClickListener(v -> navigateToProfileFragment());

        chatButton = view.findViewById(R.id.chatButton);
        chatButton.setOnClickListener(v -> navigateToChatFragment());

        CardView cardView1 = view.findViewById(R.id.card_best_foods);
        CardView cardView2 = view.findViewById(R.id.card_behavior_training);
        CardView cardView3 = view.findViewById(R.id.card_health_aid);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        cardView1.setOnClickListener(v -> {
            ArrayList<Integer> trainingImages = new ArrayList<>(Arrays.asList(
                    R.drawable.story1, R.drawable.story2,
                    R.drawable.story3, R.drawable.story4, R.drawable.story5
            ));
            openStoryFragment(trainingImages);
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
        smallProfileIcon = view.findViewById(R.id.profileButton);

        loadUserData();

        return view;
    }

    private void openStoryFragment(ArrayList<Integer> images) {
        if (images != null && !images.isEmpty()) {
            StoryFragment storyFragment = StoryFragment.newInstance(images);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, storyFragment)
                    .addToBackStack(null)
                    .commit();
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

    private void navigateToChatFragment() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatFragment())
                .addToBackStack(null)
                .commit();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            databaseRef.child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userName = snapshot.child("name").getValue(String.class);
                        String base64Image = snapshot.child("profileImageBase64").getValue(String.class);

                        welcomeText.setText(userName != null ? "Welcome back, " + userName : "Welcome back, User");

                        if (base64Image != null && !base64Image.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                Bitmap circularBitmap = getCircularBitmap(bitmap);
                                smallProfileIcon.setImageBitmap(circularBitmap);
                                Log.d(TAG, "Profile image loaded from Base64");
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to decode Base64 image: " + e.getMessage(), e);
                                smallProfileIcon.setImageResource(R.drawable.ic_profile);
                                Toast.makeText(getContext(), "Failed to load profile icon", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            smallProfileIcon.setImageResource(R.drawable.ic_profile);
                            Log.w(TAG, "No Base64 profile image found");
                        }
                    } else {
                        welcomeText.setText("Welcome back, User");
                        smallProfileIcon.setImageResource(R.drawable.ic_profile);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    welcomeText.setText("Welcome back, User");
                    smallProfileIcon.setImageResource(R.drawable.ic_profile);
                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Helper method to transform a bitmap into a circular shape
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(android.graphics.Color.WHITE);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }
}