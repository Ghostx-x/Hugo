package com.example.hugo.bottomnavbar.Profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Toast.makeText(this, "Google Play Services not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            setContentView(R.layout.activity_map_location);
        } catch (Exception e) {
            Toast.makeText(this, "Error loading layout", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        confirmLocationButton = findViewById(R.id.confirm_location_button);
        if (confirmLocationButton == null) {
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
            Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        confirmLocationButton.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLatLng.latitude);
                resultIntent.putExtra("longitude", selectedLatLng.longitude);
                String placeName = getPlaceName(selectedLatLng.latitude, selectedLatLng.longitude);
                resultIntent.putExtra("locationName", placeName != null ? placeName : "Lat: " + selectedLatLng.latitude + ", Lng: " + selectedLatLng.longitude);
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        myMap.setOnMarkerDragListener(this);

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
            return;
        }

        try {
            myMap.setMyLocationEnabled(true);
        } catch (Exception e) {
            Toast.makeText(this, "Error enabling location", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                selectedLocationMarker = myMap.addMarker(new MarkerOptions()
                        .position(selectedLatLng)
                        .title("Your Location")
                        .draggable(true));
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            } else {
                selectedLatLng = new LatLng(-33.8688, 151.2093);
                selectedLocationMarker = myMap.addMarker(new MarkerOptions()
                        .position(selectedLatLng)
                        .title("Default Location")
                        .draggable(true));
                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
            }
        }).addOnFailureListener(e -> {
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
    public void onMarkerDragStart(Marker marker) {}

    @Override
    public void onMarkerDrag(Marker marker) {}

    @Override
    public void onMarkerDragEnd(Marker marker) {
        selectedLatLng = marker.getPosition();
    }

    private String getPlaceName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressString = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) addressString.append(", ");
                    addressString.append(address.getAddressLine(i));
                }
                return addressString.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed: " + e.getMessage(), e);
        }
        return null;
    }
}