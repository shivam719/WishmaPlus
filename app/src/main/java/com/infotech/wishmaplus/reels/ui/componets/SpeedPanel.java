package com.infotech.wishmaplus.reels.ui.componets;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.infotech.wishmaplus.R;

public class SpeedPanel extends BottomSheetDialogFragment {

    public interface SpeedListener {
        void onSpeedSelected(float speed);
    }

    private SpeedListener listener;
    private float currentSpeed = 1.0f;

    private static final float[] SPEEDS       = {0.3f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f};
    private static final String[] SPEED_LABELS = {"0.3×", "0.5×", "1×", "1.5×", "2×", "3×"};

    public static SpeedPanel newInstance() { return new SpeedPanel(); }
    public void setSpeedListener(SpeedListener l) { this.listener = l; }
    public void setCurrentSpeed(float s)           { this.currentSpeed = s; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.panel_speed, container, false);

        LinearLayout strip = v.findViewById(R.id.speedButtonStrip);
        for (int i = 0; i < SPEEDS.length; i++) {
            final float speed = SPEEDS[i];
            TextView btn = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            btn.setLayoutParams(lp);
            btn.setText(SPEED_LABELS[i]);
            btn.setTextSize(15f);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(8), dp(12), dp(8), dp(12));
            btn.setTextColor(Color.WHITE);

            updateBtnStyle(btn, speed == currentSpeed);

            btn.setOnClickListener(view -> {
                currentSpeed = speed;
                // Update all buttons
                for (int j = 0; j < strip.getChildCount(); j++) {
                    View child = strip.getChildAt(j);
                    if (child instanceof TextView)
                        updateBtnStyle((TextView) child, SPEEDS[j] == speed);
                }
                if (listener != null) listener.onSpeedSelected(speed);
            });
            strip.addView(btn);
        }

        v.findViewById(R.id.btnSpeedDone).setOnClickListener(b -> dismiss());
        return v;
    }

    private void updateBtnStyle(TextView btn, boolean selected) {
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(10));
        if (selected) {
            bg.setColor(0xFFFFCC00);
            btn.setTextColor(Color.BLACK);
        } else {
            bg.setColor(0x33FFFFFF);
            btn.setTextColor(Color.WHITE);
        }
        btn.setBackground(bg);
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}
