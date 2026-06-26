package com.infotech.wishmaplus.PageAccess.BottomSheet;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.infotech.wishmaplus.PageAccess.model.ModeratorsResponse;
import com.infotech.wishmaplus.PageAccess.model.SuggestedModeratorsResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.UtilMethods;

public class ModeratorDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MODERATOR_ID = "moderatorId";
    private static final String ARG_PAGE_ID = "pageId";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_FULL_NAME = "fullName";
    private static final String ARG_PROFILE_PIC = "profilePic";
    private static final String ARG_MOBILE = "mobile";
    private static final String ARG_INVITE_STATUS = "inviteStatus";
    private static final String ARG_INVITE_STATUS_TEXT = "inviteStatusText";
    private static final String ARG_ENTRY_AT = "entryAt";
    private static final String ARG_CAN_CONTENT = "canContent";
    private static final String ARG_CAN_MESSAGES = "canMessages";
    private static final String ARG_CAN_COMMUNITY = "canCommunity";
    private static final String ARG_CAN_INSIGHTS = "canInsights";
    private OnModeratorChangedListener changeListener;
    // ── Views ─────────────────────────────────────────────────────────────
    private androidx.appcompat.widget.AppCompatImageView bsIvAvatar;
    private TextView bsTvName, bsTvMobile, bsTvInviteStatus, bsTvEntryDate;
    private SwitchCompat switchContent, switchMessages, switchCommunity, switchInsights;
    private TextView btnSave, btnRemove;

    // ── Factory ──────────────────────────────────────────────────────────
    public static ModeratorDetailBottomSheet newInstance(SuggestedModeratorsResponse.SuggestedUser m, String pageId) {
        ModeratorDetailBottomSheet sheet = new ModeratorDetailBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_MODERATOR_ID, m.getModeratorId());
        args.putString(ARG_PAGE_ID, pageId);
        args.putString(ARG_USER_ID, m.getUserId());
        args.putString(ARG_FULL_NAME, m.getFullName());
        args.putString(ARG_PROFILE_PIC, m.getProfilePictureUrl());
        args.putString(ARG_MOBILE, m.getMobileNo());
        args.putInt(ARG_INVITE_STATUS, m.getInviteStatus());
        args.putString(ARG_INVITE_STATUS_TEXT, m.getInviteStatusText());
        args.putString(ARG_ENTRY_AT, m.getEntryAt());
        args.putBoolean(ARG_CAN_CONTENT, m.isCanManageContent());
        args.putBoolean(ARG_CAN_MESSAGES, m.isCanManageMessages());
        args.putBoolean(ARG_CAN_COMMUNITY, m.isCanManageCommunity());
        args.putBoolean(ARG_CAN_INSIGHTS, m.isCanViewInsights());
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnModeratorChangedListener(OnModeratorChangedListener l) {
        this.changeListener = l;
    }

    // ── Inflate ───────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_moderator_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Expand fully by default
        View parent = (View) view.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        bindViews(view);
        populateData();
        setupListeners();
    }

    private void bindViews(View v) {
        bsIvAvatar = v.findViewById(R.id.bsIvAvatar);
        bsTvName = v.findViewById(R.id.bsTvName);
        bsTvMobile = v.findViewById(R.id.bsTvMobile);
        bsTvInviteStatus = v.findViewById(R.id.bsTvInviteStatus);
        bsTvEntryDate = v.findViewById(R.id.bsTvEntryDate);
        switchContent = v.findViewById(R.id.switchManageContent);
        switchMessages = v.findViewById(R.id.switchManageMessages);
        switchCommunity = v.findViewById(R.id.switchManageCommunity);
        switchInsights = v.findViewById(R.id.switchViewInsights);
        btnSave = v.findViewById(R.id.btnSavePermissions);
        btnRemove = v.findViewById(R.id.btnRemoveAccess);
    }

    private void populateData() {
        Bundle a = requireArguments();

        // Avatar
        Glide.with(this).load(a.getString(ARG_PROFILE_PIC)).apply(UtilMethods.INSTANCE.getRequestOption_With_UserIcon()).into(bsIvAvatar);

        // Name & mobile
        bsTvName.setText(a.getString(ARG_FULL_NAME, "—"));
        String mobile = a.getString(ARG_MOBILE, "");
        bsTvMobile.setText((mobile != null && !mobile.isEmpty()) ? mobile : "—");

        // Entry date
        String entry = a.getString(ARG_ENTRY_AT, "");
        bsTvEntryDate.setText((entry != null && !entry.isEmpty()) ? "Since " + entry : "");

        // Invite status
        int status = a.getInt(ARG_INVITE_STATUS, 0);
        String statusText = a.getString(ARG_INVITE_STATUS_TEXT, "Pending");
        bsTvInviteStatus.setText(statusText != null ? statusText : "Pending");
        switch (status) {
            case 1:
                bsTvInviteStatus.setTextColor(Color.parseColor("#1D9E75"));
                break;
            case 2:
                bsTvInviteStatus.setTextColor(Color.parseColor("#E02020"));
                break;
            default:
                bsTvInviteStatus.setTextColor(Color.parseColor("#F59E0B"));
                break;
        }

        // Switches — set without triggering listeners
        switchContent.setChecked(a.getBoolean(ARG_CAN_CONTENT, false));
        switchMessages.setChecked(a.getBoolean(ARG_CAN_MESSAGES, false));
        switchCommunity.setChecked(a.getBoolean(ARG_CAN_COMMUNITY, false));
        switchInsights.setChecked(a.getBoolean(ARG_CAN_INSIGHTS, false));
    }

    private void setupListeners() {
        Bundle a = requireArguments();
        int moderatorId = a.getInt(ARG_MODERATOR_ID);
        String pageId = a.getString(ARG_PAGE_ID, "");
        String userId = a.getString(ARG_USER_ID, "");

        // ── Save ──────────────────────────────────────────────────────────
        btnSave.setOnClickListener(v -> {
            boolean canContent = switchContent.isChecked();
            boolean canMessages = switchMessages.isChecked();
            boolean canCommunity = switchCommunity.isChecked();
            boolean canInsights = switchInsights.isChecked();

            // Show loading state on button
            btnSave.setText("Saving…");
            btnSave.setEnabled(false);

            UtilMethods.INSTANCE.updateModeratorPermissions(pageId, userId, moderatorId, canContent, canMessages, canCommunity, canInsights, new UtilMethods.ApiCallBackMulti() {
                @Override
                public void onSuccess(Object object) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Permissions updated", Toast.LENGTH_SHORT).show();
                        if (changeListener != null)
                            changeListener.onPermissionsUpdated(moderatorId);
                        dismiss();
                    });
                }

                @Override
                public void onError(String msg) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        btnSave.setText("Save");
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + msg, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // ── Remove access ─────────────────────────────────────────────────
        btnRemove.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Remove access").setMessage("Remove " + a.getString(ARG_FULL_NAME, "this person") + " from page moderators?").setPositiveButton("Remove", (dialog, which) -> {
                btnRemove.setText("Removing…");
                btnRemove.setEnabled(false);

                UtilMethods.INSTANCE.removeModerator(pageId, moderatorId, new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Access removed", Toast.LENGTH_SHORT).show();
                            if (changeListener != null) changeListener.onAccessRemoved(moderatorId);
                            dismiss();
                        });
                    }

                    @Override
                    public void onError(String msg) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            btnRemove.setText("Remove access");
                            btnRemove.setEnabled(true);
                            Toast.makeText(getContext(), "Failed: " + msg, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }).setNegativeButton("Cancel", null).show();
        });
    }

    // Callback so PageAccessActivity can refresh the list after a change
    public interface OnModeratorChangedListener {
        void onPermissionsUpdated(int moderatorId);

        void onAccessRemoved(int moderatorId);
    }
}