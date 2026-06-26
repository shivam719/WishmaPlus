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
 * Audio Mixer Bottom Sheet
 * Controls: Original audio volume, Music volume, Voice-over (future)
 */
public class AudioMixerBottomSheet extends BottomSheetDialogFragment {

    public interface AudioMixListener {
        void onOriginalVolumeChanged(float volume); // 0.0 – 1.0
        void onMusicVolumeChanged(float volume);    // 0.0 – 1.0
        void onMuteOriginalChanged(boolean muted);
        void onMuteMusicChanged(boolean muted);
    }

    private AudioMixListener listener;
    private float originalVolume = 1.0f;
    private float musicVolume = 0.8f;
    private boolean originalMuted = false;
    private boolean musicMuted = false;
    private boolean hasMusicTrack = false;

    public static AudioMixerBottomSheet newInstance(boolean hasMusicTrack,
                                                     float originalVol,
                                                     float musicVol) {
        AudioMixerBottomSheet sheet = new AudioMixerBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean("has_music", hasMusicTrack);
        args.putFloat("orig_vol", originalVol);
        args.putFloat("music_vol", musicVol);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(AudioMixListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_Dark_BottomSheetDialog);
        if (getArguments() != null) {
            hasMusicTrack = getArguments().getBoolean("has_music", false);
            originalVolume = getArguments().getFloat("orig_vol", 1.0f);
            musicVolume = getArguments().getFloat("music_vol", 0.8f);
        }
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A1A);
        root.setPadding(dp(20), dp(20), dp(20), dp(44));

        // Title
        TextView title = new TextView(requireContext());
        title.setText("Audio Mixer");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = dp(24);
        title.setLayoutParams(tlp);
        root.addView(title);

        // ── Original Audio ─────────────────────────────────────────────
        root.addView(buildSectionHeader("🎬  Original Audio"));
        root.addView(buildVolumeRow(
                originalVolume,
                originalMuted,
                (vol) -> {
                    originalVolume = vol;
                    if (listener != null) listener.onOriginalVolumeChanged(vol);
                },
                (muted) -> {
                    originalMuted = muted;
                    if (listener != null) listener.onMuteOriginalChanged(muted);
                }
        ));

        // Divider
        root.addView(buildDivider());

        // ── Music Track ────────────────────────────────────────────────
        root.addView(buildSectionHeader("🎵  Background Music"));

        if (hasMusicTrack) {
            root.addView(buildVolumeRow(
                    musicVolume,
                    musicMuted,
                    (vol) -> {
                        musicVolume = vol;
                        if (listener != null) listener.onMusicVolumeChanged(vol);
                    },
                    (muted) -> {
                        musicMuted = muted;
                        if (listener != null) listener.onMuteMusicChanged(muted);
                    }
            ));
        } else {
            TextView noMusic = new TextView(requireContext());
            noMusic.setText("No music added yet. Tap 🎵 to add music.");
            noMusic.setTextColor(0x66FFFFFF);
            noMusic.setTextSize(13f);
            LinearLayout.LayoutParams nmlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nmlp.topMargin = dp(8);
            nmlp.bottomMargin = dp(8);
            noMusic.setLayoutParams(nmlp);
            root.addView(noMusic);
        }

        // Divider
        root.addView(buildDivider());

        // ── Audio tips ─────────────────────────────────────────────────
        TextView tip = new TextView(requireContext());
        tip.setText("💡  Tip: Set original to 0% for music-only reels");
        tip.setTextColor(0x55FFFFFF);
        tip.setTextSize(12f);
        LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tipLp.topMargin = dp(12);
        tip.setLayoutParams(tipLp);
        root.addView(tip);

        return root;
    }

    @SuppressLint("SetTextI18n")
    private View buildVolumeRow(float initialVol, boolean initialMuted,
                                VolumeCallback volCb, MuteCallback muteCb) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(8);
        rowLp.bottomMargin = dp(4);
        row.setLayoutParams(rowLp);

        // Mute toggle button
        TextView muteBtn = new TextView(requireContext());
        muteBtn.setText(initialMuted ? "🔇" : "🔊");
        muteBtn.setTextSize(22f);
        muteBtn.setPadding(0, 0, dp(12), 0);
        final boolean[] muted = {initialMuted};

        // Volume % label
        TextView volLabel = new TextView(requireContext());
        volLabel.setText((int)(initialVol * 100) + "%");
        volLabel.setTextColor(0xAAFFFFFF);
        volLabel.setTextSize(13f);
        volLabel.setMinWidth(dp(36));
        volLabel.setGravity(android.view.Gravity.END);

        // Seekbar
        SeekBar seekBar = new SeekBar(requireContext());
        seekBar.setMax(100);
        seekBar.setProgress((int) (initialVol * 100));
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (!fromUser) return;
                float vol = p / 100f;
                volLabel.setText(p + "%");
                if (p == 0) {
                    muted[0] = true;
                    muteBtn.setText("🔇");
                } else {
                    muted[0] = false;
                    muteBtn.setText("🔊");
                }
                volCb.onVolume(vol);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        muteBtn.setOnClickListener(v -> {
            muted[0] = !muted[0];
            muteBtn.setText(muted[0] ? "🔇" : "🔊");
            seekBar.setProgress(muted[0] ? 0 : 80);
            volLabel.setText(muted[0] ? "0%" : "80%");
            muteCb.onMute(muted[0]);
            volCb.onVolume(muted[0] ? 0f : 0.8f);
        });

        row.addView(muteBtn);
        row.addView(seekBar);
        row.addView(volLabel);

        return row;
    }

    private View buildSectionHeader(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xCCFFFFFF);
        tv.setTextSize(14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(2);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View buildDivider() {
        View v = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(16);
        lp.bottomMargin = dp(12);
        v.setLayoutParams(lp);
        v.setBackgroundColor(0x22FFFFFF);
        return v;
    }

    private int dp(int val) {
        return (int) (val * requireContext().getResources().getDisplayMetrics().density);
    }

    interface VolumeCallback { void onVolume(float vol); }
    interface MuteCallback   { void onMute(boolean muted); }
}
