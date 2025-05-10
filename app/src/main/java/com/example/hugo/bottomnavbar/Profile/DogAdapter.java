package com.example.hugo.bottomnavbar.Profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.User;
import com.squareup.picasso.Picasso;

import java.util.List;

public class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {

    private final List<User.Dog> dogList;

    public DogAdapter(List<User.Dog> dogList) {
        this.dogList = dogList;
    }

    @NonNull
    @Override
    public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dog, parent, false);
        return new DogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DogViewHolder holder, int position) {
        User.Dog dog = dogList.get(position);
        holder.dogNameText.setText(dog.name != null ? dog.name : "Unknown");
        if (dog.profileImageUrl != null && !dog.profileImageUrl.isEmpty()) {
            Picasso.get()
                    .load(dog.profileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(holder.dogImage);
        } else {
            holder.dogImage.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public int getItemCount() {
        return dogList.size();
    }

    static class DogViewHolder extends RecyclerView.ViewHolder {
        ImageView dogImage;
        TextView dogNameText;

        DogViewHolder(@NonNull View itemView) {
            super(itemView);
            dogImage = itemView.findViewById(R.id.dog_image);
            dogNameText = itemView.findViewById(R.id.dog_name);
        }
    }
}