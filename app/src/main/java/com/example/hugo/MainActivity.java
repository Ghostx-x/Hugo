package com.example.hugo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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


        // Initialize fragments
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        locationFragment = new LocationFragment();
        profileFragment = new ProfileFragment();

        if (savedInstanceState == null) {
            showLoadingIndicator();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .commit();
            hideLoadingIndicator();
        }

        // Initialize Places in background
        executorService.submit(() -> {
            try {
                Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
                Log.d(TAG, "Google Places initialized");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Google Places: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    // Optionally notify user or handle error
                });
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
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
                Log.d(TAG, "Navigated to fragment: " + selectedFragment.getClass().getSimpleName());
            } catch (Exception e) {
                Log.e(TAG, "Fragment transaction failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    // Handle transaction failure gracefully
                    hideLoadingIndicator();
                });
                return false;
            }

            hideLoadingIndicator();
            return true;
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
        if (loadingIndicator != null) {
            runOnUiThread(() -> loadingIndicator.setVisibility(View.VISIBLE));
            Log.d(TAG, "Showing loading indicator");
        }
    }

    public void hideLoadingIndicator() {
        if (loadingIndicator != null) {
            runOnUiThread(() -> loadingIndicator.setVisibility(View.GONE));
            Log.d(TAG, "Hiding loading indicator");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        Log.d(TAG, "MainActivity destroyed");
    }
}