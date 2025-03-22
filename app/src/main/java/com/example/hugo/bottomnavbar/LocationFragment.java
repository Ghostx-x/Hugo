package com.example.hugo.bottomnavbar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BottomNavigationView bottomNavigationView;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location, container, false);


        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);


        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if (checkLocationPermissions()) {
            getLastKnownLocation();
        } else {
            requestLocationPermissions();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (checkLocationPermissions()) {
            enableMyLocation();
        } else {
            requestLocationPermissions();
        }

        getLastKnownLocation();
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                enableMyLocation();
                getLastKnownLocation();
            } else {
                Toast.makeText(requireContext(), "Location permissions are required to show your location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableMyLocation() {
        if (checkLocationPermissions()) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLastKnownLocation() {
        if (checkLocationPermissions()) {
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), location -> {
                            if (location != null) {
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                updateMap(userLocation);
                            } else {
                                Toast.makeText(requireContext(), "Unable to get your location", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (SecurityException e) {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateMap(LatLng userLocation) {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
        }
    }
}