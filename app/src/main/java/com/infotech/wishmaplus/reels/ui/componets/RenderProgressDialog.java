package com.infotech.wishmaplus.reels.ui.componets;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.infotech.wishmaplus.R;

public class RenderProgressDialog {

    private final Dialog dialog;
    private final ProgressBar progressBar;
    private final TextView tvTitle;
    private final TextView tvStatus;
    private final TextView tvPercent;
    private final TextView tvStep;
    private final ImageView ivIcon;

    public RenderProgressDialog(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_render_progress);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        progressBar = dialog.findViewById(R.id.renderProgressBar);
        tvTitle = dialog.findViewById(R.id.tvRenderTitle);
        tvStatus = dialog.findViewById(R.id.tvRenderStatus);
        tvPercent = dialog.findViewById(R.id.tvRenderPercent);
        tvStep = dialog.findViewById(R.id.tvRenderStep);
        ivIcon = dialog.findViewById(R.id.ivRenderIcon);

        // Icon pulse animation
        ivIcon.startAnimation(AnimationUtils.loadAnimation(context, R.anim.pulse));
    }

    public void show() {
        if (!dialog.isShowing()) dialog.show();
    }

    public void dismiss() {
        if (dialog.isShowing()) dialog.dismiss();
    }

    // ── Update progress ───────────────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    public void updateProgress(int percent, String message) {
        progressBar.setProgress(percent);
        tvPercent.setText(percent + "%");
        tvStep.setText(message);

        // Status message — step ke hisaab se
        if (percent <= 10) {
            tvStatus.setText("Capturing overlays...");
        } else if (percent <= 30) {
            tvStatus.setText("Processing media...");
        } else if (percent <= 60) {
            tvStatus.setText("Rendering video...");
        } else if (percent <= 90) {
            tvStatus.setText("Mixing audio...");
        } else {
            tvStatus.setText("Almost done...");
        }
    }

    // ── Success state ─────────────────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    public void showSuccess() {
        progressBar.setProgress(100);
        tvPercent.setText("100%");
        tvTitle.setText("Ready to Share!");
        tvStatus.setText("Your reel has been processed");
        tvStep.setText("Uploading now...");
        ivIcon.clearAnimation();
        ivIcon.setImageResource(R.drawable.ic_check_circle_new);
    }

    // ── Error state ───────────────────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    public void showError(String error) {
        ivIcon.clearAnimation();
        ivIcon.setImageResource(R.drawable.ic_cross_circle);
        tvTitle.setText("Processing Failed");
        tvStatus.setText(error);
        tvStep.setText("Please try again");
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }
}