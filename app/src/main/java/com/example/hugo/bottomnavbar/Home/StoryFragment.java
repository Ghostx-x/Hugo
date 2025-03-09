package com.example.hugo.bottomnavbar.Home;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager2.widget.ViewPager2;
import com.example.hugo.R;
import java.util.ArrayList;

public class StoryFragment extends Fragment {

    private static final String ARG_IMAGES = "story_images";
    private ArrayList<Integer> images;

    public static StoryFragment newInstance(ArrayList<Integer> images) {
        StoryFragment fragment = new StoryFragment();
        Bundle args = new Bundle();
        args.putIntegerArrayList(ARG_IMAGES, images);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_story, container, false);

        if (getArguments() != null) {
            images = getArguments().getIntegerArrayList(ARG_IMAGES);
        }

        // Initialize ViewPager2 and adapter here
        ViewPager2 viewPager = view.findViewById(R.id.story_view_pager);
        StoryAdapter adapter = new StoryAdapter(images);
        viewPager.setAdapter(adapter);

        return view;
    }
}
