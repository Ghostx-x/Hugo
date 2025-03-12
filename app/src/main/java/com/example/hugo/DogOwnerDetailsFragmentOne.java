package com.example.hugo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DogOwnerDetailsFragmentOne extends Fragment {

    private TextView txtDogCount;
    private int dogCount = 0;

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

        return view;
    }
}
