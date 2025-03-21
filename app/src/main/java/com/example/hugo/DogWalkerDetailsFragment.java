package com.example.hugo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

import com.example.hugo.R;

public class DogWalkerDetailsFragment extends Fragment {

    private RadioGroup dogSizeGroup;
    private RadioGroup maxDogsGroup;
    private TextView profilePhotoText;
    private TextView experienceText;
    private TextView dogSizesText;
    private TextView maxDogsText;
    private TextView titleText;
    private ImageView profilePhotoImageView;
    private Button nextButton;

    private static final int PICK_IMAGE_REQUEST = 1;

    public DogWalkerDetailsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dog_walker_details, container, false);

        dogSizeGroup = view.findViewById(R.id.dogSizeGroup);
        maxDogsGroup = view.findViewById(R.id.maxDogsGroup);
        profilePhotoText = view.findViewById(R.id.profilePhotoText);
        experienceText = view.findViewById(R.id.experienceText);
        dogSizesText = view.findViewById(R.id.dogSizesText);
        maxDogsText = view.findViewById(R.id.maxDogsText);
        titleText = view.findViewById(R.id.titleText);
        profilePhotoImageView = view.findViewById(R.id.profilePhotoImageView);
        nextButton = view.findViewById(R.id.btn_next);

        profilePhotoImageView.setOnClickListener(v -> openGalleryForImage(PICK_IMAGE_REQUEST));

        nextButton.setOnClickListener(v -> onNextButtonClick());

        return view;
    }

    private void openGalleryForImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                profilePhotoImageView.setImageURI(selectedImageUri);
            }
        }
    }

    private void onNextButtonClick() {
        if (isAllInputsValid()) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(getActivity(), "Please fill all the fields", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAllInputsValid() {
        if (profilePhotoImageView.getDrawable() == null) {
            return false;
        }

        int selectedExperienceId = experienceText.getId();
        if (selectedExperienceId == -1) {
            return false;
        }

        int selectedDogSizeId = dogSizeGroup.getCheckedRadioButtonId();
        if (selectedDogSizeId == -1) {
            return false;
        }

        int selectedMaxDogsId = maxDogsGroup.getCheckedRadioButtonId();
        if (selectedMaxDogsId == -1) {
            return false;
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(requireActivity(), SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }


}
