package com.example.hugo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

public class TrainerDetailsFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_CERTIFICATION_REQUEST = 2;

    private ImageView profilePhotoImageView;
    private TextView certificationTextView;
    private CheckBox basicObedience, behavioralCorrection, puppyTraining, agilityTraining;
    private EditText sessionDurationPricing;
    private RadioGroup inHomeTrainingGroup;
    private Button nextButton;

    public TrainerDetailsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trainer_details, container, false);

        profilePhotoImageView = view.findViewById(R.id.profilePhotoImageView);
        certificationTextView = view.findViewById(R.id.certificationTextView);
        basicObedience = view.findViewById(R.id.checkBoxBasicObedience);
        behavioralCorrection = view.findViewById(R.id.checkBoxBehavioralCorrection);
        puppyTraining = view.findViewById(R.id.checkBoxPuppyTraining);
        agilityTraining = view.findViewById(R.id.checkBoxAgilityTraining);
        sessionDurationPricing = view.findViewById(R.id.sessionDurationPricing);
        inHomeTrainingGroup = view.findViewById(R.id.inHomeTrainingGroup);
        nextButton = view.findViewById(R.id.btn_next);

        profilePhotoImageView.setOnClickListener(v -> openGallery(PICK_IMAGE_REQUEST));
        certificationTextView.setOnClickListener(v -> openGallery(PICK_CERTIFICATION_REQUEST));

        nextButton.setOnClickListener(v -> onNextButtonClick());

        return view;
    }

    private void openGallery(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                if (requestCode == PICK_IMAGE_REQUEST) {
                    profilePhotoImageView.setImageURI(selectedUri);
                } else if (requestCode == PICK_CERTIFICATION_REQUEST) {
                    certificationTextView.setText("Certification uploaded");
                }
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
        if (profilePhotoImageView.getDrawable() == null) return false;
        if (certificationTextView.getText().toString().isEmpty()) return false;
        if (!basicObedience.isChecked() && !behavioralCorrection.isChecked() && !puppyTraining.isChecked() && !agilityTraining.isChecked()) return false;
        if (sessionDurationPricing.getText().toString().isEmpty()) return false;
        if (inHomeTrainingGroup.getCheckedRadioButtonId() == -1) return false;
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
