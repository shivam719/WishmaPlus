package com.infotech.wishmaplus.PageAccess.adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.infotech.wishmaplus.PageAccess.model.SuggestedModeratorsResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.UtilMethods;

import java.util.List;

public class SuggestedUserAdapter extends RecyclerView.Adapter<SuggestedUserAdapter.VH> {

    private final Context context;
    private final List<SuggestedModeratorsResponse.SuggestedUser> list;
    private final OnInviteClickListener listener;

    public SuggestedUserAdapter(Context context,
                                List<SuggestedModeratorsResponse.SuggestedUser> list,
                                OnInviteClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_suggested_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SuggestedModeratorsResponse.SuggestedUser u = list.get(position);

        Glide.with(context)
                .load(u.getProfilePictureUrl())
                .apply(UtilMethods.INSTANCE.getRequestOption_With_UserIcon())
                .into(h.ivAvatar);

        h.tvName.setText(u.getFullName() != null ? u.getFullName() : "—");
        h.tvMobile.setText(u.getMobileNo() != null ? u.getMobileNo() : "");

        h.btnInvite.setOnClickListener(v -> {
            if (listener != null) listener.onInvite(u, h.btnInvite);
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public interface OnInviteClickListener {
        void onInvite(SuggestedModeratorsResponse.SuggestedUser user, View anchor);
    }

    static class VH extends RecyclerView.ViewHolder {
        AppCompatImageView ivAvatar;
        TextView tvName, tvMobile;
        AppCompatTextView btnInvite;

        VH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            btnInvite = itemView.findViewById(R.id.btnInvite);
        }
    }
}