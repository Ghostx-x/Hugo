package com.example.hugo.bottomnavbar;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference usersRef;
    private BottomNavigationView bottomNavigationView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Fetch and display user locations
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String username = userSnap.child("name").getValue(String.class);
                    String locationName = userSnap.child("locationName").getValue(String.class);
                    Double latitude = userSnap.child("latitude").getValue(Double.class);
                    Double longitude = userSnap.child("longitude").getValue(Double.class);

                    if (latitude != null && longitude != null && username != null) {
                        LatLng location = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(username)
                                .snippet(locationName));
                    }
                }

                // Center map on first user or default location
                if (snapshot.getChildrenCount() > 0) {
                    DataSnapshot firstUser = snapshot.getChildren().iterator().next();
                    Double lat = firstUser.child("latitude").getValue(Double.class);
                    Double lon = firstUser.child("longitude").getValue(Double.class);
                    if (lat != null && lon != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 10));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load locations", Toast.LENGTH_SHORT).show();
            }
        });
    }
}