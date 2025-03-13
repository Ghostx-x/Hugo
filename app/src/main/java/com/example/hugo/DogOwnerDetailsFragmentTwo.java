package com.example.hugo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.content.Context;

public class DogOwnerDetailsFragmentTwo extends Fragment {

    private int dogCount;

    public DogOwnerDetailsFragmentTwo() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dog_owner_details_two, container, false);

        if (getArguments() != null) {
            dogCount = getArguments().getInt("dogCount", 0);
        }

        LinearLayout mainLayout = view.findViewById(R.id.questions_container);
        for (int i = 1; i <= dogCount; i++) {
            addDogQuestions(mainLayout, i, requireContext());
        }

        return view;
    }

    private void addDogQuestions(LinearLayout parent, int dogIndex, Context context) {
        TextView title = new TextView(context);
        title.setText("Your " + ordinal(dogIndex) + " dog's details:");
        title.setTextSize(20);
        title.setPadding(10, 20, 10, 10);

        parent.addView(title);
        addQuestionField(parent, "Name:", context);
        addQuestionField(parent, "Breed:", context);
        addQuestionField(parent, "Gender:", context);
        addQuestionField(parent, "Birthday (DD/MM/YYYY):", context);
        addQuestionField(parent, "Special Needs:", context);
    }

    private void addQuestionField(LinearLayout parent, String labelText, Context context) {
        TextView label = new TextView(context);
        label.setText(labelText);
        label.setTextSize(16);
        label.setPadding(10, 10, 10, 5);

        EditText inputField = new EditText(context);
        inputField.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        parent.addView(label);
        parent.addView(inputField);
    }

    private String ordinal(int number) {
        if (number == 1) return "First";
        if (number == 2) return "Second";
        if (number == 3) return "Third";
        return number + "th";
    }
}
