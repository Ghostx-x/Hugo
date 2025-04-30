package com.example.hugo.bottomnavbar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "LocationFragment";
    private GoogleMap mMap;
    private DatabaseReference usersRef;
    private BottomNavigationView bottomNavigationView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Check Google Play Services
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services error: " + resultCode);
            Toast.makeText(getContext(), "Google Play Services not available", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "Google Play Services available");

        // Ensure BottomNavigationView is visible
        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "BottomNavigationView not found");
        }

        // Initialize Firebase reference
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found");
            Toast.makeText(getContext(), "Error loading map", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady called");
        mMap = googleMap;

        // Fetch and display user locations
        fetchUsersAndAddMarkers();
    }

    private void fetchUsersAndAddMarkers() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Fetching users from Firebase");
                mMap.clear(); // Clear existing markers

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                int markerCount = 0;

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String userId = userSnap.getKey();
                    String username = userSnap.child("name").getValue(String.class);
                    String userType = userSnap.child("userType").getValue(String.class);
                    String locationName = userSnap.child("locationName").getValue(String.class);
                    Double latitude = userSnap.child("latitude").getValue(Double.class);
                    Double longitude = userSnap.child("longitude").getValue(Double.class);
                    String profileImageBase64 = userSnap.child("profileImageBase64").getValue(String.class);

                    // Log user data
                    Log.d(TAG, "User: " + userId + ", username=" + username + ", userType=" + userType +
                            ", location=" + locationName + ", lat=" + latitude + ", lng=" + longitude +
                            ", hasProfileImage=" + (profileImageBase64 != null));

                    // Add a marker if latitude and longitude are available
                    if (latitude != null && longitude != null && username != null) {
                        LatLng location = new LatLng(latitude, longitude);

                        // Prepare the marker's info window content
                        StringBuilder snippetBuilder = new StringBuilder();
                        if (userType != null) {
                            snippetBuilder.append(userType);
                            if (locationName != null) {
                                snippetBuilder.append(" - ").append(locationName);
                            }
                        } else if (locationName != null) {
                            snippetBuilder.append(locationName);
                        } else {
                            snippetBuilder.append("Lat: ").append(latitude).append(", Lng: ").append(longitude);
                        }

                        // Create the marker
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(location)
                                .title(username)
                                .snippet(snippetBuilder.toString());

                        // Add custom circular icon if profile picture is available
                        if (profileImageBase64 != null) {
                            try {
                                byte[] decodedBytes = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                // Resize the bitmap to a larger size for the marker (80x80 pixels)
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, false);
                                // Transform to circular shape
                                Bitmap circularBitmap = getCircularBitmap(scaledBitmap);
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(circularBitmap));
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to decode or transform profile image for user " + userId + ": " + e.getMessage(), e);
                                // Fallback to default marker icon if decoding or transformation fails
                            }
                        }

                        // Add the marker to the map
                        mMap.addMarker(markerOptions);
                        boundsBuilder.include(location);
                        markerCount++;
                    } else {
                        Log.w(TAG, "Missing location data for user: " + userId);
                    }
                }

                // Adjust the camera to show all markers
                if (markerCount > 0) {
                    LatLngBounds bounds = boundsBuilder.build();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                    Log.d(TAG, "Users loaded: " + markerCount);
                } else {
                    Log.w(TAG, "No users with valid locations found in Firebase");
                    Toast.makeText(getContext(), "No user locations found", Toast.LENGTH_SHORT).show();
                    // Default location (San Francisco)
                    LatLng defaultLocation = new LatLng(37.7749, -122.4194);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load locations: " + error.getMessage(), error.toException());
                Toast.makeText(getContext(), "Failed to load locations", Toast.LENGTH_SHORT).show();
                // Default location on failure
                LatLng defaultLocation = new LatLng(37.7749, -122.4194);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
            }
        });
    }

    // Helper method to transform a bitmap into a circular shape
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.Rect rect = new android.graphics.Rect(0, 0, size, size);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(android.graphics.Color.WHITE);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
    }
}