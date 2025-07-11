package com.example.hugo.bottomnavbar.Profile;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddDogFragment extends Fragment {

    private static final String TAG = "AddDogFragment";
    private FirebaseAuth mAuth;
    private DatabaseReference dogsRef;
    private EditText nameInput, descriptionInput;
    private TextView birthDateInput;
    private RadioGroup genderGroup, sizeGroup;
    private Button submitButton, pickImageButton;
    private ImageView dogImagePreview;
    private Uri imageUri;
    private ActivityResultLauncher<String> imagePicker;
    private AutoCompleteTextView breedInput;

    public AddDogFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user detected in onCreate");
            Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
            return;
        }
        dogsRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).child("dogs");

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                imageUri = uri;
                dogImagePreview.setImageURI(uri);
                dogImagePreview.setVisibility(View.VISIBLE);
                Log.d(TAG, "Image selected: " + uri);
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        return inflater.inflate(R.layout.fragment_add_dog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated started");

        nameInput = view.findViewById(R.id.dog_name_input);
        breedInput = view.findViewById(R.id.dog_breed_input);
        birthDateInput = view.findViewById(R.id.dog_birth_date_input);
        genderGroup = view.findViewById(R.id.dog_gender_group);
        sizeGroup = view.findViewById(R.id.dog_size_group);
        descriptionInput = view.findViewById(R.id.dog_description_input);
        submitButton = view.findViewById(R.id.submit_button);
        pickImageButton = view.findViewById(R.id.pick_image_button);
        dogImagePreview = view.findViewById(R.id.dog_image_preview);

        // Populate breed list
        ArrayList<String> breeds = new ArrayList<>();
        breeds.add("Labrador Retriever");
        breeds.add("German Shepherd");
        breeds.add("Golden Retriever");
        breeds.add("Bulldog");
        breeds.add("Beagle");
        breeds.add("Poodle");
        breeds.add("Rottweiler");
        breeds.add("Yorkshire Terrier");
        breeds.add("Boxer");
        breeds.add("Dachshund");
        // Add more breeds as needed
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                breeds);
        breedInput.setAdapter(adapter);
        breedInput.setThreshold(1); // Show dropdown after 1 character

        birthDateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        birthDateInput.setText(date);
                        Log.d(TAG, "Birth date selected: " + date);
                    }, year, month, day);
            datePicker.show();
        });

        ImageView backArrow = view.findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .commit();
            }
        });

        pickImageButton.setOnClickListener(v -> {
            imagePicker.launch("image/*");
            Log.d(TAG, "Pick image button clicked");
        });

        submitButton.setOnClickListener(v -> {
            Log.d(TAG, "Submit button clicked");
            String name = nameInput.getText().toString().trim();
            String breed = breedInput.getText().toString().trim();
            String birthDate = birthDateInput.getText().toString().trim();
            String gender = getSelectedGender();
            String size = getSelectedSize();
            String description = descriptionInput.getText().toString().trim();

            if (name.isEmpty() || breed.isEmpty() || birthDate.isEmpty() || gender == null || size == null) {
                Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> dogData = new HashMap<>();
            dogData.put("name", name);
            dogData.put("breed", breed);
            dogData.put("birthDate", birthDate);
            dogData.put("gender", gender);
            dogData.put("size", size);
            dogData.put("description", description);

            String dogId = dogsRef.push().getKey();
            if (dogId == null) {
                Toast.makeText(getContext(), "Failed to generate dog ID", Toast.LENGTH_SHORT).show();
                return;
            }

            if (imageUri != null) {
                try {
                    String base64Image = convertImageToBase64(imageUri);
                    dogData.put("imageBase64", base64Image);
                    saveDogData(dogId, dogData);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to encode image: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                    saveDogData(dogId, dogData);
                }
            } else {
                saveDogData(dogId, dogData);
            }
        });

        Log.d(TAG, "UI set up");
    }

    private String convertImageToBase64(Uri imageUri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] bytes = baos.toByteArray();
        String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
        if (inputStream != null) {
            inputStream.close();
        }
        return base64;
    }

    private String getSelectedGender() {
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.gender_male) return "Male";
        if (selectedId == R.id.gender_female) return "Female";
        return null;
    }

    private String getSelectedSize() {
        int selectedId = sizeGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.size_small) return "Small (0-4 kg)";
        if (selectedId == R.id.size_medium) return "Medium (5-15 kg)";
        if (selectedId == R.id.size_large) return "Large (16-40 kg)";
        return null;
    }

    private void saveDogData(String dogId, Map<String, Object> dogData) {
        dogsRef.child(dogId).setValue(dogData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dog data saved successfully");
                    Toast.makeText(getContext(), "Dog registered", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack("DogFragment", 0);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save dog data: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to register dog", Toast.LENGTH_SHORT).show();
                });
    }
}