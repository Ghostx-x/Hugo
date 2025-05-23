package com.example.hugo.bottomnavbar.Profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.example.hugo.bottomnavbar.Search.Dog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DogFragment extends Fragment implements DogAdapter.OnDogDeleteListener {

    private static final String TAG = "DogFragment";
    private FirebaseAuth mAuth;
    private DatabaseReference dogsRef;
    private RecyclerView recyclerView;
    private DogAdapter dogAdapter;
    private List<Dog> dogList;
    private Map<String, String> dogKeys; // Map to store original Firebase keys
    private Button addDogButton;
    private Runnable loadingCallback;

    public DogFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user detected in onCreate");
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
            return;
        }
        dogsRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).child("dogs");
        dogList = new ArrayList<>();
        dogKeys = new HashMap<>(); // Initialize key map
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        return inflater.inflate(R.layout.fragment_dog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated started");

        recyclerView = view.findViewById(R.id.dog_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dogAdapter = new DogAdapter(dogList, getContext(), this, dogKeys);
        recyclerView.setAdapter(dogAdapter);

        addDogButton = view.findViewById(R.id.add_dog_button);
        addDogButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new AddDogFragment());
            transaction.addToBackStack("AddDogFragment");
            transaction.commit();
        });

        ImageView backArrow = view.findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .commit();
            }
        });

        loadDogs();
        Log.d(TAG, "UI set up");
    }

    public void setLoadingCallback(Runnable callback) {
        this.loadingCallback = callback;
    }

    private void loadDogs() {
        dogsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dogList.clear();
                dogKeys.clear();
                for (DataSnapshot dogSnapshot : snapshot.getChildren()) {
                    try {
                        String key = dogSnapshot.getKey();
                        String name = dogSnapshot.child("name").getValue(String.class);
                        String breed = dogSnapshot.child("breed").getValue(String.class);
                        String birthDate = dogSnapshot.child("birthDate").getValue(String.class);
                        String gender = dogSnapshot.child("gender").getValue(String.class);
                        String size = dogSnapshot.child("size").getValue(String.class);
                        String description = dogSnapshot.child("description").getValue(String.class);
                        String imageBase64 = dogSnapshot.child("imageBase64").getValue(String.class);
                        if (name != null && breed != null && birthDate != null && gender != null && size != null) {
                            Dog dog = new Dog(name, breed, birthDate, gender, size, description, imageBase64);
                            dogList.add(dog);
                            if (key != null) {
                                dogKeys.put(name, key); // Map name to Firebase key
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse dog data: " + e.getMessage(), e);
                    }
                }
                dogAdapter.notifyDataSetChanged();
                Log.d(TAG, "Loaded " + dogList.size() + " dogs");
                if (loadingCallback != null) {
                    loadingCallback.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load dogs: " + error.getMessage(), error.toException());
                Toast.makeText(getContext(), "Failed to load dogs", Toast.LENGTH_SHORT).show();
                if (loadingCallback != null) {
                    loadingCallback.run();
                }
            }
        });
    }

    @Override
    public void onDogDelete(Dog dog, int position) {
        String key = dogKeys.get(dog.getName());
        if (key != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Dog")
                    .setMessage("Are you sure you want to delete " + dog.getName() + "'s info?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dogsRef.child(key).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    dogList.remove(position);
                                    dogAdapter.notifyItemRemoved(position);
                                    Toast.makeText(getContext(), "Dog deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete dog: " + e.getMessage(), e);
                                    Toast.makeText(getContext(), "Failed to delete dog", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            Log.w(TAG, "No key found for dog: " + dog.getName());
            Toast.makeText(getContext(), "Unable to delete dog", Toast.LENGTH_SHORT).show();
        }
    }
}