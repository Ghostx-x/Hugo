package com.example.hugo.bottomnavbar.Search;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;


import com.example.hugo.R;
import com.squareup.picasso.Picasso;


import java.util.ArrayList;
import java.util.List;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {


    private List<User> userList;
    private Context context;


    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }


    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) return;


        holder.nameText.setText(user.name != null ? user.name : "No Name");
        holder.bioText.setText(user.bio != null ? user.bio : "");
        holder.roleText.setText(user.userType != null ? user.userType : "");
        if (user.pricePerHour > 0 && isServiceProvider(user.userType)) {
            holder.priceText.setText(String.format("Price: %.2f AMD/hr", user.pricePerHour));
            holder.priceText.setVisibility(View.VISIBLE);
        } else {
            holder.priceText.setVisibility(View.GONE);
        }


        if (user.profileImageBase64 != null && !user.profileImageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(user.profileImageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.profileImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                holder.profileImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile);
        }


        holder.itemView.setOnClickListener(v -> {
            if (context instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) context;
                ViewProfileFragment fragment = ViewProfileFragment.newInstance(userList.get(position).userId);
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }


    private boolean isServiceProvider(String userType) {
        return userType != null && (
                userType.equalsIgnoreCase("Dog Walker") ||
                        userType.equalsIgnoreCase("Trainer") ||
                        userType.equalsIgnoreCase("Veterinarian")
        );
    }


    @Override
    public int getItemCount() {
        return (userList != null) ? userList.size() : 0;
    }


    public void filterList(List<User> filteredList) {
        this.userList = filteredList != null ? filteredList : new ArrayList<>();
        notifyDataSetChanged();
    }


    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText, bioText, roleText, priceText;


        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.item_profile_image);
            nameText = itemView.findViewById(R.id.item_name);
            bioText = itemView.findViewById(R.id.item_bio);
            roleText = itemView.findViewById(R.id.item_role);
            priceText = itemView.findViewById(R.id.item_price);
        }
    }
}
