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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DogFragment extends Fragment implements DogAdapter.OnDogDeleteListener {

    private static final String TAG = "DogFragment";
    private FirebaseAuth mAuth;
    private DatabaseReference dogsRef;
    private RecyclerView recyclerView;
    private DogAdapter dogAdapter;
    private List<Dog> dogList;
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
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
            return;
        }
        dogsRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).child("dogs");
        dogList = new ArrayList<>();
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
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView not found in layout");
            if (isAdded()) {
                Toast.makeText(requireContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dogAdapter = new DogAdapter(dogList, getContext(), this);
        recyclerView.setAdapter(dogAdapter);

        addDogButton = view.findViewById(R.id.add_dog_button);
        if (addDogButton == null) {
            Log.e(TAG, "Add dog button not found in layout");
            if (isAdded()) {
                Toast.makeText(requireContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        addDogButton.setOnClickListener(v -> {
            if (!isAdded()) return;
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new AddDogFragment());
            transaction.addToBackStack("AddDogFragment");
            transaction.commit();
        });

        ImageView backArrow = view.findViewById(R.id.back_arrow);
        if (backArrow != null) {
            backArrow.setOnClickListener(v -> {
                if (!isAdded()) return;
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new ProfileFragment())
                            .commit();
                }
            });
        }

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
                        if (name != null && breed != null && birthDate != null && gender != null && size != null && key != null) {
                            Dog dog = new Dog(key, name, breed, birthDate, gender, size, description, imageBase64);
                            dogList.add(dog);
                            Log.d(TAG, "Added dog: " + name + " with key: " + key);
                        } else {
                            Log.w(TAG, "Incomplete dog data for key: " + key);
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
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load dogs", Toast.LENGTH_SHORT).show();
                }
                if (loadingCallback != null) {
                    loadingCallback.run();
                }
            }
        });
    }

    @Override
    public void onDogDelete(Dog dog, int position) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot delete dog");
            return;
        }
        String key = dog.getKey();
        if (key == null) {
            Log.w(TAG, "No key found for dog: " + dog.getName());
            Toast.makeText(requireContext(), "Unable to delete dog", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Dog")
                .setMessage("Are you sure you want to delete " + dog.getName() + "'s info?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (!isAdded()) return;
                    dogsRef.child(key).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (!isAdded()) return;
                                dogList.remove(position);
                                dogAdapter.notifyItemRemoved(position);
                                Toast.makeText(requireContext(), "Dog deleted", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Dog deleted: " + dog.getName() + " with key: " + key);
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Log.e(TAG, "Failed to delete dog: " + e.getMessage(), e);
                                Toast.makeText(requireContext(), "Failed to delete dog: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}