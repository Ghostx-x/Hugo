package com.example.hugo.bottomnavbar.Home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
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
import java.util.Map;

@ExperimentalBadgeUtils
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private TextView welcomeText;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private BottomNavigationView bottomNavigationView;
    private ShapeableImageView profileButton, chatButton, alertsButton;
    private BadgeDrawable chatBadge, alertsBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize bottom navigation
        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, "Bottom navigation view not found");
        }

        // Initialize views
        profileButton = view.findViewById(R.id.profileButton);
        chatButton = view.findViewById(R.id.chatButton);
        alertsButton = view.findViewById(R.id.alertsButton);
        welcomeText = view.findViewById(R.id.welcomeText);

        if (profileButton != null) {
            profileButton.setShapeAppearanceModel(
                    profileButton.getShapeAppearanceModel()
                            .toBuilder()
                            .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, 50f)
                            .build());
            profileButton.setOnClickListener(v -> navigateToProfileFragment());
        } else {
            Log.e(TAG, "Profile button not found");
        }

        if (chatButton != null) {
            chatButton.setOnClickListener(v -> navigateToChatFragment());
            updateChatBadge();
        } else {
            Log.e(TAG, "Chat button not found");
        }

        if (alertsButton != null) {
            alertsButton.setOnClickListener(v -> {
                navigateToAlertsFragment();
                // Clear badge when navigating to AlertsFragment
                if (alertsBadge != null) {
                    alertsBadge.setVisible(false);
                    alertsBadge = null;
                }
                // Mark notifications as read
                markNotificationsAsRead();
            });
            updateAlertsBadge();
        } else {
            Log.e(TAG, "Alerts button not found");
        }

        CardView cardBestFoods = view.findViewById(R.id.card_best_foods);
        CardView cardBehaviorTraining = view.findViewById(R.id.card_behavior_training);
        CardView cardHealthAid = view.findViewById(R.id.card_health_aid);

        if (cardBestFoods != null) {
            cardBestFoods.setOnClickListener(v -> openStoryFragment(new ArrayList<>(Arrays.asList(
                    R.drawable.story1, R.drawable.story2, R.drawable.story3, R.drawable.story4, R.drawable.story5))));
        } else {
            Log.e(TAG, "Best foods card not found");
        }

        if (cardBehaviorTraining != null) {
            cardBehaviorTraining.setOnClickListener(v -> openStoryFragment(new ArrayList<>(Arrays.asList(
                    R.drawable.train1, R.drawable.train2, R.drawable.train3, R.drawable.train4))));
        } else {
            Log.e(TAG, "Behavior training card not found");
        }

        if (cardHealthAid != null) {
            cardHealthAid.setOnClickListener(v -> openStoryFragment(new ArrayList<>(Arrays.asList(
                    R.drawable.health1, R.drawable.health2, R.drawable.health3, R.drawable.health4))));
        } else {
            Log.e(TAG, "Health aid card not found");
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        loadUserData();

        return view;
    }

    @ExperimentalBadgeUtils
    private void updateChatBadge() {
        if (chatButton == null) {
            Log.w(TAG, "Chat button is null, cannot update badge");
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("ChatPrefs", Context.MODE_PRIVATE);
        boolean hasUnreadMessages = prefs.getBoolean("hasUnreadMessages", false);

        if (hasUnreadMessages) {
            int totalUnread = 0;
            Map<String, ?> allEntries = prefs.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getKey().startsWith("unread_")) {
                    totalUnread += (int) entry.getValue();
                }
            }

            if (chatBadge == null) {
                chatBadge = BadgeDrawable.create(requireContext());
                BadgeUtils.attachBadgeDrawable(chatBadge, chatButton, null);
            }
            chatBadge.setNumber(totalUnread);
            chatBadge.setBackgroundColor(Color.RED);
            chatBadge.setBadgeGravity(BadgeDrawable.TOP_END);
            chatBadge.setHorizontalOffset(10);
            chatBadge.setVerticalOffset(10);
            chatBadge.setVisible(true);
        } else {
            if (chatBadge != null) {
                chatBadge.setVisible(false);
                chatBadge = null;
            }
        }
    }

    @ExperimentalBadgeUtils
    private void updateAlertsBadge() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || alertsButton == null) {
            Log.w(TAG, "User or alertsButton is null, cannot update badge");
            return;
        }

        DatabaseReference alertsRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).child("Alerts");
        alertsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long unreadCount = snapshot.getChildrenCount();
                if (unreadCount > 0) {
                    if (alertsBadge == null) {
                        alertsBadge = BadgeDrawable.create(requireContext());
                        BadgeUtils.attachBadgeDrawable(alertsBadge, alertsButton, null);
                    }
                    alertsBadge.setNumber((int) unreadCount);
                    alertsBadge.setBackgroundColor(Color.RED);
                    alertsBadge.setBadgeGravity(BadgeDrawable.TOP_END);
                    alertsBadge.setHorizontalOffset(10);
                    alertsBadge.setVerticalOffset(10);
                    alertsBadge.setVisible(true);
                    alertsButton.setImageResource(R.drawable.alert_active); // Change to active icon
                } else {
                    if (alertsBadge != null) {
                        alertsBadge.setVisible(false);
                        alertsBadge = null;
                    }
                    alertsButton.setImageResource(R.drawable.alert); // Revert to normal icon
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load alerts: " + error.getMessage());
            }
        });
    }

    private void markNotificationsAsRead() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference alertsRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).child("Alerts");
            alertsRef.removeValue().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Notifications marked as read");
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to mark notifications as read: " + e.getMessage());
            });
        }
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
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void navigateToChatFragment() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToAlertsFragment() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AlertsFragment())
                .addToBackStack(null)
                .commit();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user");
            return;
        }

        String userId = user.getUid();
        databaseRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userName = snapshot.child("name").getValue(String.class);
                    String base64Image = snapshot.child("profileImageBase64").getValue(String.class);

                    if (welcomeText != null) {
                        welcomeText.setText(userName != null ? "Welcome back, " + userName : "Welcome back, User");
                    }

                    if (profileButton != null) {
                        if (base64Image != null && !base64Image.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                Bitmap circularBitmap = getCircularBitmap(bitmap);
                                profileButton.setImageBitmap(circularBitmap);
                                Log.d(TAG, "Profile image loaded from Base64");
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to decode Base64 image: " + e.getMessage(), e);
                                profileButton.setImageResource(R.drawable.ic_profile);
                                Toast.makeText(getContext(), "Failed to load profile icon", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            profileButton.setImageResource(R.drawable.ic_profile);
                            Log.w(TAG, "No Base64 profile image found");
                        }
                    }
                } else {
                    if (welcomeText != null) {
                        welcomeText.setText("Welcome back, User");
                    }
                    if (profileButton != null) {
                        profileButton.setImageResource(R.drawable.ic_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (welcomeText != null) {
                    welcomeText.setText("Welcome back, User");
                }
                if (profileButton != null) {
                    profileButton.setImageResource(R.drawable.ic_profile);
                }
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
    public void onResume() {
        super.onResume();
        updateChatBadge();
        updateAlertsBadge();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }
}