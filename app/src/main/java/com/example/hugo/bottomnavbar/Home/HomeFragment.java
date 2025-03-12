package com.example.hugo.bottomnavbar.Home;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.cardview.widget.CardView;
import com.example.hugo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;

public class HomeFragment extends Fragment {

    private BottomNavigationView bottomNavigationView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);


        CardView cardView1 = view.findViewById(R.id.card_best_foods);
        CardView cardView2 = view.findViewById(R.id.card_behavior_training);
        CardView cardView3 = view.findViewById(R.id.card_health_aid);



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

    @Override
    public void onPause() {
        super.onPause();

        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }
}
