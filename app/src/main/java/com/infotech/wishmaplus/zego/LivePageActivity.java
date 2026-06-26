package com.infotech.wishmaplus.zego;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.infotech.wishmaplus.Api.Response.BasicResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.UtilMethods;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoIMSendCustomCommandCallback;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason;
import im.zego.zegoexpress.constants.ZegoStreamResourceMode;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.constants.ZegoVideoMirrorMode;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoBeautifyOption;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoPlayerConfig;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;


public class LivePageActivity extends AppCompatActivity {

    private String userID;
    private String userName;
    private String roomID;
    private boolean isHost;
    private boolean isCoHost = false;

    // ✅ FIX: Track if host manually triggered end (to avoid onPause ending live)
    private boolean isEndingLive = false;

    // Track multiple playing streams
    private Map<String, String> playingStreams = new HashMap<>(); // streamID -> userID
    private String mainStreamID = null;
    public CustomLoader loader;

    // UI Controls
    private ImageButton micButton;
    private ImageButton cameraButton;
    private ImageButton flipCameraButton;
    private ImageButton beautyFilterButton;
    private ImageButton speakerButton;
    private com.google.android.material.button.MaterialButton endLiveButton;
    private TextView viewerCountText;
    private LinearLayout layoutViewers;
    private LinearLayout liveIndicator;
    private LinearLayout waitingForHostText;

    private LinearLayout participantAvatarsLayout;

    // State tracking
    private boolean isMicEnabled = true;
    private boolean isCameraEnabled = true;
    private boolean isFrontCamera = true;
    private boolean isBeautyEnabled = false;
    private boolean isSpeakerEnabled = true;
    private Map<String, ParticipantInfo> participants = new HashMap<>();
    private Set<String> coHosts = new HashSet<>();
    private boolean isHostPresent = false;

    // Bottom sheet
    private BottomSheetDialog bottomSheetDialog;
    private ParticipantAdapter participantAdapter;

    private static final int[] AVATAR_COLORS = {
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4"),
            Color.parseColor("#FFEAA7"),
            Color.parseColor("#DDA0DD"),
            Color.parseColor("#98D8C8"),
            Color.parseColor("#F7DC6F")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.live_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.liveView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userID = getIntent().getStringExtra("userID");
        userName = getIntent().getStringExtra("userName");
        roomID = getIntent().getStringExtra("roomID");
        isHost = getIntent().getBooleanExtra("isHost", false);
        loader = new CustomLoader(this, android.R.style.Theme_Translucent_NoTitleBar);

        initializeViews();
        setupClickListeners();
        startListenEvent();
        loginRoom();
    }

    private void initializeViews() {
        micButton = findViewById(R.id.micButton);
        cameraButton = findViewById(R.id.cameraButton);
        flipCameraButton = findViewById(R.id.flipCameraButton);
        beautyFilterButton = findViewById(R.id.beautyFilterButton);
        speakerButton = findViewById(R.id.speakerButton);
        endLiveButton = findViewById(R.id.endLiveButton);
        viewerCountText = findViewById(R.id.viewerCountText);
        layoutViewers = findViewById(R.id.layoutViewers);
        liveIndicator = findViewById(R.id.liveIndicator);
        waitingForHostText = findViewById(R.id.waitingForHostText);
        participantAvatarsLayout = findViewById(R.id.participantAvatarsLayout);

        updateUIForRole();
    }

    private void updateUIForRole() {
        if (isHost || isCoHost) {
            micButton.setVisibility(View.VISIBLE);
            cameraButton.setVisibility(View.VISIBLE);
            flipCameraButton.setVisibility(View.VISIBLE);
            beautyFilterButton.setVisibility(View.VISIBLE);

            if (isHost) {
                viewerCountText.setVisibility(View.VISIBLE);
                layoutViewers.setVisibility(View.VISIBLE);
                liveIndicator.setVisibility(View.VISIBLE);
                endLiveButton.setVisibility(View.VISIBLE);
            }

            waitingForHostText.setVisibility(View.GONE);
        } else {
            speakerButton.setVisibility(View.GONE);
            micButton.setVisibility(View.GONE);
            cameraButton.setVisibility(View.GONE);
            flipCameraButton.setVisibility(View.GONE);
            beautyFilterButton.setVisibility(View.GONE);
            viewerCountText.setVisibility(View.GONE);
            layoutViewers.setVisibility(View.GONE);
            liveIndicator.setVisibility(View.GONE);
            endLiveButton.setVisibility(View.GONE);

            updateWaitingState();
        }
    }

