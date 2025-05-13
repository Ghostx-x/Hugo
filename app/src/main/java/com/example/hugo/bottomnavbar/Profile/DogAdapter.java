package com.example.hugo.bottomnavbar.Profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;

import java.util.List;

public class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {

    private final List<Dog> dogList;

    public DogAdapter(List<Dog> dogList) {
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
        Dog dog = dogList.get(position);
        setTextSafely(holder.nameText, "Name: " + dog.getName());
        setTextSafely(holder.breedText, "Breed: " + dog.getBreed());
        setTextSafely(holder.birthDateText, "Birth Date: " + dog.getBirthDate());
        setTextSafely(holder.genderText, "Gender: " + dog.getGender());
        setTextSafely(holder.sizeText, "Size: " + dog.getSize());
        setTextSafely(holder.descriptionText, "Description: " + (dog.getDescription() != null ? dog.getDescription() : "N/A"));
        if (dog.getImageBase64() != null && !dog.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(dog.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.dogImage.setImageBitmap(bitmap);
                holder.dogImage.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                holder.dogImage.setVisibility(View.GONE);
            }
        } else {
            holder.dogImage.setVisibility(View.GONE);
        }
    }

    private void setTextSafely(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "N/A");
        }
    }

    @Override
    public int getItemCount() {
        return dogList.size();
    }

    static class DogViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, breedText, birthDateText, genderText, sizeText, descriptionText;
        ImageView dogImage;

        DogViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.dog_name_text);
            breedText = itemView.findViewById(R.id.dog_breed_text);
            birthDateText = itemView.findViewById(R.id.dog_birth_date_text);
            genderText = itemView.findViewById(R.id.dog_gender_text);
            sizeText = itemView.findViewById(R.id.dog_size_text);
            descriptionText = itemView.findViewById(R.id.dog_description_text);
            dogImage = itemView.findViewById(R.id.dog_image);
        }
    }
}