    package com.example.hugo.bottomnavbar.Profile;
    
    import android.app.Activity;
    import android.app.DatePickerDialog;
    import android.content.Context;
    import android.content.Intent;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.drawable.BitmapDrawable;
    import android.net.Uri;
    import android.os.Bundle;
    import android.provider.MediaStore;
    import android.util.Base64;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ImageButton;
    import android.widget.ImageView;
    import android.widget.LinearLayout;
    import android.widget.ProgressBar;
    import android.widget.RadioButton;
    import android.widget.RadioGroup;
    import android.widget.TextView;
    import android.widget.Toast;
    
    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.core.content.ContextCompat;
    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    
    import com.example.hugo.MainActivity;
    import com.example.hugo.R;
    import com.google.android.material.bottomnavigation.BottomNavigationView;
    import com.google.android.material.imageview.ShapeableImageView;
    import com.google.android.material.shape.CornerFamily;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    
    import java.io.ByteArrayOutputStream;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    
    public class DogFragment extends Fragment {
    
        private static final String TAG = "DogFragment";
        private RecyclerView dogRecyclerView;
        private LinearLayout addDogForm, dogCountContainer;
        private TextView noDogsMessage, txtDogCount;
        private ImageButton btnDecrease, btnIncrease;
        private Button addDogsButton, saveDogsButton;
        private ProgressBar loadingIndicator;
        private FirebaseAuth mAuth;
        private DatabaseReference databaseRef;
        private DatabaseReference dogsRef;
        private List<Dog> dogList;
        private DogAdapter dogAdapter;
        private ActivityResultLauncher<Intent> dogImagePickerLauncher;
        private final String userId;
        private int dogCount = 0;
        private LinearLayout dogDetailsContainer;
    
        public DogFragment() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            this.userId = (user != null) ? user.getUid() : null;
        }
    
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "onCreate called");
            mAuth = FirebaseAuth.getInstance();
            if (userId == null) {
                Log.w(TAG, "No authenticated user, redirecting to login");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Please sign in", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(getContext(), com.example.hugo.LoginActivity.class);
                    startActivity(loginIntent);
                }
                if (getActivity() != null) {
                    getActivity().finish();
                }
                return;
            }
            databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            dogsRef = databaseRef.child("dogs");
    
            dogImagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                        int maxSize = 400;
                        bitmap = Bitmap.createScaledBitmap(bitmap, maxSize, maxSize, true);
                        ShapeableImageView selectedImageView = (ShapeableImageView) getView().findViewWithTag("image_picker_active");
                        if (selectedImageView != null) {
                            selectedImageView.setImageBitmap(bitmap);
                            selectedImageView.setTag("image_selected_" + selectedImageView.getTag().toString().replace("image_picker_", ""));
                        }
                        Log.d(TAG, "Dog image selected and converted to Bitmap: " + imageUri);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load dog image: " + e.getMessage(), e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Log.d(TAG, "onCreateView called");
            return inflater.inflate(R.layout.fragment_dog, container, false);
        }
    
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            Log.d(TAG, "onViewCreated called");
    
            // Initialize views
            dogRecyclerView = view.findViewById(R.id.dog_recycler_view);
            addDogForm = view.findViewById(R.id.add_dog_form);
            noDogsMessage = view.findViewById(R.id.no_dogs_message);
            dogCountContainer = view.findViewById(R.id.dog_count_container);
            btnDecrease = view.findViewById(R.id.btnDecrease);
            btnIncrease = view.findViewById(R.id.btnIncrease);
            txtDogCount = view.findViewById(R.id.txtDogCount);
            addDogsButton = view.findViewById(R.id.add_dogs_button);
            saveDogsButton = view.findViewById(R.id.save_dogs_button);
            loadingIndicator = view.findViewById(R.id.dog_loading_indicator);
            dogDetailsContainer = view.findViewById(R.id.dog_details_container);
    
            // Initialize RecyclerView for dogs
            dogList = new ArrayList<>();
            dogAdapter = new DogAdapter(dogList, this::base64ToBitmap);
            dogRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
            dogRecyclerView.setAdapter(dogAdapter);
    
            // Set dog count controls
            btnDecrease.setOnClickListener(v -> {
                if (dogCount > 0) {
                    dogCount--;
                    txtDogCount.setText(String.valueOf(dogCount));
                }
            });
            btnIncrease.setOnClickListener(v -> {
                dogCount++;
                txtDogCount.setText(String.valueOf(dogCount));
            });
            addDogsButton.setOnClickListener(v -> {
                if (dogCount > 0) {
                    dogDetailsContainer.removeAllViews();
                    generateDogDetailsForm(dogCount);
                    dogCountContainer.setVisibility(View.GONE);
                    addDogsButton.setVisibility(View.GONE);
                    saveDogsButton.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getContext(), "Please select at least 1 dog", Toast.LENGTH_SHORT).show();
                }
            });
            saveDogsButton.setOnClickListener(v -> saveDogDetails());
    
            // Initialize BottomNavigationView
            if (getActivity() != null) {
                BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNavigationView != null) {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                    bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                } else {
                    Log.w(TAG, "BottomNavigationView not found");
                }
            }
    
            checkFirstTimeDogOwner();
        }
    
        private void checkFirstTimeDogOwner() {
            if (userId != null) {
                databaseRef.child("userType").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String userType = snapshot.getValue(String.class);
                        if ("Dog Owner".equals(userType)) {
                            dogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        // New dog owner with no dogs, show form
                                        noDogsMessage.setVisibility(View.GONE);
                                        dogCountContainer.setVisibility(View.VISIBLE);
                                        addDogForm.setVisibility(View.VISIBLE);
                                        dogRecyclerView.setVisibility(View.GONE);
                                    } else {
                                        loadDogs();
                                    }
                                }
    
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Failed to check dogs: " + error.getMessage());
                                }
                            });
                        } else {
                            loadDogs();
                        }
                    }
    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to check user type: " + error.getMessage());
                    }
                });
            }
        }
    
        private void generateDogDetailsForm(int dogCount) {
            for (int i = 1; i <= dogCount; i++) {
                TextView title = new TextView(requireContext());
                title.setText("Your " + ordinal(i) + " dog's details:");
                title.setTextSize(20);
                title.setPadding(10, 20, 10, 10);
                title.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                dogDetailsContainer.addView(title);
    
                ShapeableImageView dogImage = new ShapeableImageView(requireContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(200, 200);
                layoutParams.setMargins(10, 10, 10, 10);
                dogImage.setLayoutParams(layoutParams);
                dogImage.setImageResource(R.drawable.ic_add_photo);
                dogImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                dogImage.setShapeAppearanceModel(
                        dogImage.getShapeAppearanceModel()
                                .toBuilder()
                                .setAllCorners(CornerFamily.ROUNDED, 100)
                                .build()
                );
                dogImage.setTag("image_picker_" + i);
                dogImage.setOnClickListener(v -> {
                    ((ShapeableImageView) v).setTag("image_picker_active");
                    openImagePicker();
                });
                dogDetailsContainer.addView(dogImage);
    
                addQuestionField(dogDetailsContainer, "Name:", i);
                addQuestionField(dogDetailsContainer, "Breed:", i);
                addGenderRadioGroup(dogDetailsContainer, i);
                addBirthdayField(dogDetailsContainer, i);
            }
        }
    
        private void openImagePicker() {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            dogImagePickerLauncher.launch(intent);
        }
    
        private void addQuestionField(LinearLayout parent, String labelText, int dogIndex) {
            TextView label = new TextView(requireContext());
            label.setText(labelText);
            label.setTextSize(16);
            label.setPadding(10, 10, 10, 5);
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            parent.addView(label);
    
            EditText inputField = new EditText(requireContext());
            inputField.setId(View.generateViewId());
            inputField.setHint(labelText);
            inputField.setTextSize(20);
            inputField.setPadding(12, 12, 12, 12);
            inputField.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            inputField.setTag("dog_" + dogIndex + "_" + labelText.replace(" ", "_").toLowerCase());
            parent.addView(inputField);
        }
    
        private void addGenderRadioGroup(LinearLayout parent, int dogIndex) {
            TextView label = new TextView(requireContext());
            label.setText("Gender:");
            label.setTextSize(16);
            label.setPadding(10, 10, 10, 5);
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            parent.addView(label);
    
            RadioGroup genderGroup = new RadioGroup(requireContext());
            genderGroup.setOrientation(RadioGroup.HORIZONTAL);
            genderGroup.setId(View.generateViewId());
            genderGroup.setTag("dog_" + dogIndex + "_gender");
    
            RadioButton male = new RadioButton(requireContext());
            male.setText("Male");
            male.setId(View.generateViewId());
            genderGroup.addView(male);
    
            RadioButton female = new RadioButton(requireContext());
            female.setText("Female");
            female.setId(View.generateViewId());
            genderGroup.addView(female);
    
            parent.addView(genderGroup);
        }
    
        private void addBirthdayField(LinearLayout parent, int dogIndex) {
            TextView label = new TextView(requireContext());
            label.setText("Birthday (DD/MM/YYYY):");
            label.setTextSize(16);
            label.setPadding(10, 10, 10, 5);
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            parent.addView(label);
    
            TextView birthdayField = new TextView(requireContext());
            birthdayField.setId(View.generateViewId());
            birthdayField.setHint("Select a date");
            birthdayField.setTextSize(20);
            birthdayField.setPadding(12, 12, 12, 12);
            birthdayField.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            birthdayField.setTag("dog_" + dogIndex + "_birthday");
            birthdayField.setOnClickListener(v -> showDatePicker((TextView) v));
            parent.addView(birthdayField);
        }
    
        private void showDatePicker(TextView birthdayField) {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
    
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        birthdayField.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        }
    
        private void saveDogDetails() {
            showLoadingIndicator();
            List<Map<String, Object>> dogsData = new ArrayList<>();
            for (int i = 1; i <= dogCount; i++) {
                Map<String, Object> dogData = new HashMap<>();
                dogData.put("name", ((EditText) dogDetailsContainer.findViewWithTag("dog_" + i + "_name")).getText().toString());
                dogData.put("breed", ((EditText) dogDetailsContainer.findViewWithTag("dog_" + i + "_breed")).getText().toString());
                RadioGroup genderGroup = dogDetailsContainer.findViewWithTag("dog_" + i + "_gender");
                dogData.put("gender", genderGroup != null && genderGroup.getCheckedRadioButtonId() != -1 ?
                        ((RadioButton) genderGroup.findViewById(genderGroup.getCheckedRadioButtonId())).getText().toString() : "");
                dogData.put("birthday", ((TextView) dogDetailsContainer.findViewWithTag("dog_" + i + "_birthday")).getText().toString());
                ShapeableImageView dogImage = dogDetailsContainer.findViewWithTag("image_picker_" + i);
                if (dogImage != null && dogImage.getTag() != null && dogImage.getTag().toString().startsWith("image_selected_")) {
                    dogData.put("imageBase64", bitmapToBase64(((BitmapDrawable) dogImage.getDrawable()).getBitmap()));
                }
                if (!dogData.get("name").toString().isEmpty()) {
                    dogsData.add(dogData);
                }
            }
    
            final int lastIndex = dogsData.size() - 1; // Final variable to use in lambda
            for (int i = 0; i < dogsData.size(); i++) {
                Map<String, Object> dog = dogsData.get(i);
                String dogId = dogsRef.push().getKey();
                int currentIndex = i; // This is effectively final

                dogsRef.child(dogId).setValue(dog)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Dog saved to Firebase: " + dog.get("name"));
                            if (currentIndex == lastIndex) {
                                Toast.makeText(getContext(), "Dogs saved successfully!", Toast.LENGTH_SHORT).show();
                                dogDetailsContainer.removeAllViews();
                                dogCountContainer.setVisibility(View.GONE);
                                addDogsButton.setVisibility(View.GONE);
                                saveDogsButton.setVisibility(View.GONE);
                                loadDogs();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to save dog: " + e.getMessage());
                            Toast.makeText(getContext(), "Failed to save dog.", Toast.LENGTH_SHORT).show();
                            hideLoadingIndicator();
                        });
            }

        }
    
        private void loadDogs() {
            Log.d(TAG, "Loading dogs for UID: " + userId);
            showLoadingIndicator();
            dogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (getActivity() == null) {
                        Log.w(TAG, "Activity is null, cannot update UI");
                        hideLoadingIndicator();
                        return;
                    }
    
                    List<Dog> dogs = new ArrayList<>();
                    for (DataSnapshot dogSnapshot : snapshot.getChildren()) {
                        String name = dogSnapshot.child("name").getValue(String.class);
                        String breed = dogSnapshot.child("breed").getValue(String.class);
                        String gender = dogSnapshot.child("gender").getValue(String.class);
                        String birthday = dogSnapshot.child("birthday").getValue(String.class);
                        String imageBase64 = dogSnapshot.child("imageBase64").getValue(String.class);
                        if (name != null) {
                            dogs.add(new Dog(name, breed, gender, birthday, imageBase64));
                        }
                    }
    
                    if (dogs.isEmpty()) {
                        noDogsMessage.setVisibility(View.VISIBLE);
                        addDogForm.setVisibility(View.GONE);
                        dogRecyclerView.setVisibility(View.GONE);
                    } else {
                        dogList.clear();
                        dogList.addAll(dogs);
                        dogAdapter.notifyDataSetChanged();
                        noDogsMessage.setVisibility(View.GONE);
                        addDogForm.setVisibility(View.GONE);
                        dogRecyclerView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Updated UI with " + dogs.size() + " dogs");
                    }
                    hideLoadingIndicator();
                }
    
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load dogs: " + error.getMessage());
                    Toast.makeText(getContext(), "Failed to load dogs", Toast.LENGTH_SHORT).show();
                    hideLoadingIndicator();
                }
            });
        }
    
        private String bitmapToBase64(Bitmap bitmap) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        }
    
        private Bitmap base64ToBitmap(String base64Str) {
            try {
                byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode Base64 to Bitmap: " + e.getMessage(), e);
                return null;
            }
        }
    
        private String ordinal(int number) {
            if (number == 1) return "First";
            if (number == 2) return "Second";
            if (number == 3) return "Third";
            return number + "th";
        }
    
        private void showLoadingIndicator() {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.VISIBLE);
                Log.d(TAG, "Showing loading indicator");
            }
        }
    
        private void hideLoadingIndicator() {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.GONE);
                Log.d(TAG, "Hiding loading indicator");
            }
        }
    
        public static class Dog {
            private String name;
            private String breed;
            private String gender;
            private String birthday;
            private String imageBase64;
    
            public Dog(String name, String breed, String gender, String birthday, String imageBase64) {
                this.name = name;
                this.breed = breed;
                this.gender = gender;
                this.birthday = birthday;
                this.imageBase64 = imageBase64;
            }
    
            public String getName() { return name; }
            public String getBreed() { return breed; }
            public String getGender() { return gender; }
            public String getBirthday() { return birthday; }
            public String getImageBase64() { return imageBase64; }
        }
    
        public static class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {
            private List<Dog> dogList;
            private java.util.function.Function<String, Bitmap> base64ToBitmapFunction;
    
            public DogAdapter(List<Dog> dogList, java.util.function.Function<String, Bitmap> base64ToBitmapFunction) {
                this.dogList = dogList;
                this.base64ToBitmapFunction = base64ToBitmapFunction;
            }
    
            @NonNull
            @Override
            public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dog, parent, false);
                return new DogViewHolder(view);
            }
    
            @Override
            public void onBindViewHolder(@NonNull DogViewHolder holder, int position) {
                Dog dog = dogList.get(position);
                holder.nameText.setText(dog.getName());
                holder.breedText.setText(dog.getBreed());
                holder.genderText.setText(dog.getGender());
                holder.birthdayText.setText(dog.getBirthday());
                if (dog.getImageBase64() != null && !dog.getImageBase64().isEmpty()) {
                    Bitmap bitmap = base64ToBitmapFunction.apply(dog.getImageBase64());
                    if (bitmap != null) {
                        holder.dogImage.setImageBitmap(bitmap);
                    } else {
                        holder.dogImage.setImageResource(R.drawable.ic_dog_placeholder);
                    }
                } else {
                    holder.dogImage.setImageResource(R.drawable.ic_dog_placeholder);
                }
            }
    
            @Override
            public int getItemCount() {
                return dogList.size();
            }
    
            static class DogViewHolder extends RecyclerView.ViewHolder {
                ShapeableImageView dogImage;
                TextView nameText, breedText, genderText, birthdayText;
    
                DogViewHolder(@NonNull View itemView) {
                    super(itemView);
                    dogImage = itemView.findViewById(R.id.dog_image);
                    nameText = itemView.findViewById(R.id.dog_name);
                    breedText = itemView.findViewById(R.id.dog_breed);
                    genderText = itemView.findViewById(R.id.dog_gender);
                    birthdayText = itemView.findViewById(R.id.dog_birthday);
                    dogImage.setShapeAppearanceModel(
                            dogImage.getShapeAppearanceModel()
                                    .toBuilder()
                                    .setAllCorners(CornerFamily.ROUNDED, 50f)
                                    .build());
                }
            }
        }
    }