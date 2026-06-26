package com.infotech.wishmaplus.PageAccess.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.infotech.wishmaplus.PageAccess.model.SuggestedModeratorsResponse;
import com.infotech.wishmaplus.R;

import java.util.List;
import java.util.Locale;

public class ModeratorAdapter extends RecyclerView.Adapter<ModeratorAdapter.VH> {

    private final Context context;
    private final List<SuggestedModeratorsResponse.SuggestedUser> list;
    private final OnRowClickListener rowListener;
    private final OnModeratorClickListener moreListener;

    private static final int[] AVATAR_COLORS = {
            Color.parseColor("#1877F2"),
            Color.parseColor("#E02020"),
            Color.parseColor("#1D9E75"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#8E44AD"),
            Color.parseColor("#16A0C6"),
            Color.parseColor("#E8590C"),
            Color.parseColor("#5C6BC0")
    };

    public ModeratorAdapter(Context context,
                            List<SuggestedModeratorsResponse.SuggestedUser> list,
                            OnRowClickListener rowListener,
                            OnModeratorClickListener moreListener) {
        this.context = context;
        this.list = list;
        this.rowListener = rowListener;
        this.moreListener = moreListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_moderator, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SuggestedModeratorsResponse.SuggestedUser m = list.get(position);

        bindAvatarInitials(h.tvAvatarInitials, m.getFullName());

        h.tvName.setText(m.getFullName() != null ? m.getFullName() : "—");
        h.tvMobile.setText(m.getMobileNo() != null && !m.getMobileNo().isEmpty()
                ? m.getMobileNo() : "—");
        h.tvEntryDate.setText(m.getEntryAt() != null ? m.getEntryAt() : "");

        bindInviteStatus(h, m.getInviteStatus(), m.getInviteStatusText());

        boolean anyAllowed = m.isCanManageContent() || m.isCanManageMessages()
                || m.isCanManageCommunity() || m.isCanViewInsights();

        bindCardBorder(h, anyAllowed);

        if (anyAllowed) {
            h.tvPermissionsLabel.setVisibility(View.VISIBLE);
            h.permRow1.setVisibility(View.VISIBLE);
            h.permRow2.setVisibility(View.VISIBLE);
            h.tvNoPermissionWarning.setVisibility(View.GONE);

            bindPermChip(h.chipManageContent, h.tvManageContent, h.tvManageContentSub, h.dotContent,
                    m.isCanManageContent(), "Edit & delete posts", "No content access");
            bindPermChip(h.chipManageMessages, h.tvManageMessages, h.tvManageMessagesSub, h.dotMessages,
                    m.isCanManageMessages(), "Reply on page's behalf", "No message access");
            bindPermChip(h.chipManageCommunity, h.tvManageCommunity, h.tvManageCommunitySub, h.dotCommunity,
                    m.isCanManageCommunity(), "Manage followers & members", "No community access");
            bindPermChip(h.chipViewInsights, h.tvViewInsights, h.tvViewInsightsSub, h.dotInsights,
                    m.isCanViewInsights(), "View page analytics", "No insights access");
        } else {
            h.tvPermissionsLabel.setVisibility(View.GONE);
            h.permRow1.setVisibility(View.GONE);
            h.permRow2.setVisibility(View.GONE);
            h.tvNoPermissionWarning.setVisibility(View.VISIBLE);
        }

        h.itemView.setOnClickListener(v -> {
            if (rowListener != null) rowListener.onRowClick(m);
        });

        h.btnMore.setOnClickListener(v -> {
            if (moreListener != null) moreListener.onMoreClick(m, v);
        });
    }

    // ── Card border: red outline when zero permissions allowed ─────────
    private void bindCardBorder(VH h, boolean anyAllowed) {
        Drawable base = ContextCompat.getDrawable(context, R.drawable.rounded_corners);
        if (base == null) return;
        Drawable mutated = base.mutate();

        if (mutated instanceof GradientDrawable) {
            GradientDrawable gd = (GradientDrawable) mutated;
            if (anyAllowed) {
                gd.setStroke(0, Color.TRANSPARENT);
            } else {
                gd.setStroke(dpToPx(1), Color.parseColor("#E02020"));
            }
            h.itemView.setBackground(gd);
        } else {
            // Fallback if rounded_corners isn't a shape drawable: re-apply as-is
            h.itemView.setBackground(mutated);
        }
    }

    private void bindAvatarInitials(TextView tvAvatar, String fullName) {
        tvAvatar.setText(getInitials(fullName));
        tvAvatar.setTextColor(Color.WHITE);

        int seed = fullName != null ? Math.abs(fullName.hashCode()) : 0;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(AVATAR_COLORS[seed % AVATAR_COLORS.length]);
        tvAvatar.setBackground(bg);
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        initials.append(parts[0].charAt(0));
        if (parts.length > 1) {
            initials.append(parts[parts.length - 1].charAt(0));
        }
        return initials.toString().toUpperCase(Locale.getDefault());
    }

    private void bindInviteStatus(VH h, int status, String statusText) {
        String label = (statusText != null && !statusText.isEmpty()) ? statusText : "Pending";
        h.tvInviteStatus.setText(label);
        switch (status) {
            case 1:
                h.tvInviteStatus.setTextColor(Color.parseColor("#1D9E75"));
                break;
            case 2:
                h.tvInviteStatus.setTextColor(Color.parseColor("#E02020"));
                break;
            default:
                h.tvInviteStatus.setTextColor(Color.parseColor("#F59E0B"));
                break;
        }
    }

    private void bindPermChip(LinearLayout chip, TextView label, TextView subLabel, View dot,
                              boolean active, String activeText, String inactiveText) {
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_tab_selected);
            label.setTextColor(Color.parseColor("#1877F2"));
            subLabel.setText(activeText);
            subLabel.setTextColor(Color.parseColor("#6B7280"));
            dot.setVisibility(View.VISIBLE);
            setDotColor(dot, Color.parseColor("#1D9E75"));
        } else {
            chip.setBackgroundResource(R.drawable.bg_tab_unselected);
            label.setTextColor(Color.parseColor("#A8A7A7"));
            subLabel.setText(inactiveText);
            subLabel.setTextColor(Color.parseColor("#A8A7A7"));
            dot.setVisibility(View.INVISIBLE);
        }
    }

    private void setDotColor(View dot, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        dot.setBackground(bg);
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public interface OnRowClickListener {
        void onRowClick(SuggestedModeratorsResponse.SuggestedUser moderator);
    }

    public interface OnModeratorClickListener {
        void onMoreClick(SuggestedModeratorsResponse.SuggestedUser moderator, View anchor);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatarInitials;
        TextView tvName, tvMobile, tvEntryDate, tvInviteStatus;
        androidx.appcompat.widget.AppCompatImageButton btnMore;
        TextView tvPermissionsLabel, tvNoPermissionWarning;
        LinearLayout permRow1, permRow2;
        LinearLayout chipManageContent, chipManageMessages, chipManageCommunity, chipViewInsights;
        TextView tvManageContent, tvManageMessages, tvManageCommunity, tvViewInsights;
        TextView tvManageContentSub, tvManageMessagesSub, tvManageCommunitySub, tvViewInsightsSub;
        View dotContent, dotMessages, dotCommunity, dotInsights;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitials = itemView.findViewById(R.id.tvAvatarInitials);
            tvName = itemView.findViewById(R.id.tvName);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvEntryDate = itemView.findViewById(R.id.tvEntryDate);
            tvInviteStatus = itemView.findViewById(R.id.tvInviteStatus);
            btnMore = itemView.findViewById(R.id.btnMore);

            tvPermissionsLabel = itemView.findViewById(R.id.tvPermissionsLabel);
            tvNoPermissionWarning = itemView.findViewById(R.id.tvNoPermissionWarning);
            permRow1 = itemView.findViewById(R.id.permRow1);
            permRow2 = itemView.findViewById(R.id.permRow2);

            chipManageContent = itemView.findViewById(R.id.chipManageContent);
            chipManageMessages = itemView.findViewById(R.id.chipManageMessages);
            chipManageCommunity = itemView.findViewById(R.id.chipManageCommunity);
            chipViewInsights = itemView.findViewById(R.id.chipViewInsights);

            tvManageContent = itemView.findViewById(R.id.tvManageContent);
            tvManageMessages = itemView.findViewById(R.id.tvManageMessages);
            tvManageCommunity = itemView.findViewById(R.id.tvManageCommunity);
            tvViewInsights = itemView.findViewById(R.id.tvViewInsights);

            tvManageContentSub = itemView.findViewById(R.id.tvManageContentSub);
            tvManageMessagesSub = itemView.findViewById(R.id.tvManageMessagesSub);
            tvManageCommunitySub = itemView.findViewById(R.id.tvManageCommunitySub);
            tvViewInsightsSub = itemView.findViewById(R.id.tvViewInsightsSub);

            dotContent = itemView.findViewById(R.id.dotContent);
            dotMessages = itemView.findViewById(R.id.dotMessages);
            dotCommunity = itemView.findViewById(R.id.dotCommunity);
            dotInsights = itemView.findViewById(R.id.dotInsights);
        }
    }
}