package com.example.hugo.bottomnavbar.Profile;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditDogFragment extends Fragment {

    private static final String TAG = "EditDogFragment";
    private static final String ARG_DOG = "dog";
    private static final String ARG_KEY = "key";
    private EditText editName, editDescription;
    private AutoCompleteTextView editBreed;
    private TextView editBirthDate;
    private RadioGroup editGenderGroup, editSizeGroup;
    private ImageView backArrow;
    private Button pickImageButton, saveButton;
    private ShapeableImageView dogImagePreview;
    private Dog dog;
    private String key;
    private DatabaseReference dogsRef;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String newImageBase64;

    public static EditDogFragment newInstance(Dog dog, String key) {
        EditDogFragment fragment = new EditDogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DOG, dog);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isAdded()) return;
        Log.d(TAG, "onCreate started");

        if (getArguments() != null) {
            dog = (Dog) getArguments().getSerializable(ARG_DOG);
            key = getArguments().getString(ARG_KEY);
        }
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
            return;
        }
        dogsRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).child("dogs");

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (!isAdded()) return;
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                try {
                    InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    dogImagePreview.setImageBitmap(bitmap);
                    dogImagePreview.setVisibility(View.VISIBLE);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    newImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                    Log.d(TAG, "New image selected and encoded");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load new image: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        return inflater.inflate(R.layout.fragment_edit_dog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!isAdded()) return;
        Log.d(TAG, "onViewCreated started");

        editName = view.findViewById(R.id.dog_name_input);
        editBreed = view.findViewById(R.id.dog_breed_input);
        editBirthDate = view.findViewById(R.id.dog_birth_date_input);
        editGenderGroup = view.findViewById(R.id.edit_dog_gender_group);
        editSizeGroup = view.findViewById(R.id.dog_size_group);
        editDescription = view.findViewById(R.id.dog_description_input);
        dogImagePreview = view.findViewById(R.id.dog_image_preview);
        pickImageButton = view.findViewById(R.id.pick_image_button);
        saveButton = view.findViewById(R.id.save_button);
        backArrow = view.findViewById(R.id.back_arrow);

        if (editName == null || editBreed == null || editBirthDate == null || editGenderGroup == null ||
                editSizeGroup == null || editDescription == null || dogImagePreview == null ||
                pickImageButton == null || saveButton == null || backArrow == null) {
            Log.e(TAG, "One or more UI elements not found");
            if (isAdded()) {
                Toast.makeText(requireContext(), "UI initialization failed", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
            return;
        }

        // Set up AutoCompleteTextView for breed
        String[] breeds = getResources().getStringArray(R.array.dog_breeds);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, breeds);
        editBreed.setAdapter(adapter);

        if (dog != null) {
            editName.setText(dog.getName());
            editBreed.setText(dog.getBreed(), false);
            editBirthDate.setText(dog.getBirthDate());
            editDescription.setText(dog.getDescription() != null ? dog.getDescription() : "");
            if ("Male".equalsIgnoreCase(dog.getGender())) {
                editGenderGroup.check(R.id.gender_male);
            } else if ("Female".equalsIgnoreCase(dog.getGender())) {
                editGenderGroup.check(R.id.gender_female);
            }
            if ("Small (0-4 kg)".equalsIgnoreCase(dog.getSize())) {
                editSizeGroup.check(R.id.size_small);
            } else if ("Medium (5-15 kg)".equalsIgnoreCase(dog.getSize())) {
                editSizeGroup.check(R.id.size_medium);
            } else if ("Large (16-40 kg)".equalsIgnoreCase(dog.getSize())) {
                editSizeGroup.check(R.id.size_large);
            }
            if (dog.getImageBase64() != null && !dog.getImageBase64().isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(dog.getImageBase64(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        dogImagePreview.setImageBitmap(bitmap);
                        dogImagePreview.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load dog image: " + e.getMessage(), e);
                    dogImagePreview.setImageResource(R.drawable.ic_profile);
                }
            } else {
                dogImagePreview.setImageResource(R.drawable.ic_profile);
            }
        } else {
            Log.w(TAG, "No dog data provided");
            if (isAdded()) {
                Toast.makeText(requireContext(), "No dog data available", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
            return;
        }

        editBirthDate.setOnClickListener(v -> {
            if (!isAdded()) return;
            Calendar calendar = Calendar.getInstance();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                if (dog.getBirthDate() != null) {
                    calendar.setTime(sdf.parse(dog.getBirthDate()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse birth date: " + e.getMessage());
            }
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year1, month1, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month1 + 1, year1);
                        editBirthDate.setText(selectedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        pickImageButton.setOnClickListener(v -> {
            if (!isAdded()) return;
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        saveButton.setOnClickListener(v -> saveDogChanges());

        backArrow.setOnClickListener(v -> {
            if (!isAdded()) return;
            getParentFragmentManager().popBackStack();
        });
    }

    private void saveDogChanges() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot save");
            return;
        }
        if (dog == null || key == null) {
            Toast.makeText(requireContext(), "No dog data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editName.getText().toString().trim();
        String breed = editBreed.getText().toString().trim();
        String birthDate = editBirthDate.getText().toString().trim();
        int selectedGenderId = editGenderGroup.getCheckedRadioButtonId();
        String gender = selectedGenderId == R.id.gender_male ? "Male" : selectedGenderId == R.id.gender_female ? "Female" : null;
        int selectedSizeId = editSizeGroup.getCheckedRadioButtonId();
        String size = selectedSizeId == R.id.size_small ? "Small (0-4 kg)" :
                selectedSizeId == R.id.size_medium ? "Medium (5-15 kg)" :
                        selectedSizeId == R.id.size_large ? "Large (16-40 kg)" : null;
        String description = editDescription.getText().toString().trim();

        if (name.isEmpty() || breed.isEmpty() || birthDate.isEmpty() || gender == null || size == null) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> dogData = new HashMap<>();
        dogData.put("name", name);
        dogData.put("breed", breed);
        dogData.put("birthDate", birthDate);
        dogData.put("gender", gender);
        dogData.put("size", size);
        dogData.put("description", description.isEmpty() ? null : description);
        dogData.put("imageBase64", newImageBase64 != null ? newImageBase64 : dog.getImageBase64());

        dogsRef.child(key)
                .setValue(dogData)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Log.d(TAG, "Dog updated successfully");
                    Toast.makeText(requireContext(), "Dog updated", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to update dog: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to update dog: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}