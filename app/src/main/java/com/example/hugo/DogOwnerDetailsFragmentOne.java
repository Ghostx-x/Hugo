package com.example.hugo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class DogOwnerDetailsFragmentOne extends Fragment {

    private TextView txtDogCount;
    private int dogCount = 0;
    private Button next;

    public DogOwnerDetailsFragmentOne() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_dog_owner_details_fragment_one, container, false);

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
