package com.example.hugo.bottomnavbar.Home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentActivity;

import com.example.hugo.R;

import java.util.List;

public class StoryViewAdapter extends RecyclerView.Adapter<StoryViewAdapter.StoryViewHolder> {

    private List<String> storyTitles;
    private FragmentActivity activity;
    private int[] storyBackgrounds = {
            R.drawable.story1,
            R.drawable.story2,
            R.drawable.story3,
            R.drawable.story4,
            R.drawable.story5
    };

    public StoryViewAdapter(List<String> storyTitles, FragmentActivity activity) {
        this.storyTitles = storyTitles;
        this.activity = activity;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item_layout, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        holder.username.setText(storyTitles.get(position));

        // Set dynamic background
        holder.frameLayout.setBackgroundResource(storyBackgrounds[position % storyBackgrounds.length]);

        holder.frameLayout.setOnClickListener(view -> {
            StoryFragment storyFragment = new StoryFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, storyFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return storyTitles.size();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        FrameLayout frameLayout;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            frameLayout = itemView.findViewById(R.id.frameLayout);
        }
    }
}
