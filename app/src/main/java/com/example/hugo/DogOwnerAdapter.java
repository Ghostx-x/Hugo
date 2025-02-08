package com.example.hugo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DogOwnerPagerAdapter extends FragmentStateAdapter {

    public DogOwnerPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DogWalkerListFragment();  // Fragment for the first tab
            case 1:
                return new DogCareTipsFragment();  // Fragment for the second tab
            case 2:
                return new DogWalkingRequestsFragment();  // Fragment for the third tab
            case 3:
                return new ProfileFragment();  // Fragment for the fourth tab
            default:
                return new DogWalkerListFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;  // Total number of tabs
    }
}
