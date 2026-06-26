package com.infotech.wishmaplus.PageAccess.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.infotech.wishmaplus.Api.Response.BasicResponse;
import com.infotech.wishmaplus.PageAccess.adapter.PendingInvitesAdapter;
import com.infotech.wishmaplus.PageAccess.model.ModeratorInvite;
import com.infotech.wishmaplus.PageAccess.model.PendingInvitesResponse;
import com.infotech.wishmaplus.PageAccess.model.RespondToInviteRequest;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.ApiClient;
import com.infotech.wishmaplus.Utils.EndPointInterface;
import com.infotech.wishmaplus.Utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PendingInvitesActivity extends AppCompatActivity
        implements PendingInvitesAdapter.OnInviteActionListener {

    private final List<ModeratorInvite> inviteList = new ArrayList<>();
    // ── Views ────────────────────────────────────────────────────────────────
    private RecyclerView rvPendingInvites;
    private LinearLayout layoutLoading, layoutEmpty, layoutError;
    private TextView tvErrorMsg;
    private View btnRetry;
    // ── Data / Adapter ───────────────────────────────────────────────────────
    private PendingInvitesAdapter adapter;
    // ── Network ──────────────────────────────────────────────────────────────
    private EndPointInterface apiService;
    private PreferencesManager tokenManager;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pending_invites);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
        initViews();
        initToolbar();
        initDependencies();
        setupRecyclerView();
        fetchPendingInvites();
    }

    // ── Init helpers ─────────────────────────────────────────────────────────

    private void initViews() {
        rvPendingInvites = findViewById(R.id.rvPendingInvites);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutError = findViewById(R.id.layoutError);
        tvErrorMsg = findViewById(R.id.tvErrorMsg);
        btnRetry = findViewById(R.id.btnRetry);

        btnRetry.setOnClickListener(v -> fetchPendingInvites());
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initDependencies() {
         tokenManager = new PreferencesManager(this,1);
        apiService = ApiClient.getClient().create(EndPointInterface.class);
    }

    private void setupRecyclerView() {
        adapter = new PendingInvitesAdapter(this, inviteList, this);
        rvPendingInvites.setLayoutManager(new LinearLayoutManager(this));
        rvPendingInvites.setAdapter(adapter);

        // Add item animation for smooth removal
        rvPendingInvites.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
    }

    // ── API: Fetch pending invites ───────────────────────────────────────────

    private void fetchPendingInvites() {
        showState(State.LOADING);

        String token = "Bearer " + tokenManager.getAccessToken();

        apiService.getPendingInvites(token)
                .enqueue(new Callback<PendingInvitesResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<PendingInvitesResponse> call,
                                           @NonNull Response<PendingInvitesResponse> response) {

                        if (!isDestroyed()) {
                            if (response.isSuccessful() && response.body() != null) {
                                handleInvitesResponse(response.body());
                            } else {
                                String msg = "Server error: " + response.code();
                                showError(msg);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PendingInvitesResponse> call,
                                          @NonNull Throwable t) {
                        if (!isDestroyed()) {
                            // Network failure (no internet, timeout, etc.)
                            showError("Network error. Please check your connection.");
                        }
                    }
                });
    }

    private void handleInvitesResponse(PendingInvitesResponse body) {
        if (body.isSuccess()) {
            List<ModeratorInvite> list = body.getResult();
            if (list != null && !list.isEmpty()) {
                inviteList.clear();
                inviteList.addAll(list);
                adapter.notifyDataSetChanged();
                showState(State.CONTENT);
            } else {
                showState(State.EMPTY);
            }
        } else {
            // API returned statusCode != 1
            showError(body.getResponseText() != null
                    ? body.getResponseText()
                    : "Failed to load invites.");
        }
    }

    // ── API: Respond to invite ───────────────────────────────────────────────

    @Override
    public void onAccept(ModeratorInvite invite, int position) {
        confirmAndRespond(invite, position, true);
    }

    @Override
    public void onDecline(ModeratorInvite invite, int position) {
        confirmAndRespond(invite, position, false);
    }

    /**
     * Shows a confirmation dialog before accept/decline to prevent accidents.
     */
    private void confirmAndRespond(ModeratorInvite invite, int position, boolean accept) {
        String action = accept ? "accept" : "decline";
        String message = "Are you sure you want to "
                + action
                + " the moderator invite for "
                + invite.getPageName()
                + "?";

        new AlertDialog.Builder(this)
                .setTitle(accept ? "Accept Invite" : "Decline Invite")
                .setMessage(message)
                .setPositiveButton(accept ? "Accept" : "Decline", (dialog, which) ->
                        respondToInvite(invite, position, accept))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void respondToInvite(ModeratorInvite invite, int position, boolean accept) {
        adapter.setLoading(position, true);

        String token = "Bearer " + tokenManager.getAccessToken();
        RespondToInviteRequest requestBody =
                new RespondToInviteRequest(invite.getModeratorId(), accept);

        apiService.respondToInvite(token, requestBody)
                .enqueue(new Callback<BasicResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<BasicResponse> call,
                                           @NonNull Response<BasicResponse> response) {

                        if (isDestroyed()) return;

                        adapter.setLoading(position, false);

                        if (response.isSuccessful() && response.body() != null) {
                            BasicResponse body = response.body();

                            if (body.getStatusCode()==1) {
                                // Remove card with animation
                                adapter.removeItem(position);
                                String msg = accept
                                        ? "You are now a moderator of \"" + invite.getPageName() + "\"."
                                        : "The moderator invite has been declined.";
                                showToast(msg);

                                // If last invite removed, show empty state
                                if (inviteList.isEmpty()) showState(State.EMPTY);

                            } else {
                                // API-level failure (e.g. "Invite not found or already responded")
                                showToast(body.getResponseText() != null
                                        ? body.getResponseText()
                                        : "Could not process invite. Please try again.");
                            }

                        } else {
                            showToast("Server error " + response.code() + ". Please try again.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BasicResponse> call,
                                          @NonNull Throwable t) {
                        if (isDestroyed()) return;
                        adapter.setLoading(position, false);
                        showToast("Network error. Please check your connection.");
                    }
                });
    }

    // ── UI State helpers ─────────────────────────────────────────────────────

    private void showState(State state) {
        layoutLoading.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        rvPendingInvites.setVisibility(state == State.CONTENT ? View.VISIBLE : View.GONE);
        layoutEmpty.setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvErrorMsg.setText(message != null ? message : "Something went wrong.");
        showState(State.ERROR);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private enum State {LOADING, CONTENT, EMPTY, ERROR}
}