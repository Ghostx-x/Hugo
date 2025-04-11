package com.example.hugo;

import android.os.Bundle;
<<<<<<< HEAD
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


import com.example.hugo.bottomnavbar.Home.HomeFragment;
import com.example.hugo.bottomnavbar.Home.StoryFragment;
import com.example.hugo.bottomnavbar.Search.SearchFragment;
import com.example.hugo.bottomnavbar.LocationFragment;
import com.example.hugo.bottomnavbar.Profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
=======

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
>>>>>>> 5cbdb4be63784f0f808166bfbf4aacc506e0590b

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<<<<<<< HEAD
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_location) {
                selectedFragment = new LocationFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else {
                return false;
            }


            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();

            return true;
        });
    }

    public void hideBottomNavigationBar() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    public void showBottomNavigationBar() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }


    public void openStoryFragment(ArrayList<Integer> images) {
        StoryFragment storyFragment = StoryFragment.newInstance(images);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, storyFragment)
                .addToBackStack(null)
                .commit();
    }
}
=======
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
    }
}
>>>>>>> 5cbdb4be63784f0f808166bfbf4aacc506e0590b
