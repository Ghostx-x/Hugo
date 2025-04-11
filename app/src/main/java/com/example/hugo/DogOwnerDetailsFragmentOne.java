package com.example.hugo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class DogOwnerDetailsFragmentOne extends Fragment {

    private TextView txtDogCount;
    private int dogCount = 0;
    private Button next;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    public DogOwnerDetailsFragmentOne() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_dog_owner_details_fragment_one, container, false);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        txtDogCount = view.findViewById(R.id.txtDogCount);
        ImageButton btnIncrease = view.findViewById(R.id.btnIncrease);
        ImageButton btnDecrease = view.findViewById(R.id.btnDecrease);
        next = view.findViewById(R.id.btn_next);

        btnIncrease.setOnClickListener(v -> {
            dogCount++;
            txtDogCount.setText(String.valueOf(dogCount));
        });

        btnDecrease.setOnClickListener(v -> {
            if (dogCount > 0) {
                dogCount--;
                txtDogCount.setText(String.valueOf(dogCount));
            }
        });

        next.setOnClickListener(v -> {
            // Save the dog count to Firebase
            saveDogCountToFirebase(dogCount);

            // Proceed to next fragment
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            DogOwnerDetailsFragmentTwo fragmentTwo = new DogOwnerDetailsFragmentTwo();
            Bundle args = new Bundle();
            args.putInt("dogCount", dogCount);
            fragmentTwo.setArguments(args);

            transaction.replace(R.id.fragment_container, fragmentTwo);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    private void saveDogCountToFirebase(int dogCount) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("dogCount", dogCount);

            // Save the dog count under the user's UID
            databaseRef.child(userId).updateChildren(userMap)
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Dog count saved!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to save dog count.", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(requireActivity(), SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}