    private void updateWaitingState() {
        if (!isHost && !isCoHost) {
            if (!isHostPresent) {
                waitingForHostText.setVisibility(View.VISIBLE);
                speakerButton.setVisibility(View.GONE);
                liveIndicator.setVisibility(View.GONE);
                findViewById(R.id.hostView).setVisibility(View.GONE);
            } else {
                waitingForHostText.setVisibility(View.GONE);
                speakerButton.setVisibility(View.VISIBLE);
                liveIndicator.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupClickListeners() {

        findViewById(R.id.leaveButton).setOnClickListener(view -> {
            if (isHost) {
                new AlertDialog.Builder(this)
                        .setTitle("End Live Stream")
                        .setMessage("You are currently hosting this live stream. Leaving now will end the live session for all participants. Do you want to continue?")
                        .setPositiveButton("End Live", (dialog, which) -> {
                            showEndLiveConfirmation();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

            } else {
                leaveRoomAsParticipant();
            }
        });

        micButton.setOnClickListener(view -> toggleMicrophone());
        cameraButton.setOnClickListener(view -> toggleCamera());
        flipCameraButton.setOnClickListener(view -> flipCamera());
        beautyFilterButton.setOnClickListener(view -> toggleBeautyFilter());
        speakerButton.setOnClickListener(view -> toggleSpeaker());
        layoutViewers.setOnClickListener(view -> showParticipantsBottomSheet());
        endLiveButton.setOnClickListener(view -> showEndLiveConfirmation());
    }


    private void leaveRoomAsParticipant() {
        if (isCoHost) {
            stopPreview();
            stopPublish();
        }
        for (String streamID : new ArrayList<>(playingStreams.keySet())) {
            ZegoExpressEngine.getEngine().stopPlayingStream(streamID);
        }
        playingStreams.clear();
        stopListenEvent();
        logoutRoom();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isFinishing()) {
            if (isHost || isCoHost) {
                stopPreview();
                stopPublish();
            }
            for (String streamID : new ArrayList<>(playingStreams.keySet())) {
                stopPlayStream(streamID);
            }
            stopListenEvent();
            logoutRoom();
        }
    }


    protected void onPause() {
        super.onPause();
    }

    // ========== Stream Management ==========

    void startPreview() {
        ZegoExpressEngine.getEngine().stopPreview();
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            View hostView = findViewById(R.id.hostView);
            hostView.setVisibility(View.VISIBLE);
            ZegoCanvas previewCanvas = new ZegoCanvas(hostView);
            previewCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
            ZegoExpressEngine.getEngine().startPreview(previewCanvas);
        }, 100);
    }

    void stopPreview() {
        ZegoExpressEngine.getEngine().stopPreview();
    }

    void loginRoom() {
        ZegoUser user = new ZegoUser(userID, userName);
        ZegoRoomConfig roomConfig = new ZegoRoomConfig();
        roomConfig.isUserStatusNotify = true;

        ZegoExpressEngine.getEngine().loginRoom(roomID, user, roomConfig, (int error, JSONObject extendedData) -> {
            if (error == 0) {
                if (isHost) {
                    isHostPresent = true;
                    new android.os.Handler(getMainLooper()).postDelayed(() -> {
                        initializeHostSettings();
                        startPreview();
                        new android.os.Handler(getMainLooper()).postDelayed(() -> {
                            startPublish();
                            notifyHostPresence(true);
                        }, 500);
                    }, 500);
                }
            } else {
                Toast.makeText(this, "Login failed. error = " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    void logoutRoom() {
        ZegoExpressEngine.getEngine().logoutRoom();
    }

    void startPublish() {
        String streamID = roomID + "_" + userID + "_call";
        ZegoExpressEngine.getEngine().startPublishingStream(streamID);
    }

    void stopPublish() {
        ZegoExpressEngine.getEngine().stopPublishingStream();
    }

    void startPlayStream(String streamID) {
        String myStreamID = roomID + "_" + userID + "_call";
        if (streamID.equals(myStreamID)) return;
        if (playingStreams.containsKey(streamID)) return;

        ZegoCanvas playCanvas;
        View targetView;

        if (isHost || isCoHost) {
            targetView = findViewById(R.id.participantView);
            targetView.setVisibility(View.VISIBLE);
            playCanvas = new ZegoCanvas(targetView);
        } else {
            if (mainStreamID == null) {
                targetView = findViewById(R.id.hostView);
                targetView.setVisibility(View.VISIBLE);
                mainStreamID = streamID;
                waitingForHostText.setVisibility(View.GONE);
            } else {
                targetView = findViewById(R.id.participantView);
                targetView.setVisibility(View.VISIBLE);
            }
            playCanvas = new ZegoCanvas(targetView);
        }

        playCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
        ZegoPlayerConfig config = new ZegoPlayerConfig();
        config.resourceMode = ZegoStreamResourceMode.DEFAULT;
        ZegoExpressEngine.getEngine().startPlayingStream(streamID, playCanvas, config);

        String streamUserID = extractUserIDFromStreamID(streamID);
        playingStreams.put(streamID, streamUserID);
        checkIfHostStream(streamID);
    }

    void stopPlayStream(String streamID) {
        if (!playingStreams.containsKey(streamID)) return;

        ZegoExpressEngine.getEngine().stopPlayingStream(streamID);
        playingStreams.remove(streamID);

        if (streamID.equals(mainStreamID)) {
            mainStreamID = null;
            findViewById(R.id.hostView).setVisibility(View.GONE);

            if (!playingStreams.isEmpty() && !(isHost || isCoHost)) {
                String nextStreamID = playingStreams.keySet().iterator().next();
                stopPlayStream(nextStreamID);
                startPlayStream(nextStreamID);
            } else if (!isHost && !isCoHost) {
                updateWaitingState();
            }
        }

        if (isHost || isCoHost) {
            if (playingStreams.isEmpty()) {
                findViewById(R.id.participantView).setVisibility(View.GONE);
            }
        } else {
            boolean hasSecondaryStream = playingStreams.size() > 1 ||
                    (playingStreams.size() == 1 && mainStreamID != null);
            if (!hasSecondaryStream) {
                findViewById(R.id.participantView).setVisibility(View.GONE);
            }
        }
    }

    private String extractUserIDFromStreamID(String streamID) {
        String[] parts = streamID.split("_");
        if (parts.length >= 2) return parts[1];
        return streamID;
    }

    private void checkIfHostStream(String streamID) {
        if (!isHost && !isCoHost) {
            isHostPresent = true;
            runOnUiThread(() -> updateWaitingState());
        }
    }

    // ========== Host Feature Controls ==========

    private void initializeHostSettings() {
        ZegoExpressEngine.getEngine().setVideoMirrorMode(ZegoVideoMirrorMode.ONLY_PREVIEW_MIRROR);
        ZegoExpressEngine.getEngine().muteMicrophone(false);
        ZegoExpressEngine.getEngine().enableCamera(true);
        ZegoExpressEngine.getEngine().enableAEC(true);
        ZegoExpressEngine.getEngine().enableAGC(true);
        ZegoExpressEngine.getEngine().enableANS(true);
    }

    private void enableCoHostFeatures() {
        isCoHost = true;
        updateUIForRole();
        runOnUiThread(() -> {
            initializeHostSettings();
            startPreview();
            startPublish();
            Toast.makeText(this, "You can now stream as a co-host!", Toast.LENGTH_LONG).show();
        });
    }

    private void disableCoHostFeatures() {
        isCoHost = false;
        updateUIForRole();
        runOnUiThread(() -> {
            stopPreview();
            stopPublish();
            findViewById(R.id.hostView).setVisibility(View.GONE);
            Toast.makeText(this, "Co-host features disabled", Toast.LENGTH_LONG).show();
        });
    }

    private void toggleMicrophone() {
        isMicEnabled = !isMicEnabled;
        ZegoExpressEngine.getEngine().muteMicrophone(!isMicEnabled);
        micButton.setImageResource(isMicEnabled ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
        Toast.makeText(this, isMicEnabled ? "Microphone enabled" : "Microphone muted", Toast.LENGTH_SHORT).show();
    }

    private void toggleCamera() {
        isCameraEnabled = !isCameraEnabled;
        ZegoExpressEngine.getEngine().enableCamera(isCameraEnabled);
        cameraButton.setImageResource(isCameraEnabled ? R.drawable.ic_video_on : R.drawable.ic_video_off);
        Toast.makeText(this, isCameraEnabled ? "Camera enabled" : "Camera disabled", Toast.LENGTH_SHORT).show();
    }

    private void flipCamera() {
        isFrontCamera = !isFrontCamera;
        ZegoExpressEngine.getEngine().useFrontCamera(isFrontCamera);
        Toast.makeText(this, isFrontCamera ? "Switched to front camera" : "Switched to back camera", Toast.LENGTH_SHORT).show();
    }

    private void toggleBeautyFilter() {
        isBeautyEnabled = !isBeautyEnabled;
        ZegoBeautifyOption option = new ZegoBeautifyOption();
        if (isBeautyEnabled) {
            option.polishStep = 0.5;
            option.sharpenFactor = 0.3;
            option.whitenFactor = 0.3;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                beautyFilterButton.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_light));
            }
            Toast.makeText(this, "Beauty filter enabled", Toast.LENGTH_SHORT).show();
        } else {
            option.polishStep = 0.0;
            beautyFilterButton.setBackgroundTintList(null);
            Toast.makeText(this, "Beauty filter disabled", Toast.LENGTH_SHORT).show();
        }
        ZegoExpressEngine.getEngine().enableBeautify(isBeautyEnabled ? 1 : 0);
    }

    private void toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled;
        ZegoExpressEngine.getEngine().muteSpeaker(!isSpeakerEnabled);
        speakerButton.setImageResource(isSpeakerEnabled ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
        Toast.makeText(this, isSpeakerEnabled ? "Speaker enabled" : "Speaker muted", Toast.LENGTH_SHORT).show();
    }

    private void updateViewerCount() {
        int viewerCount = participants.size();
        viewerCountText.setText(viewerCount + "");
    }

    //  NEW: Show participant name + colored avatar bubble on screen
    private void updateParticipantAvatars() {
        if (participantAvatarsLayout == null) return;

        runOnUiThread(() -> {
            participantAvatarsLayout.removeAllViews();

            int colorIndex = 0;
            int maxShow = 5;
            int total = participants.size();
            int shown = 0;

            for (ParticipantInfo info : participants.values()) {
                if (shown >= maxShow) break;

                TextView avatarView = new TextView(this);
                int size = (int) (40 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(4, 0, 4, 0);
                avatarView.setLayoutParams(params);
                String initials = info.userName != null && info.userName.length() > 0
                        ? String.valueOf(info.userName.charAt(0)).toUpperCase()
                        : "?";
                avatarView.setText(initials);
                avatarView.setTextColor(Color.WHITE);
                avatarView.setTextSize(14);
                avatarView.setGravity(android.view.Gravity.CENTER);
                avatarView.setBackgroundColor(AVATAR_COLORS[colorIndex % AVATAR_COLORS.length]);

                // Make it circular
                avatarView.setPadding(0, 0, 0, 0);
                android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                circle.setColor(AVATAR_COLORS[colorIndex % AVATAR_COLORS.length]);
                avatarView.setBackground(circle);

                // Click pe name toast
                final String participantName = info.userName;
                final boolean isParticipantCoHost = info.isCoHost;
                avatarView.setOnClickListener(v -> {
                    String role = isParticipantCoHost ? " (Co-Host)" : "";
                    Toast.makeText(this, participantName + role, Toast.LENGTH_SHORT).show();
                });

                participantAvatarsLayout.addView(avatarView);
                colorIndex++;
                shown++;
            }

            if (total > maxShow) {
                TextView moreView = new TextView(this);
                int size = (int) (40 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(4, 0, 4, 0);
                moreView.setLayoutParams(params);
                moreView.setText("+" + (total - maxShow));
                moreView.setTextColor(Color.WHITE);
                moreView.setTextSize(11);
                moreView.setGravity(android.view.Gravity.CENTER);

                android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                circle.setColor(Color.parseColor("#555555"));
                moreView.setBackground(circle);
                moreView.setOnClickListener(v -> showParticipantsBottomSheet());

                participantAvatarsLayout.addView(moreView);
            }
        });
    }

    private void showEndLiveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("End Live Stream")
                .setMessage("Are you sure you want to end this live stream? All participants will be disconnected from the room.")
                .setPositiveButton("End Live", (dialog, which) -> endLiveStream())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endLiveStream() {
        if (isEndingLive) return;
        isEndingLive = true;

        loader.show();
        UtilMethods.INSTANCE.endLive(roomID, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                if (loader != null && loader.isShowing()) loader.dismiss();

                BasicResponse response = (BasicResponse) object;
                if (response.getStatusCode() == 1) {
                    try {
                        JSONObject commandData = new JSONObject();
                        commandData.put("action", "END_LIVE");

                        // Sabko notify karo
                        for (ParticipantInfo participant : participants.values()) {
                            ArrayList<ZegoUser> targetUsers = new ArrayList<>();
                            targetUsers.add(new ZegoUser(participant.userID));
                            ZegoExpressEngine.getEngine().sendCustomCommand(roomID, commandData.toString(), targetUsers, null);
                        }

                        runOnUiThread(() ->
                                Toast.makeText(LivePageActivity.this, "Live stream ended", Toast.LENGTH_SHORT).show()
                        );
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    stopPreview();
                    stopPublish();
                    notifyHostPresence(false);

                    new android.os.Handler().postDelayed(() -> finish(), 500);
                } else {
                    isEndingLive = false; // Reset if API failed
                    if (loader != null && loader.isShowing()) loader.dismiss();
                }
            }

            @Override
            public void onError(String msg) {
                isEndingLive = false;
                if (loader != null && loader.isShowing()) loader.dismiss();
                runOnUiThread(() ->
                        Toast.makeText(LivePageActivity.this, "Error ending live: " + msg, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void notifyHostPresence(boolean isPresent) {
        try {
            JSONObject commandData = new JSONObject();
            commandData.put("action", "HOST_PRESENCE");
            commandData.put("isPresent", isPresent);

            for (ParticipantInfo participant : participants.values()) {
                ArrayList<ZegoUser> targetUsers = new ArrayList<>();
                targetUsers.add(new ZegoUser(participant.userID));
                ZegoExpressEngine.getEngine().sendCustomCommand(roomID, commandData.toString(), targetUsers, null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ========== Participants Management ==========

    private void showParticipantsBottomSheet() {
        if (bottomSheetDialog == null) {
            bottomSheetDialog = new BottomSheetDialog(this);
            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_participants, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            RecyclerView recyclerView = bottomSheetView.findViewById(R.id.participantsRecyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            participantAdapter = new ParticipantAdapter(new ParticipantAdapter.OnParticipantActionListener() {
                @Override
                public void onRemoveParticipant(ParticipantInfo participant) {
                    showRemoveConfirmation(participant);
                }

                @Override
                public void onPromoteToCoHost(ParticipantInfo participant) {
                    promoteToCoHost(participant);
                }

                @Override
                public void onDemoteFromCoHost(ParticipantInfo participant) {
                    demoteFromCoHost(participant);
                }
            });

            recyclerView.setAdapter(participantAdapter);

            Button inviteButton = bottomSheetView.findViewById(R.id.inviteButton);
            inviteButton.setOnClickListener(v -> inviteParticipants());
        }

        updateParticipantList();
        bottomSheetDialog.show();
    }

    private void updateParticipantList() {
        List<ParticipantInfo> participantList = new ArrayList<>(participants.values());
        if (participantAdapter != null) {
            participantAdapter.setParticipants(participantList);
        }
    }

    private void inviteParticipants() {
        String inviteLink = "Join my live stream! Room ID: " + roomID;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Room ID", inviteLink);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Invite link copied to clipboard!", Toast.LENGTH_LONG).show();
    }

    private void showRemoveConfirmation(ParticipantInfo participant) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Participant")
                .setMessage("Are you sure you want to remove "
                        + participant.userName
                        + " from the live room?")
                .setPositiveButton("Remove", (dialog, which) -> removeParticipant(participant))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeParticipant(ParticipantInfo participant) {
        try {
            JSONObject commandData = new JSONObject();
            commandData.put("action", "REMOVE");
            commandData.put("targetUserID", participant.userID);

            ArrayList<ZegoUser> targetUsers = new ArrayList<>();
            targetUsers.add(new ZegoUser(participant.userID));

            ZegoExpressEngine.getEngine().sendCustomCommand(roomID, commandData.toString(), targetUsers, new IZegoIMSendCustomCommandCallback() {
                @Override
                public void onIMSendCustomCommandResult(int errorCode) {
                    runOnUiThread(() -> {
                        if (errorCode == 0) {
                            participants.remove(participant.userID);
                            coHosts.remove(participant.userID);
                            updateViewerCount();
                            updateParticipantList();
                            updateParticipantAvatars();
                            Toast.makeText(LivePageActivity.this, participant.userName + " removed", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LivePageActivity.this, "Failed to remove. Error: " + errorCode, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void promoteToCoHost(ParticipantInfo participant) {
        try {
            JSONObject commandData = new JSONObject();
            commandData.put("action", "PROMOTE_COHOST");
            commandData.put("targetUserID", participant.userID);

            ArrayList<ZegoUser> targetUsers = new ArrayList<>();
            targetUsers.add(new ZegoUser(participant.userID));

            ZegoExpressEngine.getEngine().sendCustomCommand(roomID, commandData.toString(), targetUsers, new IZegoIMSendCustomCommandCallback() {
                @Override
                public void onIMSendCustomCommandResult(int errorCode) {
                    runOnUiThread(() -> {
                        if (errorCode == 0) {
                            coHosts.add(participant.userID);
                            participant.isCoHost = true;
                            updateParticipantList();
                            updateParticipantAvatars();
                            Toast.makeText(LivePageActivity.this, participant.userName + " is now a co-host", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void demoteFromCoHost(ParticipantInfo participant) {
        try {
            JSONObject commandData = new JSONObject();
            commandData.put("action", "DEMOTE_COHOST");
            commandData.put("targetUserID", participant.userID);

            ArrayList<ZegoUser> targetUsers = new ArrayList<>();
            targetUsers.add(new ZegoUser(participant.userID));

            ZegoExpressEngine.getEngine().sendCustomCommand(roomID, commandData.toString(), targetUsers, new IZegoIMSendCustomCommandCallback() {
                @Override
                public void onIMSendCustomCommandResult(int errorCode) {
                    runOnUiThread(() -> {
                        if (errorCode == 0) {
                            coHosts.remove(participant.userID);
                            participant.isCoHost = false;
                            updateParticipantList();
                            updateParticipantAvatars();
                            Toast.makeText(LivePageActivity.this, participant.userName + " removed from co-host", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleCustomCommand(String fromUserID, String content) {
        try {
            JSONObject commandData = new JSONObject(content);
            String action = commandData.getString("action");

            if ("END_LIVE".equals(action)) {
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "The host has ended the live stream.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
                return;
            } else if ("HOST_PRESENCE".equals(action)) {
                boolean isPresent = commandData.getBoolean("isPresent");
                isHostPresent = isPresent;
                runOnUiThread(() -> updateWaitingState());
                return;
            }

            if (!commandData.has("targetUserID")) return;
            String targetUserID = commandData.getString("targetUserID");

            if (targetUserID.equals(userID)) {
                switch (action) {
                    case "REMOVE":
                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    "You have been removed from the live room by the host.",
                                    Toast.LENGTH_LONG).show();
                            leaveRoomAsParticipant();
                        });
                        break;
                    case "PROMOTE_COHOST":
                        enableCoHostFeatures();
                        break;
                    case "DEMOTE_COHOST":
                        disableCoHostFeatures();
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ========== Event Listeners ==========

    void startListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(new IZegoEventHandler() {
            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList, JSONObject extendedData) {
                super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData);

                if (updateType == ZegoUpdateType.ADD) {
                    for (ZegoStream stream : streamList) {
                        startPlayStream(stream.streamID);
                    }
                } else {
                    for (ZegoStream stream : streamList) {
                        stopPlayStream(stream.streamID);
                    }
                    if (!isHost && !isCoHost && playingStreams.isEmpty()) {
                        isHostPresent = false;
                        runOnUiThread(() -> updateWaitingState());
                    }
                }
            }

            @Override
            public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
                super.onRoomUserUpdate(roomID, updateType, userList);

                if (updateType == ZegoUpdateType.ADD) {
                    for (ZegoUser user : userList) {
                        if (!user.userID.equals(userID)) {
                            boolean isUserCoHost = coHosts.contains(user.userID);
                            participants.put(user.userID, new ParticipantInfo(user.userID, user.userName, isUserCoHost));
                            Toast.makeText(getApplicationContext(), user.userName + " joined", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (updateType == ZegoUpdateType.DELETE) {
                    for (ZegoUser user : userList) {
                        participants.remove(user.userID);
                        coHosts.remove(user.userID);
                        Toast.makeText(getApplicationContext(), user.userName + " left", Toast.LENGTH_SHORT).show();
                    }
                }

                runOnUiThread(() -> {
                    if (isHost) updateViewerCount();
                    updateParticipantAvatars();
                    if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
                        updateParticipantList();
                    }
                });
            }

            @Override
            public void onIMRecvCustomCommand(String roomID, ZegoUser fromUser, String command) {
                super.onIMRecvCustomCommand(roomID, fromUser, command);
                handleCustomCommand(fromUser.userID, command);
            }

            @Override
            public void onRoomStateChanged(String roomID, ZegoRoomStateChangedReason reason, int i, JSONObject jsonObject) {
                super.onRoomStateChanged(roomID, reason, i, jsonObject);
                if (reason == ZegoRoomStateChangedReason.LOGIN_FAILED) {
                    Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_LONG).show();
                } else if (reason == ZegoRoomStateChangedReason.RECONNECT_FAILED) {
                    Toast.makeText(getApplicationContext(), "Reconnection failed", Toast.LENGTH_LONG).show();
                } else if (reason == ZegoRoomStateChangedReason.KICK_OUT) {
                    Toast.makeText(getApplicationContext(), "Kicked out", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData);
                if (errorCode != 0) {
                    Toast.makeText(getApplicationContext(), "Publishing error: " + errorCode, Toast.LENGTH_LONG).show();
                } else if (state == ZegoPublisherState.PUBLISHING) {
                    runOnUiThread(() -> {
                        if (isHost) liveIndicator.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData);
                if (errorCode != 0) {
                    Toast.makeText(getApplicationContext(), "Playing error: " + errorCode, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    void stopListenEvent() {
        ZegoExpressEngine.getEngine().setEventHandler(null);
    }
}