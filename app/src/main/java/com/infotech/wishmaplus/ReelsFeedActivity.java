package com.infotech.wishmaplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.infotech.wishmaplus.Utils.ApiClient;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.EndPointInterface;
import com.infotech.wishmaplus.Utils.PreferencesManager;
import com.infotech.wishmaplus.Utils.UtilMethods;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReelsFeedActivity extends AppCompatActivity {

    private RecyclerView reelsRecycler;
    private LinearLayoutManager layoutManager;
    private ReelsFeedAdapter adapter;
    private final List<ReelModel> reelList = new ArrayList<>();
    private Spinner spinnerSort;
    private CustomLoader loader;
    private int pageNumber = 1;
    private final int pageSize = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentSort = "Latest";
    private String reelId = "0";
    String finalPageId = null;
    public boolean isMyReel = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reels_feed);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if (getIntent() != null) {
            isMyReel = getIntent().getBooleanExtra("isMyReel", false);
            finalPageId = getIntent().getStringExtra("pageId");
        }
        extractReelIdFromDeepLink();
        loader = new CustomLoader(this,
                android.R.style.Theme_Translucent_NoTitleBar);
        spinnerSort = findViewById(R.id.spinnerSort);
        reelsRecycler = findViewById(R.id.reelsRecycler);
        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Layout

        layoutManager = new LinearLayoutManager(this);
        reelsRecycler.setLayoutManager(layoutManager);
        reelsRecycler.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        // Pager snap — one reel at a time
        PagerSnapHelper snap = new PagerSnapHelper();
        snap.attachToRecyclerView(reelsRecycler);

        // Adapter
        adapter = new ReelsFeedAdapter(this, reelList, loader);
        reelsRecycler.setAdapter(adapter);

        // ── Scroll listener ─────────────────────────────────────────────
        reelsRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv,
                                             int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int visible = layoutManager
                            .findFirstCompletelyVisibleItemPosition();
                    if (visible < 0)
                        visible = layoutManager
                                .findFirstVisibleItemPosition();
                    if (visible >= 0
                            && visible != adapter.currentPlayingPosition) {
                        adapter.playPosition(visible);
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView rv,
                                   int dx, int dy) {
                // Pagination
                if (!isLoading && !isLastPage
                        && layoutManager.findLastVisibleItemPosition()
                        >= layoutManager.getItemCount() - 3) {
                    loadReelsFromApi(currentSort);
                }
            }
        });
        if (isMyReel) {
            spinnerSort.setVisibility(View.GONE);
        }
        setupSpinner();
        loadReelsFromApi("Latest");
    }

    private void extractReelIdFromDeepLink() {
        try {
            Intent intent = getIntent();
            if (intent == null) return;
            String extraId = intent.getStringExtra("reelId");
            if (extraId != null && extraId.matches("\\d+")) {
                reelId = extraId;
                return;
            }
            Uri data = intent.getData();
            if (data != null) {
                List<String> segments = data.getPathSegments();
                if (segments != null && segments.size() > 1) {
                    String idFromUrl = segments.get(1);
                    if (idFromUrl != null && idFromUrl.matches("\\d+")) {
                        reelId = idFromUrl;
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            reelId = "0";
        }
    }

    private void setupSpinner() {
        String[] opts = {"Latest", "Popular", "Trending"};
        spinnerSort.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opts));
        spinnerSort.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> p, View v,
                                               int pos, long id) {
                        String selected = opts[pos];
                        if (!selected.equals(currentSort)) {
                            currentSort = selected;
                            pageNumber = 1;
                            isLastPage = false;
                            reelList.clear();
                            adapter.notifyDataSetChanged();
                            loadReelsFromApi(currentSort);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p) {
                    }
                });
    }

    private void loadReelsFromApi(String sort) {
        if (isLoading || isLastPage) return;
        isLoading = true;

        if (isMyReel) {
            // ★ Load only this user's reels
            EndPointInterface api = ApiClient.getClient()
                    .create(EndPointInterface.class);

            PreferencesManager tokenManager = new PreferencesManager(this, 1);

            Call<GetReelResponse> call = api.getMyReels(
                    "Bearer " + tokenManager.getAccessToken(),
                    pageNumber,
                    pageSize,
                    finalPageId
            );

            call.enqueue(new Callback<GetReelResponse>() {
                @Override
                public void onResponse(@NonNull Call<GetReelResponse> call,
                                       @NonNull Response<GetReelResponse> response) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        if (response.body() == null
                                || response.body().result == null) return;

                        GetReelResponse data = response.body();
                        int prevSize = reelList.size();
                        reelList.addAll(data.result);
                        adapter.notifyItemRangeInserted(prevSize, data.result.size());

                        if (data.result.size() < pageSize) isLastPage = true;

                        // ★ Scroll to tapped reel on first load
                        if (pageNumber == 1) {
                            reelsRecycler.post(() -> {
                                int targetPos = 0;
                                if (!reelId.equals("0")) {
                                    int tapId = Integer.parseInt(reelId);
                                    for (int i = 0; i < reelList.size(); i++) {
                                        if (reelList.get(i).getReelId() == tapId) {
                                            targetPos = i;
                                            break;
                                        }
                                    }
                                }
                                reelsRecycler.scrollToPosition(targetPos);
                                adapter.playPosition(targetPos);
                            });
                        }
                        pageNumber++;
                    });
                }

                @Override
                public void onFailure(@NonNull Call<GetReelResponse> call,
                                      @NonNull Throwable t) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        Toast.makeText(ReelsFeedActivity.this,
                                t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } else {
            // ★ Original general feed — unchanged
            UtilMethods.INSTANCE.getReels(loader, pageNumber, pageSize, sort,
                    Integer.parseInt(reelId), finalPageId,
                    new UtilMethods.ApiCallBackMulti() {
                        @Override
                        public void onSuccess(Object response) {
                            GetReelResponse data = (GetReelResponse) response;
                            runOnUiThread(() -> {
                                isLoading = false;
                                if (data.result == null) return;

                                int prevSize = reelList.size();
                                reelList.addAll(data.result);
                                adapter.notifyItemRangeInserted(
                                        prevSize, data.result.size());

                                if (data.result.size() < pageSize) isLastPage = true;

                                if (pageNumber == 1) {
                                    reelsRecycler.post(() -> adapter.playPosition(0));
                                }
                                pageNumber++;
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                isLoading = false;
                                Toast.makeText(ReelsFeedActivity.this,
                                        error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        adapter.pauseAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.resumeCurrent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.releaseAll();
    }
}