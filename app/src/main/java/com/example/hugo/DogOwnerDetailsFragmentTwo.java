package com.example.hugo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.widget.Toast;

import com.example.hugo.bottomnavbar.Home.HomeFragment;
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

        // Set up the OnClickListener for the "Next" button
        nextButton.setOnClickListener(v -> {
            // Load the nested fragment
            FrameLayout nestedFragmentContainer = view.findViewById(R.id.nested_fragment_container);
            if (nestedFragmentContainer != null) {
                NestedFragment nestedFragment = new NestedFragment();
                FragmentTransaction nestedTransaction = getChildFragmentManager().beginTransaction();
                nestedTransaction.replace(R.id.nested_fragment_container, nestedFragment);
                nestedTransaction.addToBackStack(null); // Optional: Add to back stack
                nestedTransaction.commit();
            }

            // Navigate to the HomeFragment
            HomeFragment homeFragment = new HomeFragment();
            FragmentTransaction homeTransaction = getParentFragmentManager().beginTransaction();
            homeTransaction.replace(R.id.fragment_container, homeFragment);
            homeTransaction.addToBackStack(null); // Optional: Add to back stack
            homeTransaction.commit();
        });

        return view;
    }
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null && selectedImageView != null) {
                        selectedImageView.setImageURI(selectedImageUri);
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
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(300, 300);
        layoutParams.setMargins(10, 10, 10, 10);
        dogImage.setLayoutParams(layoutParams);
        dogImage.setImageResource(R.drawable.ic_add_photo);
        dogImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        dogImage.setShapeAppearanceModel(
                dogImage.getShapeAppearanceModel()
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, 150)
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
        SpecialNeedsOption(parent, context);
    }

    private void addQuestionField(LinearLayout parent, String labelText, Context context) {
        TextView label = new TextView(context);
        label.setText(labelText);
        label.setTextSize(16);
        label.setPadding(10, 10, 10, 5);
        label.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium)); // Set font if needed
        label.setTextColor(ContextCompat.getColor(context, R.color.black));

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

        RadioButton maleOption = new RadioButton(context);
        maleOption.setText(option1);
        maleOption.setId(View.generateViewId());
        maleOption.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        maleOption.setTextColor(ContextCompat.getColor(context, R.color.black));

        RadioButton femaleOption = new RadioButton(context);
        femaleOption.setText(option2);
        femaleOption.setId(View.generateViewId());
        femaleOption.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        femaleOption.setTextColor(ContextCompat.getColor(context, R.color.black));


        genderGroup.addView(maleOption);
        genderGroup.addView(femaleOption);

        parent.addView(label);
        parent.addView(genderGroup);
    }

    private void SpecialNeedsOption(LinearLayout parent, Context context) {
        TextView label = new TextView(context);
        label.setText("Special Needs:");
        label.setTextSize(16);
        label.setPadding(10, 10, 10, 5);
        label.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        label.setTextColor(ContextCompat.getColor(context, R.color.black));

        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton noOption = new RadioButton(context);
        noOption.setText("No");
        noOption.setId(View.generateViewId());
        noOption.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        noOption.setTextColor(ContextCompat.getColor(context, R.color.black));

        RadioButton yesOption = new RadioButton(context);
        yesOption.setText("Yes");
        yesOption.setId(View.generateViewId());
        yesOption.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        yesOption.setTextColor(ContextCompat.getColor(context, R.color.black));

        radioGroup.addView(noOption);
        radioGroup.addView(yesOption);

        TextView specialNeedsLabel = new TextView(context);
        specialNeedsLabel.setText("Please specify special needs:");
        specialNeedsLabel.setTextSize(16);
        specialNeedsLabel.setPadding(10, 5, 10, 5);
        specialNeedsLabel.setTypeface(ResourcesCompat.getFont(context, R.font.quicksand_medium));
        specialNeedsLabel.setTextColor(ContextCompat.getColor(context, R.color.black));
        specialNeedsLabel.setVisibility(View.GONE);

        EditText specialNeedsInput = new EditText(context);
        specialNeedsInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        specialNeedsInput.setVisibility(View.GONE);
        specialNeedsInput.setHint("Please specify special needs:");
        specialNeedsInput.setTextSize(20);
        specialNeedsInput.setPadding(12, 12, 12, 12);
        specialNeedsInput.setTextColor(ContextCompat.getColor(context, R.color.black));
        specialNeedsInput.setHintTextColor(ContextCompat.getColor(context, R.color.milky));
        specialNeedsInput.setInputType(InputType.TYPE_CLASS_TEXT);
        specialNeedsInput.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_input));


        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == yesOption.getId()) {
                specialNeedsLabel.setVisibility(View.VISIBLE);
                specialNeedsInput.setVisibility(View.VISIBLE);
            } else {
                specialNeedsLabel.setVisibility(View.GONE);
                specialNeedsInput.setVisibility(View.GONE);
            }
        });

        parent.addView(label);
        parent.addView(radioGroup);
        parent.addView(specialNeedsLabel);
        parent.addView(specialNeedsInput);
    }


}
