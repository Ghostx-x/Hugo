package com.example.hugo.bottomnavbar.Profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;

import java.util.List;

public class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {

    private static final String TAG = "DogAdapter";
    private final List<Dog> dogList;
    private Context context;
    private OnDogDeleteListener deleteListener;

    public interface OnDogDeleteListener {
        void onDogDelete(Dog dog, int position);
    }

    public DogAdapter(List<Dog> dogList, Context context, OnDogDeleteListener deleteListener) {
        this.dogList = dogList;
        this.context = context;
        this.deleteListener = deleteListener;
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
        setTextSafely(holder.nameText, "Name: " + (dog.getName() != null ? dog.getName() : "N/A"));
        setTextSafely(holder.breedText, "Breed: " + (dog.getBreed() != null ? dog.getBreed() : "N/A"));
        setTextSafely(holder.birthDateText, "Birth Date: " + (dog.getBirthDate() != null ? dog.getBirthDate() : "N/A"));
        setTextSafely(holder.genderText, "Gender: " + (dog.getGender() != null ? dog.getGender() : "N/A"));
        setTextSafely(holder.sizeText, "Size: " + (dog.getSize() != null ? dog.getSize() : "N/A"));
        setTextSafely(holder.descriptionText, "Description: " + (dog.getDescription() != null ? dog.getDescription() : "N/A"));

        // Load image from Base64 if available
        if (dog.getImageBase64() != null && !dog.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(dog.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                if (bitmap != null) {
                    holder.dogImage.setImageBitmap(bitmap);
                    holder.dogImage.setVisibility(View.VISIBLE);
                } else {
                    holder.dogImage.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode image: " + e.getMessage(), e);
                holder.dogImage.setVisibility(View.GONE);
            }
        } else {
            holder.dogImage.setVisibility(View.GONE);
        }

        // Edit button
        holder.editIcon.setOnClickListener(v -> {
            if (context instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) context;
                String key = dog.getKey();
                if (key != null) {
                    EditDogFragment editDogFragment = EditDogFragment.newInstance(dog, key);
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, editDogFragment)
                            .addToBackStack("EditDogFragment")
                            .commit();
                } else {
                    Log.w(TAG, "No key found for dog: " + dog.getName());
                    Toast.makeText(context, "Unable to edit dog", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Delete button
        holder.deleteIcon.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDogDelete(dog, position);
            }
        });
    }

    private void setTextSafely(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "N/A");
        }
    }

    @Override
    public int getItemCount() {
        return dogList != null ? dogList.size() : 0;
    }

    static class DogViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, breedText, birthDateText, genderText, sizeText, descriptionText;
        ImageView dogImage, editIcon, deleteIcon;

        DogViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.dog_name_text);
            breedText = itemView.findViewById(R.id.dog_breed_text);
            birthDateText = itemView.findViewById(R.id.dog_birth_date_text);
            genderText = itemView.findViewById(R.id.dog_gender_text);
            sizeText = itemView.findViewById(R.id.dog_size_text);
            descriptionText = itemView.findViewById(R.id.dog_description_text);
            dogImage = itemView.findViewById(R.id.dog_image);
            editIcon = itemView.findViewById(R.id.edit_icon);
            deleteIcon = itemView.findViewById(R.id.delete_icon);
        }
    }
}