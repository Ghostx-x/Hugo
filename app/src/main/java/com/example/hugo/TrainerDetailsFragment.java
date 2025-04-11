package com.example.hugo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hugo.MainActivity;
import com.example.hugo.R;

public class TrainerDetailsFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profilePhotoImageView;
    private TextView certificationTextView;
    private SeekBar sessionDurationPricing;
    private TextView textSessionPricing;
    private SeekBar seekBarSessionPricing;
    private TextView textSessionDuration;
    private Button nextButton;
    private Uri imageUri;
    private int sessionPricing = 1000;
    private int sessionDuration = 30;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trainer_details, container, false);

        profilePhotoImageView = view.findViewById(R.id.profilePhotoImageView);
        certificationTextView = view.findViewById(R.id.certificationTextView);
        sessionDurationPricing = view.findViewById(R.id.seekBarSessionDuration);
        textSessionPricing = view.findViewById(R.id.textSessionPricing);
        seekBarSessionPricing = view.findViewById(R.id.seekBarSessionPricing);
        textSessionDuration = view.findViewById(R.id.textSessionDuration);
        nextButton = view.findViewById(R.id.btn_next);

        profilePhotoImageView.setOnClickListener(v -> openFileChooser());
        nextButton.setOnClickListener(v -> onNextButtonClick());

        seekBarSessionPricing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sessionPricing = Math.max(1000, (progress / 1000) * 1000);
                textSessionPricing.setText("Price: " + sessionPricing + " drams");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sessionDurationPricing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sessionDuration = Math.max(10, progress + 10);
                textSessionDuration.setText("Duration: " + sessionDuration + " min");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profilePhotoImageView.setImageURI(imageUri);
            certificationTextView.setText(getFileName(imageUri));
        }
    }

    private String getFileName(Uri uri) {
        if (getActivity() == null) return "Unknown";
        String result = null;
        try (android.database.Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                result = cursor.getString(index);
            }
        }
        return result != null ? result : "Unknown";
    }

    private void onNextButtonClick() {
        int sessionDuration = sessionDurationPricing.getProgress();
        if (sessionDuration == 0) {
            Toast.makeText(getActivity(), "Please set session duration.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
