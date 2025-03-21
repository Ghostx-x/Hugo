package com.example.hugo;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.widget.Toast;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;

public class DogOwnerDetailsFragmentTwo extends Fragment {

    private int dogCount;
    private ImageView selectedImageView;

    public DogOwnerDetailsFragmentTwo() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dog_owner_details_two, container, false);
        Button nextButton = view.findViewById(R.id.btn_next);

        if (getArguments() != null) {
            dogCount = getArguments().getInt("dogCount", 0);
        }

        LinearLayout mainLayout = view.findViewById(R.id.questions_container);
        for (int i = 1; i <= dogCount; i++) {
            addDogQuestions(mainLayout, i, requireContext());
        }

        nextButton.setOnClickListener(v -> {
            if (areAllFieldsFilled(mainLayout)) {
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields and upload a picture of your pet before proceeding!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null && selectedImageView != null) {
                        selectedImageView.setImageURI(selectedImageUri);
                        selectedImageView.setTag("image_selected");
                    } else {
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void addDogQuestions(LinearLayout parent, int dogIndex, Context context) {
        TextView title = new TextView(context);
        title.setText("Your " + ordinal(dogIndex) + " dog's details:");
        title.setTextSize(20);
        title.setPadding(10, 20, 10, 10);
        title.setTextColor(ContextCompat.getColor(context, R.color.black));
        title.setTypeface(ResourcesCompat.getFont(context, R.font.baloo2_semibold));
        parent.addView(title);

        ShapeableImageView dogImage = new ShapeableImageView(context);
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

        dogImage.setOnClickListener(v -> {
            selectedImageView = dogImage;
            openImagePicker();
        });

        parent.addView(dogImage);
        addQuestionField(parent, "Name:", context);
        addQuestionField(parent, "Breed:", context);
        RadioOption(parent, context, "Gender:", "Male", "Female");
        addQuestionField(parent, "Birthday (DD/MM/YYYY):", context);
    }

    private boolean areAllFieldsFilled(LinearLayout parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof EditText) {
                EditText editText = (EditText) child;
                if (editText.getText().toString().trim().isEmpty()) {
                    return false;
                }
            }

            if (child instanceof ShapeableImageView) {
                ShapeableImageView imageView = (ShapeableImageView) child;
                if (imageView.getTag() == null || !imageView.getTag().equals("image_selected")) {
                    return false;
                }
            }
        }
        return true;
    }

    private String ordinal(int number) {
        if (number == 1) return "First";
        if (number == 2) return "Second";
        if (number == 3) return "Third";
        return number + "th";
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void RadioOption(LinearLayout parent, Context context, String title, String option1, String option2) {
        TextView label = new TextView(context);
        label.setText(title);
        label.setTextSize(16);
        label.setPadding(10, 10, 10, 5);
        label.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        label.setTextColor(ContextCompat.getColor(context, R.color.black));

        RadioGroup genderGroup = new RadioGroup(context);
        genderGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton option1Button = new RadioButton(context);
        option1Button.setText(option1);
        option1Button.setTextColor(ContextCompat.getColor(context, R.color.black));

        RadioButton option2Button = new RadioButton(context);
        option2Button.setText(option2);
        option2Button.setTextColor(ContextCompat.getColor(context, R.color.black));

        genderGroup.addView(option1Button);
        genderGroup.addView(option2Button);

        parent.addView(label);
        parent.addView(genderGroup);
    }


    private void addQuestionField(LinearLayout parent, String labelText, Context context) {
        TextView label = new TextView(context);
        label.setText(labelText);
        label.setTextSize(16);
        label.setPadding(10, 10, 10, 5);
        label.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        label.setTextColor(ContextCompat.getColor(context, R.color.black));


        if (labelText.equals("Birthday (DD/MM/YYYY):")) {
            TextView birthdayField = new TextView(context);
            birthdayField.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            birthdayField.setHint("Select a date");
            birthdayField.setTextSize(20);
            birthdayField.setPadding(12, 12, 12, 12);
            birthdayField.setTextColor(ContextCompat.getColor(context, R.color.black));
            birthdayField.setHintTextColor(ContextCompat.getColor(context, R.color.milky));
            birthdayField.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_input));

            birthdayField.setOnClickListener(v -> showDatePicker(birthdayField));

            parent.addView(label);
            parent.addView(birthdayField);
        } else {
            EditText inputField = new EditText(context);
            inputField.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            inputField.setHint(labelText);
            inputField.setTextSize(20);
            inputField.setPadding(12, 12, 12, 12);
            inputField.setTextColor(ContextCompat.getColor(context, R.color.black));
            inputField.setHintTextColor(ContextCompat.getColor(context, R.color.milky));
            inputField.setInputType(InputType.TYPE_CLASS_TEXT);
            inputField.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_input));

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) inputField.getLayoutParams();
            params.setMargins(0, 0, 0, 12);
            inputField.setLayoutParams(params);

            parent.addView(label);
            parent.addView(inputField);
        }
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


}