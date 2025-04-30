package com.example.hugo.bottomnavbar.Profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.hugo.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapLocationActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerDragListener {

    private static final String TAG = "MapLocationActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap myMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker selectedLocationMarker;
    private Button confirmLocationButton;
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        // Check Google Play Services
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services error: " + resultCode);
            Toast.makeText(this, "Google Play Services not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Google Play Services available");

        try {
            setContentView(R.layout.activity_map_location);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading layout", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        confirmLocationButton = findViewById(R.id.confirm_location_button);
        if (confirmLocationButton == null) {
            Log.e(TAG, "Confirm location button not found in layout");
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found");
            Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        confirmLocationButton.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLatLng.latitude);
                resultIntent.putExtra("longitude", selectedLatLng.longitude);
                setResult(RESULT_OK, resultIntent);
                Log.d(TAG, "Location confirmed: lat=" + selectedLatLng.latitude + ", lng=" + selectedLatLng.longitude);
            } else {
                setResult(RESULT_CANCELED);
                Log.d(TAG, "No location selected");
            }
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady called");
        myMap = googleMap;
        myMap.setOnMarkerDragListener(this);

        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            return;
        }

        try {
            myMap.setMyLocationEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable my location: " + e.getMessage(), e);
            Toast.makeText(this, "Error enabling location", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                selectedLatLng = currentLocation;

                // Add a draggable marker at the current location
                selectedLocationMarker = myMap.addMarker(new MarkerOptions()
                        .position(currentLocation)
                        .title("Your Location")
                        .draggable(true));

                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                Log.d(TAG, "Current location: " + currentLocation);
            } else {
                Log.w(TAG, "Last known location is null");
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                // Default to a location (e.g., Sydney)
                selectedLatLng = new LatLng(-33.8688, 151.2093);
                selectedLocationMarker = myMap.addMarker(new MarkerOptions()
                        .position(selectedLatLng)
                        .title("Default Location")
                        .draggable(true));
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get location: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
            // Default to a location (e.g., Sydney)
            selectedLatLng = new LatLng(-33.8688, 151.2093);
            selectedLocationMarker = myMap.addMarker(new MarkerOptions()
                    .position(selectedLatLng)
                    .title("Default Location")
                    .draggable(true));
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Log.w(TAG, "Location permission denied");
                Toast.makeText(this, "Location permission is denied, please allow the permission", Toast.LENGTH_LONG).show();
                // Default to a location (e.g., Sydney)
                selectedLatLng = new LatLng(-33.8688, 151.2093);
                selectedLocationMarker = myMap.addMarker(new MarkerOptions()
                        .position(selectedLatLng)
                        .title("Default Location")
                        .draggable(true));
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            }
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.d(TAG, "Marker drag started: " + marker.getPosition());
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        // Optional: Update UI during drag
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        selectedLatLng = marker.getPosition();
        Log.d(TAG, "Marker drag ended: " + selectedLatLng);
    }
}