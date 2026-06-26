package com.infotech.wishmaplus.reels.bottomsheet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Speed Control Bottom Sheet
 * Speeds: 0.3x, 0.5x, 1x, 1.5x, 2x, 3x
 */
public class SpeedControlBottomSheet extends BottomSheetDialogFragment {

    public interface SpeedListener {
        void onSpeedSelected(float speed);
    }

    private static final float[] SPEEDS = {0.3f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f};
    private static final String[] SPEED_LABELS = {"0.3x", "0.5x", "1x", "1.5x", "2x", "3x"};

    private SpeedListener listener;
    private float currentSpeed = 1.0f;

    public static SpeedControlBottomSheet newInstance(float currentSpeed) {
        SpeedControlBottomSheet sheet = new SpeedControlBottomSheet();
        Bundle args = new Bundle();
        args.putFloat("current_speed", currentSpeed);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(SpeedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_Dark_BottomSheetDialog);
        if (getArguments() != null) {
            currentSpeed = getArguments().getFloat("current_speed", 1.0f);
        }
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Build programmatically
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A1A);
        root.setPadding(dp(20), dp(20), dp(20), dp(40));

        // Title
        TextView title = new TextView(requireContext());
        title.setText("Playback Speed");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18f);
        title.setPadding(0, 0, 0, dp(20));
        root.addView(title);

        // Current speed display
        TextView currentLabel = new TextView(requireContext());
        currentLabel.setTextColor(0xFF1877F2);
        currentLabel.setTextSize(36f);
        currentLabel.setGravity(android.view.Gravity.CENTER);
        currentLabel.setText(formatSpeed(currentSpeed));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(24);
        currentLabel.setLayoutParams(clp);
        root.addView(currentLabel);

        // Speed buttons row
        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(28);
        btnRow.setLayoutParams(rowLp);

        TextView[] speedBtns = new TextView[SPEEDS.length];
        for (int i = 0; i < SPEEDS.length; i++) {
            final float speed = SPEEDS[i];
            final int idx = i;
            TextView btn = new TextView(requireContext());
            btn.setText(SPEED_LABELS[i]);
            btn.setTextSize(13f);
            btn.setGravity(android.view.Gravity.CENTER);
            boolean isSelected = Math.abs(speed - currentSpeed) < 0.01f;
            styleSpeedBtn(btn, isSelected);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            blp.setMargins(dp(4), 0, dp(4), 0);
            btn.setLayoutParams(blp);

            btn.setOnClickListener(v -> {
                currentSpeed = speed;
                currentLabel.setText(formatSpeed(speed));
                for (int j = 0; j < speedBtns.length; j++) {
                    if (speedBtns[j] != null)
                        styleSpeedBtn(speedBtns[j], j == idx);
                }
                if (listener != null) listener.onSpeedSelected(speed);
            });
            speedBtns[i] = btn;
            btnRow.addView(btn);
        }
        root.addView(btnRow);

        // Fine-grained seekbar (0.1x – 3.0x)
        TextView seekLabel = new TextView(requireContext());
        seekLabel.setText("Fine Control");
        seekLabel.setTextColor(0xAAFFFFFF);
        seekLabel.setTextSize(13f);
        LinearLayout.LayoutParams slLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slLp.bottomMargin = dp(8);
        seekLabel.setLayoutParams(slLp);
        root.addView(seekLabel);

        SeekBar seekBar = new SeekBar(requireContext());
        seekBar.setMax(290); // 0.1 to 3.0 in 0.01 steps → 290 steps
        int initialProgress = (int) ((currentSpeed - 0.1f) * 100);
        seekBar.setProgress(Math.max(0, Math.min(290, initialProgress)));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (!fromUser) return;
                float speed = 0.1f + p / 100f;
                speed = Math.round(speed * 10) / 10f; // round to 1 decimal
                currentSpeed = speed;
                currentLabel.setText(formatSpeed(speed));
                // Update button highlights
                for (int j = 0; j < SPEEDS.length; j++) {
                    if (speedBtns[j] != null)
                        styleSpeedBtn(speedBtns[j], Math.abs(SPEEDS[j] - speed) < 0.01f);
                }
                if (listener != null) listener.onSpeedSelected(speed);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        root.addView(seekBar);

        // Labels row
        LinearLayout labelRow = new LinearLayout(requireContext());
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lrLp.topMargin = dp(4);
        labelRow.setLayoutParams(lrLp);

        TextView slowLabel = new TextView(requireContext());
        slowLabel.setText("Slow");
        slowLabel.setTextColor(0x66FFFFFF);
        slowLabel.setTextSize(11f);
        labelRow.addView(slowLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView fastLabel = new TextView(requireContext());
        fastLabel.setText("Fast");
        fastLabel.setTextColor(0x66FFFFFF);
        fastLabel.setTextSize(11f);
        fastLabel.setGravity(android.view.Gravity.END);
        labelRow.addView(fastLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(labelRow);

        // Note
        TextView note = new TextView(requireContext());
        note.setText("⚡ Speed change applies during export");
        note.setTextColor(0x66FFFFFF);
        note.setTextSize(11f);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteLp.topMargin = dp(20);
        note.setLayoutParams(noteLp);
        note.setGravity(android.view.Gravity.CENTER);
        root.addView(note);

        return root;
    }

    private void styleSpeedBtn(TextView btn, boolean selected) {
        btn.setTextColor(selected ? 0xFF1877F2 : 0xAAFFFFFF);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(selected ? 0x221877F2 : 0x22FFFFFF);
        if (selected) bg.setStroke(dp(1), 0xFF1877F2);
        btn.setBackground(bg);
    }

    private String formatSpeed(float speed) {
        if (speed == (int) speed) return (int) speed + "x";
        return speed + "x";
    }

    private int dp(int val) {
        return (int) (val * requireContext().getResources().getDisplayMetrics().density);
    }
}
