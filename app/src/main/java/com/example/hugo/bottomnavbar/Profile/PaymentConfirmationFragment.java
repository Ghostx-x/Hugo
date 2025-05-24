package com.example.hugo.bottomnavbar.Profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PaymentConfirmationFragment extends Fragment {

    private Button yesButton, noButton;
    private String appointmentId;
    private float price;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_confirmation, container, false);

        yesButton = view.findViewById(R.id.yes_button);
        noButton = view.findViewById(R.id.no_button);
        appointmentId = getArguments() != null ? getArguments().getString("appointmentId") : null;
        price = getArguments() != null ? getArguments().getFloat("price", 0.0f) : 0.0f;

        yesButton.setOnClickListener(v -> {
            DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
            appointmentRef.child("status").setValue("Completed");
            RatingFragment fragment = new RatingFragment();
            Bundle args = new Bundle();
            args.putString("appointmentId", appointmentId);
            fragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        noButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }
}