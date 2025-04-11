package com.example.hugo.bottomnavbar.Home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hugo.R;
import java.util.ArrayList;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {
    private ArrayList<Integer> storyImages;

    public StoryAdapter(ArrayList<Integer> storyImages) {
        this.storyImages = storyImages;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        holder.storyImageView.setImageResource(storyImages.get(position));
    }

    @Override
    public int getItemCount() {
        return storyImages.size();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView storyImageView;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            storyImageView = itemView.findViewById(R.id.story_image);
        }
    }
}
