package com.example.hugo.bottomnavbar.Profile;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class DogFragment extends Fragment {

    private static final String TAG = "DogFragment";
    private RecyclerView recyclerView;
    private ProgressBar dogLoadingIndicator;
    private DogAdapter dogAdapter;
    private List<Dog> dogList;
    private DatabaseReference databaseRef;
    private String userId;

    public DogFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            }
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }
        userId = user.getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("dogs");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_dog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        try {
            recyclerView = view.findViewById(R.id.dog_recycler_view);
            dogLoadingIndicator = view.findViewById(R.id.dog_loading_indicator);

            if (recyclerView == null || dogLoadingIndicator == null) {
                Log.e(TAG, "One or more views are null");
                Toast.makeText(getContext(), "Error initializing dog list", Toast.LENGTH_SHORT).show();
                return;
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            dogList = new ArrayList<>();
            dogAdapter = new DogAdapter(dogList);
            recyclerView.setAdapter(dogAdapter);

            loadDogs();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize views: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error initializing dog list", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDogs() {
        Log.d(TAG, "Loading dogs for UID: " + userId);
        showLoadingIndicator();
        new LoadDogsTask().execute();
    }

    private class LoadDogsTask extends AsyncTask<Void, Void, List<Dog>> {
        @Override
        protected List<Dog> doInBackground(Void... voids) {
            final List<Dog> dogs = new ArrayList<>();
            try {
                databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        synchronized (dogs) {
                            for (DataSnapshot dogSnapshot : snapshot.getChildren()) {
                                String name = dogSnapshot.child("name").getValue(String.class);
                                String breed = dogSnapshot.child("breed").getValue(String.class);
                                String age = dogSnapshot.child("age").getValue(String.class);
                                String imageUrl = dogSnapshot.child("imageUrl").getValue(String.class);
                                if (name != null) {
                                    dogs.add(new Dog(name, breed, age, imageUrl));
                                }
                            }
                            Log.d(TAG, "Fetched " + dogs.size() + " dogs from Firebase");
                            dogs.notify();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        synchronized (dogs) {
                            Log.e(TAG, "Failed to load dogs: " + error.getMessage());
                            dogs.notify();
                        }
                    }
                });

                // Wait for Firebase callback (timeout after 5 seconds)
                synchronized (dogs) {
                    dogs.wait(5000);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while loading dogs: " + e.getMessage(), e);
            }
            return dogs;
        }

        @Override
        protected void onPostExecute(List<Dog> dogs) {
            if (getActivity() == null) {
                Log.w(TAG, "Activity is null, cannot update UI");
                hideLoadingIndicator();
                return;
            }

            if (dogs.isEmpty()) {
                Toast.makeText(getContext(), "No dogs found or failed to load", Toast.LENGTH_SHORT).show();
            } else {
                dogList.clear();
                dogList.addAll(dogs);
                dogAdapter.notifyDataSetChanged();
                Log.d(TAG, "Updated UI with " + dogs.size() + " dogs");
            }
            hideLoadingIndicator();
        }
    }

    private void showLoadingIndicator() {
        if (dogLoadingIndicator != null) {
            dogLoadingIndicator.setVisibility(View.VISIBLE);
            Log.d(TAG, "Showing dog loading indicator");
        }
    }

    private void hideLoadingIndicator() {
        if (dogLoadingIndicator != null) {
            dogLoadingIndicator.setVisibility(View.GONE);
            Log.d(TAG, "Hiding dog loading indicator");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
    }

    // Placeholder Dog class (adjust based on your actual model)
    public static class Dog {
        private String name;
        private String breed;
        private String age;
        private String imageUrl;

        public Dog(String name, String breed, String age, String imageUrl) {
            this.name = name;
            this.breed = breed;
            this.age = age;
            this.imageUrl = imageUrl;
        }

        public String getName() { return name; }
        public String getBreed() { return breed; }
        public String getAge() { return age; }
        public String getImageUrl() { return imageUrl; }
    }

    // Placeholder DogAdapter (implement based on your needs)
    public static class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {
        private List<Dog> dogList;

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
            holder.nameText.setText(dog.getName());
            holder.breedText.setText(dog.getBreed());
            holder.ageText.setText(dog.getAge());
            if (dog.getImageUrl() != null && !dog.getImageUrl().isEmpty()) {
                Picasso.get()
                        .load(dog.getImageUrl())
                        .resize(100, 100)
                        .centerCrop()
                        .placeholder(R.drawable.ic_dog_placeholder)
                        .error(R.drawable.ic_dog_placeholder)
                        .into(holder.dogImage);
            } else {
                holder.dogImage.setImageResource(R.drawable.ic_dog_placeholder);
            }
        }

        @Override
        public int getItemCount() {
            return dogList.size();
        }

        static class DogViewHolder extends RecyclerView.ViewHolder {
            ImageView dogImage;
            TextView nameText, breedText, ageText;

            DogViewHolder(@NonNull View itemView) {
                super(itemView);
                dogImage = itemView.findViewById(R.id.dog_image);
                nameText = itemView.findViewById(R.id.dog_name);
                breedText = itemView.findViewById(R.id.dog_breed);
                ageText = itemView.findViewById(R.id.dog_age);
            }
        }
    }
}