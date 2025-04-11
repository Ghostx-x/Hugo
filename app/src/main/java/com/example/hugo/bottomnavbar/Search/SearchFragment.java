package com.example.hugo.bottomnavbar.Search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hugo.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private Spinner roleFilter;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> allUsers = new ArrayList<>();
    private DatabaseReference usersRef;
    private TextView noResultsText;
    private BottomNavigationView bottomNavigationView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        searchInput = view.findViewById(R.id.search_input);
        roleFilter = view.findViewById(R.id.role_filter);
        recyclerView = view.findViewById(R.id.user_recycler_view);
        noResultsText = view.findViewById(R.id.no_results_text);

        bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(userAdapter);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadAllUsers();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers();
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        roleFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterUsers();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAllUsers() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsers.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null) {
                        allUsers.add(user);
                    }
                }
                filterUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers() {
        String query = searchInput.getText().toString().toLowerCase().trim();
        String selectedRole = roleFilter.getSelectedItem().toString();

        List<User> filtered = new ArrayList<>();
        for (User user : allUsers) {
            if (user == null) continue;

            String name = user.name != null ? user.name : "";
            String bio = user.bio != null ? user.bio : "";
            String role = user.userType != null ? user.userType : "";

            boolean matchesQuery = FuzzySearchUtil.isFuzzyMatch(query, name) ||
                    FuzzySearchUtil.isFuzzyMatch(query, bio);

            boolean matchesRole = selectedRole.equals("All") || role.equals(selectedRole);

            if (matchesQuery && matchesRole) {
                filtered.add(user);
            }
        }

        userAdapter.filterList(filtered);
        noResultsText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }





}

