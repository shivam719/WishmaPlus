package com.infotech.wishmaplus.Activity;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.infotech.wishmaplus.Adapter.MultiContentAdapter;
import com.infotech.wishmaplus.Api.Object.ContentResult;
import com.infotech.wishmaplus.Api.Object.StoryResult;
import com.infotech.wishmaplus.Api.Response.BasicObjectResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.ApiClient;
import com.infotech.wishmaplus.Utils.ApplicationConstant;
import com.infotech.wishmaplus.Utils.EndPointInterface;
import com.infotech.wishmaplus.Utils.PreferencesManager;
import com.infotech.wishmaplus.Utils.UtilMethods;
import com.infotech.wishmaplus.Utils.VideoEdit.AutoPlayVideo.CustomRecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareRedirectActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 10;
    private final List<ContentResult> contentList = new ArrayList<>();
    // ── Views ──────────────────────────────────────────────────────────────────
    private CustomRecyclerView recyclerView;
    private ProgressBar progress;
    // ── Core ───────────────────────────────────────────────────────────────────
    private PreferencesManager tokenManager;
    private MultiContentAdapter adapter;
    private LinearLayoutManager layoutManager;
    // ── Pagination ─────────────────────────────────────────────────────────────
    private String postId;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean scrollDone = false;
    // ── Network call reference (cancel on destroy) ─────────────────────────────
    @Nullable
    private Call<BasicObjectResponse<List<ContentResult>>> pendingCall;
    // ── Audio focus ────────────────────────────────────────────────────────────
    private AudioManager audioManager;
    @Nullable
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;

    // ==========================================================================
    //  onCreate
    // ==========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_share_redirect);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainS), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // ── Deep link validate ─────────────────────────────────────────────────
        Uri deepLink = getIntent().getData();
        Log.d("DEEPLINK", "Full URI: " + deepLink);
        Log.d("DEEPLINK", "Host: " + (deepLink != null ? deepLink.getHost() : "null"));
        Log.d("DEEPLINK", "Path: " + (deepLink != null ? deepLink.getPath() : "null"));
        Log.d("DEEPLINK", "Segments: " + (deepLink != null ? deepLink.getPathSegments() : "null"));
        Log.d("DEEPLINK", "Component: " + getIntent().getComponent()); // ← Yeh batayega kaun handle kar raha hai
        if (deepLink != null) {
            String host = deepLink.getHost();
            List<String> segments = deepLink.getPathSegments();
            // segments = ["post", "B03E4A28-2A84-4F3A-8191-8C54D18EDEFB"]
            if (ApplicationConstant.INSTANCE.Domain.equals(host)
                    && segments.size() >= 2
                    && "post".equals(segments.get(0))) {
                postId = segments.get(1); //  Always correct, no trailing slash issue
            } else {
                startActivity(new Intent(this, WelcomeActivity.class));
                finishAffinity();
                return;
            }
        } else {
            startActivity(new Intent(this, WelcomeActivity.class));
            finishAffinity();
            return;
        }

        // ── Auth check ─────────────────────────────────────────────────────────
        tokenManager = new PreferencesManager(this, 1);
        boolean isLoggedIn = !tokenManager.getString(tokenManager.LoginPref).isEmpty();

        setupUI();
        setupBackPress();

        if (!isLoggedIn) {
            Toast.makeText(this, "Please sign in to continue liking, commenting, and sharing content.", Toast.LENGTH_LONG).show();
        }

        loadPage(currentPage);
    }

    // ==========================================================================
    //  UI setup
    // ==========================================================================
    private void setupUI() {
        recyclerView = findViewById(R.id.recyclerView);
        progress = findViewById(R.id.progress);

        findViewById(R.id.back_button).setOnClickListener(v -> handleBack());

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MultiContentAdapter(null, tokenManager != null ? tokenManager.getUserId() : "", contentList, recyclerView, tokenManager, this, buildCallBack(), true,null, false);
        recyclerView.setAdapter(adapter);

        // Pagination scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!hasMore || isLoading || dy <= 0) return;
                int last = layoutManager.findLastVisibleItemPosition();
                if (last >= contentList.size() - 3) {
                    currentPage++;
                    loadPage(currentPage);
                }
            }
        });
    }

    // ==========================================================================
    //  Back press — modern OnBackPressedDispatcher
    // ==========================================================================
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void handleBack() {
        pauseMedia();
        finish();
    }

    // ==========================================================================
    //  Adapter callbacks
    // ==========================================================================
    private MultiContentAdapter.ClickCallBack buildCallBack() {
        return new MultiContentAdapter.ClickCallBack() {

            @Override
            public void onClickCreatePost(String pid) { /* read-only */ }

            @Override
            public void onClickCreateStory(String sid) { /* yahan nahi */ }

            @Override
            public void onClickProfile(String uid, ContentResult c) {
                if (uid == null || uid.isEmpty()) return;
                Intent i = new Intent(ShareRedirectActivity.this, ProfileActivity.class);
                i.putExtra("id", uid);
                if (c != null && c.getPageId() != null) i.putExtra("pageId", c.getPageId());
                startActivity(i);
            }

            @Override
            public void onOpenStory(ArrayList<StoryResult> list, int pos, StoryResult r) {
            }

            @Override
            public void onDelete(int pos) {
                if (pos < 0 || pos >= contentList.size()) return;
                contentList.remove(pos);
                adapter.notifyItemRemoved(pos);
                adapter.notifyItemRangeChanged(pos, contentList.size() - pos);
                if (contentList.isEmpty()) finish();
            }
        };
    }

    // ==========================================================================
    //  API — load page
    // ==========================================================================
    private void loadPage(int page) {
        if (isLoading) return;
        isLoading = true;
        progress.setVisibility(View.VISIBLE);

        String token = (tokenManager != null && !tokenManager.getAccessToken().isEmpty()) ? "Bearer " + tokenManager.getAccessToken() : "";

        EndPointInterface api = ApiClient.getClient().create(EndPointInterface.class);
        pendingCall = api.getPost(token, postId, page, PAGE_SIZE);
        pendingCall.enqueue(new Callback<BasicObjectResponse<List<ContentResult>>>() {
            @Override
            public void onResponse(@NonNull Call<BasicObjectResponse<List<ContentResult>>> call, @NonNull Response<BasicObjectResponse<List<ContentResult>>> response) {
                if (isDestroyed() || isFinishing()) return;
                isLoading = false;
                pendingCall = null;
                progress.setVisibility(View.GONE);
                BasicObjectResponse<List<ContentResult>> body = response.body();
                if (body == null || body.getStatusCode() != 1 || body.getResult() == null || body.getResult().isEmpty()) {
                    hasMore = false;
                    if (page == 1) showErrorAndExit("Post not available");
                    return;
                }

                List<ContentResult> newItems = body.getResult();
                if (newItems.size() < PAGE_SIZE) hasMore = false;

                int insertStart = contentList.size();
                contentList.addAll(newItems);
                adapter.notifyItemRangeInserted(insertStart, newItems.size());
                if (!scrollDone) {
                    for (int i = insertStart; i < contentList.size(); i++) {
                        ContentResult item = contentList.get(i);
                        if (item.getPostId() != null && item.getPostId().equalsIgnoreCase(postId)) {
                            scrollDone = true;
                            scrollAndHighlight(i);
                            return;
                        }
                    }
                    if (hasMore) {
                        currentPage++;
                        loadPage(currentPage);
                    } else {
                        showErrorAndExit("Unable to find the requested post.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BasicObjectResponse<List<ContentResult>>> call, @NonNull Throwable t) {

                if (isDestroyed() || isFinishing()) return;
                if (call.isCanceled()) return; // intentional cancel — ignore

                isLoading = false;
                pendingCall = null;
                progress.setVisibility(View.GONE);

                try {
                    UtilMethods.INSTANCE.apiFailureError(ShareRedirectActivity.this, t);
                } catch (Exception ignored) {
                    showErrorAndExit("Network error ");
                }
            }
        });
    }

    // ==========================================================================
    //  Scroll to target post + amber highlight flash
    // ==========================================================================
    private void scrollAndHighlight(final int index) {
        if (recyclerView == null) return;

        recyclerView.post(() -> {
            layoutManager.scrollToPositionWithOffset(index, 80);

            recyclerView.postDelayed(() -> {
                if (isDestroyed() || isFinishing()) return;
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(index);
                if (vh == null) return;
                View item = vh.itemView;
                item.setBackgroundColor(0x33FFB300); // amber flash
                item.postDelayed(() -> {
                    if (!isDestroyed()) item.setBackgroundColor(0x00000000);
                }, 1500);
            }, 350);
        });
    }

    // ==========================================================================
    //  Media control — CustomRecyclerView ke exact methods use kiye
    // ==========================================================================

    /**
     * Video pause + audio focus abandon
     */
    private void pauseMedia() {
        if (recyclerView != null) {
            try {
                recyclerView.pauseVideo(); // CustomRecyclerView ka exact method
            } catch (Exception ignored) {
            }
        }
        abandonAudioFocus();
    }

    /**
     * Video resume
     */
    private void resumeMedia() {
        if (recyclerView != null) {
            try {
                recyclerView.playVideo(); // CustomRecyclerView ka exact method
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * ExoPlayer + Handler runnables sab release
     */
    private void destroyMedia() {
        if (recyclerView != null) {
            try {
                recyclerView.destroyVideo(); // CustomRecyclerView ka exact method
            } catch (Exception ignored) {
            }
        }
        abandonAudioFocus();
    }

    /**
     * System ko audio focus wapas do — doosri app (music/call) resume ho sake
     */
    private void abandonAudioFocus() {
        if (audioManager != null && audioFocusListener != null) {
            audioManager.abandonAudioFocus(audioFocusListener);
            audioFocusListener = null;
        }
    }

    // ==========================================================================
    //  All resources release
    // ==========================================================================
    private void releaseAll() {
        // 1. Pending network call cancel
        if (pendingCall != null) {
            if (!pendingCall.isCanceled()) pendingCall.cancel();
            pendingCall = null;
        }
        destroyMedia();
        contentList.clear();
        recyclerView = null;
        adapter = null;
    }

    // ==========================================================================
    //  Lifecycle — proper order
    // ==========================================================================

    @Override
    protected void onResume() {
        super.onResume();
        resumeMedia();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMedia();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseMedia();
    }

    @Override
    protected void onDestroy() {
        releaseAll();
        super.onDestroy();
    }

    // ==========================================================================
    //  Helpers
    // ==========================================================================
    private void showErrorAndExit(String msg) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}