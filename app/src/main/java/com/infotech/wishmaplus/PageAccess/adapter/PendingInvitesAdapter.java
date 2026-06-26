package com.infotech.wishmaplus.PageAccess.adapter;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.infotech.wishmaplus.PageAccess.model.ModeratorInvite;
import com.infotech.wishmaplus.R;

import java.util.ArrayList;
import java.util.List;

public class PendingInvitesAdapter
        extends RecyclerView.Adapter<PendingInvitesAdapter.InviteViewHolder> {

    private final Context context;
    private final List<ModeratorInvite> invites;
    private final OnInviteActionListener listener;
    // Track which positions are currently loading (accept/decline in progress)
    private final List<Integer> loadingPositions = new ArrayList<>();

    public PendingInvitesAdapter(Context context,
                                 List<ModeratorInvite> invites,
                                 OnInviteActionListener listener) {
        this.context = context;
        this.invites = invites;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_pending_invite, parent, false);
        return new InviteViewHolder(view);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        ModeratorInvite invite = invites.get(position);
        holder.bind(invite, position);
    }

    @Override
    public int getItemCount() {
        return invites != null ? invites.size() : 0;
    }

    /**
     * Call when an action API call starts – shows loading on that card
     */
    public void setLoading(int position, boolean loading) {
        if (loading) {
            if (!loadingPositions.contains(position)) loadingPositions.add(position);
        } else {
            loadingPositions.remove(Integer.valueOf(position));
        }
        notifyItemChanged(position);
    }

    // ── Public helpers ───────────────────────────────────────────────────────

    /**
     * Remove a card after successful accept/decline
     */
    public void removeItem(int position) {
        if (position >= 0 && position < invites.size()) {
            invites.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, invites.size());
        }
    }

    // ── Listener interface ───────────────────────────────────────────────────
    public interface OnInviteActionListener {
        void onAccept(ModeratorInvite invite, int position);

        void onDecline(ModeratorInvite invite, int position);
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────

    class InviteViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPageImage;
        private final TextView tvPageName, tvInvitedBy, tvEntryDate;
        private final ImageView icManageContent, icManageMessages,
                icManageCommunity, icViewInsights;
        private final MaterialButton btnAccept, btnDecline;
        private final FrameLayout layoutActionLoading;

        InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPageImage = itemView.findViewById(R.id.ivPageImage);
            tvPageName = itemView.findViewById(R.id.tvPageName);
            tvInvitedBy = itemView.findViewById(R.id.tvInvitedBy);
            tvEntryDate = itemView.findViewById(R.id.tvEntryDate);
            icManageContent = itemView.findViewById(R.id.icManageContent);
            icManageMessages = itemView.findViewById(R.id.icManageMessages);
            icManageCommunity = itemView.findViewById(R.id.icManageCommunity);
            icViewInsights = itemView.findViewById(R.id.icViewInsights);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            layoutActionLoading = itemView.findViewById(R.id.layoutActionLoading);
        }

        @SuppressLint("SetTextI18n")
        void bind(ModeratorInvite invite, int position) {

            // ── Text fields ──────────────────────────────────────────────────
            tvPageName.setText(invite.getPageName() != null ? invite.getPageName() : "—");
            tvInvitedBy.setText("Invited by " + (invite.getInvitedByName() != null
                    ? invite.getInvitedByName() : "Unknown"));
            tvEntryDate.setText(invite.getEntryAt() != null ? invite.getEntryAt() : "");

            // ── Page profile image ───────────────────────────────────────────
            String imgUrl = invite.getPageProfileImageUrl();
            if (imgUrl != null && !imgUrl.isEmpty() && !imgUrl.equalsIgnoreCase("string")) {
                Glide.with(context)
                        .load(imgUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_default_page)
                                .error(R.drawable.ic_default_page)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .circleCrop())
                        .into(ivPageImage);
            } else {
                ivPageImage.setImageResource(R.drawable.ic_default_page);
            }

            // ── Permission icons ─────────────────────────────────────────────
            setPermissionIcon(icManageContent, invite.isCanManageContent());
            setPermissionIcon(icManageMessages, invite.isCanManageMessages());
            setPermissionIcon(icManageCommunity, invite.isCanManageCommunity());
            setPermissionIcon(icViewInsights, invite.isCanViewInsights());

            // ── Loading state ────────────────────────────────────────────────
            boolean isLoading = loadingPositions.contains(position);
            layoutActionLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnAccept.setEnabled(!isLoading);
            btnDecline.setEnabled(!isLoading);
            btnAccept.setAlpha(isLoading ? 0.5f : 1f);
            btnDecline.setAlpha(isLoading ? 0.5f : 1f);

            // ── Button clicks ────────────────────────────────────────────────
            btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(invite, getBindingAdapterPosition());
            });

            btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDecline(invite, getBindingAdapterPosition());
            });
        }

        /**
         * Tints the permission check icon green (granted) or grey (denied)
         */
/*        private void setPermissionIcon(ImageView icon, boolean granted) {
            int colorRes = granted ? R.color.color_green : R.color.grey_2;
            icon.setColorFilter(
                    ContextCompat.getColor(context, colorRes),
                    PorterDuff.Mode.SRC_IN
            );
            icon.setImageResource(granted
                    ? R.drawable.ic_check_circle
                    : R.drawable.ic_error);
        }*/
        private void setPermissionIcon(ImageView icon, boolean granted) {

            int colorRes = granted
                    ? R.color.color_green
                    : R.color.grey_2;

            icon.setColorFilter(
                    ContextCompat.getColor(context, colorRes),
                    PorterDuff.Mode.SRC_IN
            );
        }
    }
}