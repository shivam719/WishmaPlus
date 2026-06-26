package com.infotech.wishmaplus.PageAccess.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.infotech.wishmaplus.Api.Response.BasicResponse;
import com.infotech.wishmaplus.PageAccess.BottomSheet.ModeratorDetailBottomSheet;
import com.infotech.wishmaplus.PageAccess.adapter.ModeratorAdapter;
import com.infotech.wishmaplus.PageAccess.adapter.SuggestedUserAdapter;
import com.infotech.wishmaplus.PageAccess.model.SuggestedModeratorsResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.UtilMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PageAccessActivity extends AppCompatActivity {

    private static final String TAG = "PageAccessActivity";
    private static final long SEARCH_DEBOUNCE_MS = 400;
    // ── Add People bottom sheets (search + invite) ───────────────────────
    private final List<SuggestedModeratorsResponse.SuggestedUser> suggestedList = new ArrayList<>();
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private String pageId = "";
    // ── PEOPLE WITH PAGE ACCESS (list) ───────────────────────────────────
    private LinearLayout loadingState, emptyState, errorState;
    private TextView tvErrorMsg, tvRetry;
    private RecyclerView rvModerators;
    private ModeratorAdapter adapter;
    // ── PEOPLE WITH TASK ACCESS (list) ───────────────────────────────────
    private LinearLayout loadingStateTask, emptyStateTask, errorStateTask;
    private TextView tvErrorMsgTask, tvRetryTask;
    private RecyclerView rvTaskAccess;
    private ModeratorAdapter taskAdapter;
    private SuggestedUserAdapter suggestedAdapter;
    private Runnable searchRunnable;

    private BottomSheetDialog bottomSheetAddPeople;
    private BottomSheetDialog bottomSheetInvitePermissions;
    private BottomSheetDialog bottomSheetPageAccessDialog;
    private SwipeRefreshLayout swipeRefresh;
    // ── Pull-to-refresh bookkeeping ───────────────────────────────────────
    // SwipeRefreshLayout wraps BOTH sections (page access + task access),
    // so we wait for both async loads to settle before hiding the spinner.
    private int pendingSwipeRefreshCalls = 0;

    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_page_access);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        if (getIntent() != null && getIntent().hasExtra("pageId")
                && getIntent().getStringExtra("pageId") != null
                && !Objects.requireNonNull(getIntent().getStringExtra("pageId")).trim().isEmpty()) {
            pageId = Objects.requireNonNull(getIntent().getStringExtra("pageId")).trim();
        }

        bindViews();
        setupNavigation();

        loadPageAccessList();   // PEOPLE WITH PAGE ACCESS  → GetModerators
        loadTaskAccessList();   // PEOPLE WITH TASK ACCESS  → (no list API yet, shows empty)
    }

    // ── Bind ─────────────────────────────────────────────────────────────
    private void bindViews() {
        loadingState = findViewById(R.id.loadingState);
        emptyState = findViewById(R.id.emptyState);
        errorState = findViewById(R.id.errorState);
        tvErrorMsg = findViewById(R.id.tvErrorMsg);
        tvRetry = findViewById(R.id.tvRetry);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvModerators = findViewById(R.id.rvModerators);

        rvModerators.setLayoutManager(new LinearLayoutManager(this));
        rvModerators.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        loadingStateTask = findViewById(R.id.loadingStateTask);
        emptyStateTask = findViewById(R.id.emptyStateTask);
        errorStateTask = findViewById(R.id.errorStateTask);
        tvErrorMsgTask = findViewById(R.id.tvErrorMsgTask);
        tvRetryTask = findViewById(R.id.tvRetryTask);
        rvTaskAccess = findViewById(R.id.rvTaskAccess);

        rvTaskAccess.setLayoutManager(new LinearLayoutManager(this));
        rvTaskAccess.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        findViewById(R.id.infoBanner).setOnClickListener(v -> openPageAccessInfoBottomSheet());

        swipeRefresh.setOnRefreshListener(this::onSwipeToRefresh);
    }

    // ── Navigation ───────────────────────────────────────────────────────
    private void setupNavigation() {
        AppCompatImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        // Toolbar "+ Add" → invite for PAGE ACCESS (uses GetPageAccessModerators for search)
        TextView btnAddPageAccess = findViewById(R.id.btnAddModerator);
        btnAddPageAccess.setOnClickListener(v ->
                openAddPeopleBottomSheet(AddPeopleMode.PAGE_ACCESS));

        // Task Access section "+ Add" → invite for TASK ACCESS (uses GetTaskAccessModerators for search)
        TextView btnAddTaskAccess = findViewById(R.id.btnAddTaskAccess);
        btnAddTaskAccess.setOnClickListener(v ->
                openAddPeopleBottomSheet(AddPeopleMode.TASK_ACCESS));

        tvRetry.setOnClickListener(v -> loadPageAccessList());
        tvRetryTask.setOnClickListener(v -> loadTaskAccessList());
    }

    // ── Pull-to-refresh ──────────────────────────────────────────────────
    private void onSwipeToRefresh() {
        pendingSwipeRefreshCalls = 2; // page access + task access
        loadPageAccessList();
        loadTaskAccessList();
    }

    private void notifySwipeRefreshSectionDone() {
        if (pendingSwipeRefreshCalls > 0) {
            pendingSwipeRefreshCalls--;
        }
        if (pendingSwipeRefreshCalls <= 0 && swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    // =========================================================================
    // PEOPLE WITH PAGE ACCESS — list (GetModerators)
    // =========================================================================
    private void loadPageAccessList() {
        showLoading();
        UtilMethods.INSTANCE.getModerators(pageId, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                SuggestedModeratorsResponse response = (SuggestedModeratorsResponse) object;

                if (response.getStatusCode() != 1) {
                    String msg = response.getResponseText() != null ? response.getResponseText() : "Failed to load";
                    showError(msg);
                    return;
                }

                List<SuggestedModeratorsResponse.SuggestedUser> mods = response.getResult();

                runOnUiThread(() -> {
                    if (mods == null || mods.isEmpty()) {
                        showEmpty();
                    } else {
                        showList(mods);
                    }
                });
            }

            @Override
            public void onError(String msg) {
                Log.e(TAG, "getModerators error: " + msg);
                showError("Could not load page access. Tap Retry.");
            }
        });
    }

    private void showList(List<SuggestedModeratorsResponse.SuggestedUser> mods) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        rvModerators.setVisibility(View.VISIBLE);

        adapter = new ModeratorAdapter(this, mods, this::openDetailSheet, this::showModeratorMenu);
        rvModerators.setAdapter(adapter);

        notifySwipeRefreshSectionDone();
    }

    private void showLoading() {
        runOnUiThread(() -> {
            loadingState.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
            rvModerators.setVisibility(View.GONE);
        });
    }

    private void showEmpty() {
        runOnUiThread(() -> {
            loadingState.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
            rvModerators.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);

            notifySwipeRefreshSectionDone();
        });
    }

    private void showError(String msg) {
        runOnUiThread(() -> {
            loadingState.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            rvModerators.setVisibility(View.GONE);
            errorState.setVisibility(View.VISIBLE);
            tvErrorMsg.setText(msg);

            notifySwipeRefreshSectionDone();
        });
    }

    // =========================================================================
    // PEOPLE WITH TASK ACCESS — list
    // NOTE: No dedicated "list" API has been provided yet for this section.
    // For now this always renders the empty state. Once a list API exists
    // (e.g. GetTaskAccessAssignedUsers), wire it here the same way as
    // loadPageAccessList() above.
    // =========================================================================
    private void loadTaskAccessList() {
        showEmptyTask();
    }


    private void showLoadingTask() {
        runOnUiThread(() -> {
            loadingStateTask.setVisibility(View.VISIBLE);
            emptyStateTask.setVisibility(View.GONE);
            errorStateTask.setVisibility(View.GONE);
            rvTaskAccess.setVisibility(View.GONE);
        });
    }

    private void showEmptyTask() {
        runOnUiThread(() -> {
            loadingStateTask.setVisibility(View.GONE);
            errorStateTask.setVisibility(View.GONE);
            rvTaskAccess.setVisibility(View.GONE);
            emptyStateTask.setVisibility(View.VISIBLE);

            notifySwipeRefreshSectionDone();
        });
    }

    private void showErrorTask(String msg) {
        runOnUiThread(() -> {
            loadingStateTask.setVisibility(View.GONE);
            emptyStateTask.setVisibility(View.GONE);
            rvTaskAccess.setVisibility(View.GONE);
            errorStateTask.setVisibility(View.VISIBLE);
            tvErrorMsgTask.setText(msg);

            notifySwipeRefreshSectionDone();
        });
    }

    private void openAddPeopleBottomSheet(AddPeopleMode mode) {
        if (bottomSheetAddPeople != null && bottomSheetAddPeople.isShowing()) return;

        bottomSheetAddPeople = new BottomSheetDialog(this, R.style.DialogStyle);
        Objects.requireNonNull(bottomSheetAddPeople.getWindow())
                .setBackgroundDrawableResource(android.R.color.transparent);

        LayoutInflater inflater = LayoutInflater.from(this);
        View sheetView = inflater.inflate(R.layout.bottom_sheet_add_task_access, null);
        bottomSheetAddPeople.setContentView(sheetView);
        BottomSheetBehavior
                .from(Objects.requireNonNull(bottomSheetAddPeople.findViewById(com.google.android.material.R.id.design_bottom_sheet)))
                .setState(BottomSheetBehavior.STATE_EXPANDED);

        // Optional: set sheet title based on mode if your layout has a title TextView
        TextView tvSheetTitle = sheetView.findViewById(R.id.title);
        if (tvSheetTitle != null) {
            tvSheetTitle.setText(mode == AddPeopleMode.PAGE_ACCESS
                    ? "Give page access"
                    : "Give task access");
        }

        EditText etSearch = sheetView.findViewById(R.id.etSearch);
        ProgressBar progressSearch = sheetView.findViewById(R.id.progressSearch);
        TextView tvNoResults = sheetView.findViewById(R.id.tvNoResults);
        RecyclerView rvSuggested = sheetView.findViewById(R.id.rvSuggested);

        rvSuggested.setLayoutManager(new LinearLayoutManager(this));
        suggestedList.clear();
        suggestedAdapter = new SuggestedUserAdapter(this, suggestedList, (user, anchor) ->
                inviteUser(user, mode, bottomSheetAddPeople)
        );
        rvSuggested.setAdapter(suggestedAdapter);

        // ── Initial load — empty searchText returns the default suggested list ──
        fetchSuggestedUsers(mode, "", progressSearch, tvNoResults, rvSuggested);

        // ── Server-side search as user types (debounced) ───────────────────
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                String query = s.toString().trim();
                searchRunnable = () -> fetchSuggestedUsers(mode, query, progressSearch, tvNoResults, rvSuggested);
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        bottomSheetAddPeople.show();
    }

    // ── Fetches suggested users from the server, passing searchText through
    //    to GetPageAccessModerators / GetTaskAccessModerators so search now
    //    happens API-side instead of client-side filtering ──────────────────
    private void fetchSuggestedUsers(AddPeopleMode mode, String searchText,
                                     ProgressBar progressSearch, TextView tvNoResults,
                                     RecyclerView rvSuggested) {

        progressSearch.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        UtilMethods.ApiCallBackMulti callback = new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                SuggestedModeratorsResponse response = (SuggestedModeratorsResponse) object;

                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);

                    if (response.getStatusCode() != 1 || response.getResult() == null || response.getResult().isEmpty()) {
                        suggestedList.clear();
                        suggestedAdapter.notifyDataSetChanged();
                        rvSuggested.setVisibility(View.GONE);
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("No people found");
                        return;
                    }

                    suggestedList.clear();
                    suggestedList.addAll(response.getResult());
                    suggestedAdapter.notifyDataSetChanged();

                    tvNoResults.setVisibility(View.GONE);
                    rvSuggested.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    rvSuggested.setVisibility(View.GONE);
                    tvNoResults.setVisibility(View.VISIBLE);
                    tvNoResults.setText("Could not load people. Try again.");
                });
            }
        };

        // ── Mode-specific suggested users API, now with searchText ─────────
        if (mode == AddPeopleMode.PAGE_ACCESS) {
            UtilMethods.INSTANCE.getPageAccessModerators(pageId, searchText, callback);
        } else {
            UtilMethods.INSTANCE.getTaskAccessModerators(pageId, searchText, callback);
        }
    }

    // =========================================================================
    // INVITE — permission selection bottom sheet
    // =========================================================================
    private void inviteUser(SuggestedModeratorsResponse.SuggestedUser user, AddPeopleMode mode, BottomSheetDialog parentSheet) {
        if (bottomSheetInvitePermissions != null && bottomSheetInvitePermissions.isShowing())
            return;

        bottomSheetInvitePermissions = new BottomSheetDialog(this, R.style.DialogStyle);
        Objects.requireNonNull(bottomSheetInvitePermissions.getWindow())
                .setBackgroundDrawableResource(android.R.color.transparent);

        LayoutInflater inflater = LayoutInflater.from(this);
        View sheetView = inflater.inflate(R.layout.bottom_sheet_invite_permissions, null);
        bottomSheetInvitePermissions.setContentView(sheetView);
        BottomSheetBehavior
                .from(Objects.requireNonNull(bottomSheetInvitePermissions.findViewById(com.google.android.material.R.id.design_bottom_sheet)))
                .setState(BottomSheetBehavior.STATE_EXPANDED);

        androidx.appcompat.widget.AppCompatImageView ivAvatar = sheetView.findViewById(R.id.ivAvatar);
        TextView tvName = sheetView.findViewById(R.id.tvName);
        SwitchMaterial switchManageContent = sheetView.findViewById(R.id.switchManageContent);
        SwitchMaterial switchManageMessages = sheetView.findViewById(R.id.switchManageMessages);
        SwitchMaterial switchManageCommunity = sheetView.findViewById(R.id.switchManageCommunity);
        SwitchMaterial switchViewInsights = sheetView.findViewById(R.id.switchViewInsights);
        TextView btnSendInvite = sheetView.findViewById(R.id.btnSendInvite);

        com.bumptech.glide.Glide.with(this)
                .load(user.getProfilePictureUrl())
                .apply(UtilMethods.INSTANCE.getRequestOption_With_UserIcon())
                .into(ivAvatar);

        tvName.setText(user.getFullName() != null ? user.getFullName() : "—");

        btnSendInvite.setOnClickListener(v -> {
            boolean canManageContent = switchManageContent.isChecked();
            boolean canManageMessages = switchManageMessages.isChecked();
            boolean canManageCommunity = switchManageCommunity.isChecked();
            boolean canViewInsights = switchViewInsights.isChecked();

            if (!canManageContent && !canManageMessages && !canManageCommunity && !canViewInsights) {
                Toast.makeText(this, "Please enable at least one permission", Toast.LENGTH_SHORT).show();
                return;
            }

            sendInvite(user, canManageContent, canManageMessages, canManageCommunity, canViewInsights, parentSheet);
        });

        bottomSheetInvitePermissions.show();
    }

    private void sendInvite(SuggestedModeratorsResponse.SuggestedUser user,
                            boolean canManageContent, boolean canManageMessages,
                            boolean canManageCommunity, boolean canViewInsights,
                            BottomSheetDialog parentSheet) {

        UtilMethods.INSTANCE.inviteModerator(
                pageId,
                user.getUserId(),
                canManageContent,
                canManageMessages,
                canManageCommunity,
                canViewInsights,
                new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                        BasicResponse response = (BasicResponse) object;

                        runOnUiThread(() -> {
                            if (response.getStatusCode() == 1) {
                                Toast.makeText(PageAccessActivity.this,
                                        "Invitation sent to " + user.getFullName(),
                                        Toast.LENGTH_SHORT).show();

                                if (bottomSheetInvitePermissions != null)
                                    bottomSheetInvitePermissions.dismiss();
                                if (parentSheet != null) parentSheet.dismiss();

                                loadPageAccessList();
                                loadTaskAccessList();
                            } else {
                                String msg = response.getResponseText() != null
                                        ? response.getResponseText()
                                        : "Unable to send invitation. Please try again.";
                                Toast.makeText(PageAccessActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String msg) {
                        runOnUiThread(() ->
                                Toast.makeText(PageAccessActivity.this,
                                        "Failed to send invitation: " + msg,
                                        Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    // =========================================================================
    // Detail sheet, three-dot menu, remove
    // =========================================================================
    private void openDetailSheet(SuggestedModeratorsResponse.SuggestedUser moderator) {
        ModeratorDetailBottomSheet sheet = ModeratorDetailBottomSheet.newInstance(moderator, pageId);

        sheet.setOnModeratorChangedListener(new ModeratorDetailBottomSheet.OnModeratorChangedListener() {
            @Override
            public void onPermissionsUpdated(int moderatorId) {
                loadPageAccessList();
                loadTaskAccessList();
            }

            @Override
            public void onAccessRemoved(int moderatorId) {
                loadPageAccessList();
                loadTaskAccessList();
            }
        });

        sheet.show(getSupportFragmentManager(), "moderator_detail");
    }

    private void showModeratorMenu(SuggestedModeratorsResponse.SuggestedUser moderator, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Edit permissions");
        popup.getMenu().add(0, 2, 1, "Remove access");

        popup.setOnMenuItemClickListener(item -> switch (item.getItemId()) {
            case 1 -> {
                openDetailSheet(moderator);
                yield true;
            }
            case 2 -> {
                new AlertDialog.Builder(PageAccessActivity.this)
                        .setTitle("Remove access")
                        .setMessage("Remove " + moderator.getFullName() + " from page moderators?")
                        .setPositiveButton("Remove", (dialog, which) -> removeModerator(moderator))
                        .setNegativeButton("Cancel", null)
                        .show();
                yield true;
            }
            default -> false;
        });
        popup.show();
    }

    private void removeModerator(SuggestedModeratorsResponse.SuggestedUser moderator) {
        UtilMethods.INSTANCE.removeModerator(
                pageId,
                moderator.getModeratorId(),
                new UtilMethods.ApiCallBackMulti() {

                    @Override
                    public void onSuccess(Object object) {
                        runOnUiThread(() -> {
                            Toast.makeText(PageAccessActivity.this,
                                    moderator.getFullName() + " removed",
                                    Toast.LENGTH_SHORT).show();
                            loadPageAccessList();
                            loadTaskAccessList();
                        });
                    }

                    @Override
                    public void onError(String msg) {
                        runOnUiThread(() ->
                                Toast.makeText(PageAccessActivity.this,
                                        "Failed: " + msg,
                                        Toast.LENGTH_SHORT).show());
                    }
                });
    }

    // ── Info banner sheet ───────────────────────────────────────────────
    public void openPageAccessInfoBottomSheet() {
        if (bottomSheetPageAccessDialog != null && bottomSheetPageAccessDialog.isShowing()) return;

        bottomSheetPageAccessDialog = new BottomSheetDialog(this, R.style.DialogStyle);
        Objects.requireNonNull(bottomSheetPageAccessDialog.getWindow())
                .setBackgroundDrawableResource(android.R.color.transparent);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View sheetView = inflater.inflate(R.layout.bottom_sheet_page_access_detailes, null);
        bottomSheetPageAccessDialog.setContentView(sheetView);
        BottomSheetBehavior
                .from(Objects.requireNonNull(bottomSheetPageAccessDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetPageAccessDialog.show();
    }

    // =========================================================================
    // ADD PEOPLE — bottom sheet with search (used by both "+ Add" buttons)
    // =========================================================================
    private enum AddPeopleMode {PAGE_ACCESS, TASK_ACCESS}
}