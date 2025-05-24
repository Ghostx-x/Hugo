package com.example.hugo.bottomnavbar.Profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PaymentFragment extends Fragment {

    private TextView paymentAmount;
    private Button confirmPaymentButton;
    private String appointmentId;
    private Float price; // Changed from float to Float

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);

        paymentAmount = view.findViewById(R.id.payment_amount);
        confirmPaymentButton = view.findViewById(R.id.confirm_payment_button);
        appointmentId = getArguments() != null ? getArguments().getString("appointmentId") : null;

        // Load price from Firebase
        DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        appointmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String walkerId = snapshot.child("dogWalkerId").getValue(String.class);
                if (walkerId != null) {
                    DatabaseReference walkerRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId);
                    walkerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot walkerSnapshot) {
                            price = walkerSnapshot.child("pricePerHour").getValue(Float.class);
                            if (price != null) {
                                paymentAmount.setText("Amount: USD " + price);
                            } else {
                                paymentAmount.setText("Amount: USD 0.0"); // Fallback value if price is null
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Handle error
                            paymentAmount.setText("Amount: Error");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
                paymentAmount.setText("Amount: Error");
            }
        });

        confirmPaymentButton.setOnClickListener(v -> {
            PaymentConfirmationFragment fragment = new PaymentConfirmationFragment();
            Bundle args = new Bundle();
            args.putString("appointmentId", appointmentId);
            // Pass price as a float, default to 0.0f if price is null
            args.putFloat("price", price != null ? price : 0.0f);
            fragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}