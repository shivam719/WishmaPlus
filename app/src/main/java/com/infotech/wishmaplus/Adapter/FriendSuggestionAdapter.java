package com.infotech.wishmaplus.Adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.infotech.wishmaplus.R;

import java.util.List;

public class FriendSuggestionAdapter extends RecyclerView.Adapter<FriendSuggestionAdapter.UserVH> {

    private final List<FriendSuggestionItem> list;
    private final Context ctx;
    private final OnItemClickListener listener;

    // In FriendSuggestionAdapter.java
    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<FriendSuggestionItem> newList) {
        this.list.clear();
        this.list.addAll(newList);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(FriendSuggestionItem user, int pos);
        void onMoreClicked(View anchor, FriendSuggestionItem user, int pos);
        void onProfileClick(FriendSuggestionItem user, int position);
    }

    public FriendSuggestionAdapter(Context ctx, List<FriendSuggestionItem> list, OnItemClickListener listener) {
        this.ctx = ctx;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_remove_friend_layout, parent, false);
        return new UserVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH holder, int position) {

        FriendSuggestionItem user = list.get(position);

        holder.name.setText(user.getFullName());

        Glide.with(ctx)
                .load(user.getProfilePictureUrl())
                .placeholder(R.drawable.user_icon)
                .error(R.drawable.user_icon)
                .into(holder.profileImage);

        // Friend Status UI
        if (user.isFriend()) {
            holder.btnConfirm.setEnabled(false);
            holder.btnConfirm.setText("Already Friends");
        } else {
            holder.btnConfirm.setEnabled(true);
            holder.btnConfirm.setText("Add Friend");

        }
        holder.btnConfirm.setVisibility(VISIBLE);
        holder.btnDelete.setVisibility(GONE);

        View.OnClickListener profileClick = v -> {
            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onProfileClick(
                        list.get(adapterPosition),
                        adapterPosition
                );
            }
        };

        holder.profileImage.setOnClickListener(profileClick);
        holder.name.setOnClickListener(profileClick);

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(
                        list.get(adapterPosition),
                        adapterPosition
                );
            }
        });

        holder.btnConfirm.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            FriendSuggestionItem clickedUser = list.get(adapterPosition);
            if (clickedUser.isFriend()) {
                Toast.makeText(
                        ctx,
                        "You are already friends with " + clickedUser.getFullName(),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            if (listener != null) {
                listener.onMoreClicked(
                        holder.btnConfirm,
                        clickedUser,
                        adapterPosition
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView name;
        Button btnConfirm, btnDelete;

        public UserVH(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            name = itemView.findViewById(R.id.userName);
            btnConfirm = itemView.findViewById(R.id.btnAddFriend);
            btnDelete = itemView.findViewById(R.id.removeUserBtn);
        }
    }
}