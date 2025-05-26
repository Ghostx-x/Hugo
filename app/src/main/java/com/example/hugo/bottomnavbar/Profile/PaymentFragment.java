package com.example.hugo.bottomnavbar.Profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    private Float price;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);

        paymentAmount = view.findViewById(R.id.payment_amount);
        confirmPaymentButton = view.findViewById(R.id.confirm_payment_button);
        appointmentId = getArguments() != null ? getArguments().getString("appointmentId") : null;

        if (appointmentId == null) {
            Log.w("PaymentFragment", "Invalid appointmentId: null");
            Toast.makeText(getContext(), "Invalid appointment ID", Toast.LENGTH_SHORT).show();
            paymentAmount.setText("Amount: Error");
            confirmPaymentButton.setEnabled(false);
            return view;
        }

        Log.d("PaymentFragment", "Fetching appointment data for appointmentId: " + appointmentId);
        DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
        appointmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w("PaymentFragment", "Appointment not found for appointmentId: " + appointmentId);
                    paymentAmount.setText("Amount: Not set");
                    confirmPaymentButton.setEnabled(false);
                    Toast.makeText(getContext(), "Appointment not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String walkerId = snapshot.child("dogWalkerId").getValue(String.class);
                if (walkerId == null) {
                    Log.w("PaymentFragment", "No dogWalkerId found for appointment: " + appointmentId);
                    paymentAmount.setText("Amount: Not set");
                    confirmPaymentButton.setEnabled(false);
                    Toast.makeText(getContext(), "Service provider not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d("PaymentFragment", "Fetching price for walkerId: " + walkerId);
                DatabaseReference walkerRef = FirebaseDatabase.getInstance().getReference("Users").child(walkerId);
                walkerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot walkerSnapshot) {
                        Double priceDouble = walkerSnapshot.child("pricePerHour").getValue(Double.class);
                        price = priceDouble != null ? priceDouble.floatValue() : null;
                        if (price == null) {
                            price = walkerSnapshot.child("pricePerHour").getValue(Float.class);
                        }
                        if (price != null && price > 0) {
                            paymentAmount.setText(String.format("Amount: %.2f AMD", price));
                            Log.d("PaymentFragment", "Price set to: " + price + " AMD for walker: " + walkerId);
                            confirmPaymentButton.setEnabled(true);
                        } else {
                            paymentAmount.setText("Amount: Not set");
                            confirmPaymentButton.setEnabled(false);
                            Log.w("PaymentFragment", "Price not set or invalid for walker: " + walkerId + ", price: " + price);
                            Toast.makeText(getContext(), "Price not set by service provider", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PaymentFragment", "Failed to fetch walker data: " + error.getMessage());
                        paymentAmount.setText("Amount: Error");
                        confirmPaymentButton.setEnabled(false);
                        Toast.makeText(getContext(), "Failed to load price", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PaymentFragment", "Failed to fetch appointment data: " + error.getMessage());
                paymentAmount.setText("Amount: Error");
                confirmPaymentButton.setEnabled(false);
                Toast.makeText(getContext(), "Failed to load appointment", Toast.LENGTH_SHORT).show();
            }
        });

        confirmPaymentButton.setOnClickListener(v -> {
            if (price == null || price <= 0) {
                Toast.makeText(getContext(), "Cannot confirm payment: Price not set", Toast.LENGTH_SHORT).show();
                return;
            }
            PaymentConfirmationFragment fragment = new PaymentConfirmationFragment();
            Bundle args = new Bundle();
            args.putString("appointmentId", appointmentId);
            args.putFloat("price", price);
            fragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}