package com.example.hugo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.badge.ExperimentalBadgeUtils;

import com.example.hugo.bottomnavbar.Home.ConversationFragment;
import com.example.hugo.bottomnavbar.Home.HomeFragment;
import com.example.hugo.bottomnavbar.Home.StoryFragment;
import com.example.hugo.bottomnavbar.Search.SearchFragment;
import com.example.hugo.bottomnavbar.LocationFragment;
import com.example.hugo.bottomnavbar.Profile.ProfileFragment;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.annotation.OptIn;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;
    private ProgressBar loadingIndicator;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Fragment homeFragment, searchFragment, locationFragment, profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // Initialize fragments
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        locationFragment = new LocationFragment();
        profileFragment = new ProfileFragment();

        if (bottomNavigationView == null || loadingIndicator == null) {
            Log.e(TAG, "Critical views not found in layout");
            Toast.makeText(this, "UI initialization failed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            Log.d(TAG, "Initial load: HomeFragment");
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .commit();
            showBottomNavigationBar();
        }

        // Initialize Places in background
        executorService.submit(() -> {
            try {
                Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
                Log.d(TAG, "Google Places initialized");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Google Places: " + e.getMessage(), e);
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            showLoadingIndicator();
            if (itemId == R.id.nav_home) {
                selectedFragment = homeFragment;
            } else if (itemId == R.id.nav_search) {
                selectedFragment = searchFragment;
            } else if (itemId == R.id.nav_location) {
                selectedFragment = locationFragment;
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = profileFragment;
            } else {
                hideLoadingIndicator();
                return false;
            }

            try {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                showBottomNavigationBar();
                Log.d(TAG, "Navigated to fragment: " + selectedFragment.getClass().getSimpleName());
            } catch (Exception e) {
                Log.e(TAG, "Fragment transaction failed: " + e.getMessage(), e);
                hideLoadingIndicator();
                return false;
            }

            hideLoadingIndicator();
            return true;
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("navigateTo")) {
            String navigateTo = intent.getStringExtra("navigateTo");
            if ("ConversationFragment".equals(navigateTo)) {
                String otherUserId = intent.getStringExtra("otherUserId");
                String otherUserName = intent.getStringExtra("otherUserName");
                Fragment fragment = ConversationFragment.newInstance(otherUserId, otherUserName);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
        }

        // Ensure navigation bar visibility on back stack changes
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof HomeFragment ||
                    currentFragment instanceof SearchFragment ||
                    currentFragment instanceof LocationFragment ||
                    currentFragment instanceof ProfileFragment) {
                showBottomNavigationBar();
                Log.d(TAG, "Back stack changed, showing bottom navigation");
            }
        });
    }

    public void hideBottomNavigationBar() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
            Log.d(TAG, "Bottom navigation bar hidden");
        }
    }

    public void showBottomNavigationBar() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Bottom navigation bar shown");
        }
    }

    public void openStoryFragment(ArrayList<Integer> images) {
        showLoadingIndicator();
        StoryFragment storyFragment = StoryFragment.newInstance(images);
        try {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, storyFragment)
                    .addToBackStack(null)
                    .commit();
            Log.d(TAG, "Opened StoryFragment");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open StoryFragment: " + e.getMessage(), e);
        }
        hideLoadingIndicator();
    }

    public void showLoadingIndicator() {
        runOnUiThread(() -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.VISIBLE);
                Log.d(TAG, "Showing loading indicator");
            } else {
                Log.e(TAG, "Loading indicator is null");
            }
        });
    }

    public void hideLoadingIndicator() {
        runOnUiThread(() -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.GONE);
                Log.d(TAG, "Hiding loading indicator");
            } else {
                Log.e(TAG, "Loading indicator is null");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        Log.d(TAG, "MainActivity destroyed");
    }
}