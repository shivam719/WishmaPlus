package com.infotech.wishmaplus.Activity;

import static android.view.View.GONE;
import static com.infotech.wishmaplus.Api.Request.TimeFilter.SEVEN_DAYS;
import static com.infotech.wishmaplus.Api.Request.TimeFilter.TODAY;
import static com.infotech.wishmaplus.Api.Request.TimeFilter.TWENTY_EIGHT_DAYS;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.infotech.wishmaplus.Api.Request.TimeFilter;
import com.infotech.wishmaplus.Api.Response.AnalyticsResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Result;
import com.infotech.wishmaplus.Summary;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.GetPageInsightsResponse;
import com.infotech.wishmaplus.Utils.PreferencesManager;
import com.infotech.wishmaplus.Utils.UtilMethods;
import com.infotech.wishmaplus.VideoInsightsChartView;
import com.infotech.wishmaplus.VideoInsightsResponse;
import com.infotech.wishmaplus.zego.DateWiseItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfessionalDashBoardPersonal extends AppCompatActivity {

    private static final String TAG = "ProfDashboard";

    // ── Existing views ────────────────────────────────────────────────────
    LinearLayout analyticsHead, contentHead;
    AppCompatImageView profile_image;
    AppCompatTextView profile_name;
    TextView tab28, tab7, tabToday;
    TextView tvLikeValue, tvCommentsValue, tvSharesValue, tvEngagementValue;
    TextView postCaption, likesValue, earningsValue, engagementValue, commentsValue;
    List<TextView> analyticsTabs;
    AnalyticsResponse analyticsResponse = new AnalyticsResponse();
    AppCompatTextView tvPercent;
    ProgressBar progressCircle;
    ImageView imgPost;
    VideoView videoPost;

    // ── Insights views ────────────────────────────────────────────────────
    TextView tvMinutesViewed, tvVideoViews, tvThreeSecViews;
    TextView tvInsightEngagement, tvNetFollowers, tvViewsToday, tvInsightDate;
    LinearLayout insightsLoadingState, insightsContent, insightsErrorState;
    TextView tvInsightsRetry;
    VideoInsightsChartView videoInsightsChart;
    TextView tvChartBigNumber, tvChartMetricName, tvChartGrowth;

    // ── Metric tabs ───────────────────────────────────────────────────────
    TextView tabMinutes, tabVideoViews, tab3SecViews, tabEngagement, tabNetFollowers;
    List<TextView> metricTabs;

    // ── Date filter chips ─────────────────────────────────────────────────
    TextView filterToday, filterYesterday, filter7, filter28, filter90;
    List<TextView> dateFilters;

    // ── SwipeRefresh ──────────────────────────────────────────────────────
    SwipeRefreshLayout swipeRefresh;
    boolean isProfileType;
    // Page Insights views
    TextView tvPageImpressions, tvPageImpressionsGrowth;
    TextView tvPageLikes, tvPageLikesGrowth;
    TextView tvNewPageLikes, tvNewPageLikesGrowth;
    TextView tvViralReach, tvViralReachGrowth;
    TextView tvAdImpressions, tvAdImpressionsGrowth;
    TextView tvAdClicks, tvAdClicksGrowth;
    TextView tvPageActions, tvPageActionsGrowth;
    TextView tvPageReach, tvPageReachGrowth;
    TextView tvPageInsightDate;
    LinearLayout pageInsightsLoadingState, pageInsightsContent, pageInsightsErrorState;
    TextView tvPageInsightsRetry;
    LinearLayout topPostsContainer;
    // Page Insights date filter chips
    TextView pageFilterToday, pageFilterYesterday, pageFilter7, pageFilter28, pageFilter90;
    List<TextView> pageDateFilters;
    CardView pageInsightsCard;
    // ── State ─────────────────────────────────────────────────────────────
    private CustomLoader loader;
    private PreferencesManager tokenManager;
    private String currentPostId = "";
    // Stored insight values
    private int insightMinutes, insight1MinViews, insight3SecViews, insightEngagement, insightNetFollowers;
    private List<DateWiseItem> insightDateWise;
    // Currently active date range — default: today/today
    private String insightStartDate;
    private String insightEndDate;
    private String activeDateFilterTag = "today"; // tag to track which chip is active
    // Page Insights date state
    private String pageInsightStartDate;
    private String pageInsightEndDate;
    private String activePageDateFilterTag = "today";
    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_professional_dash_board_personal);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        String postId = getIntent() != null ? getIntent().getStringExtra("page_id") : null;
        isProfileType = getIntent() != null && getIntent().getBooleanExtra("isProfileType", false);
        currentPostId = !TextUtils.isEmpty(postId) ? postId : "";

        bindViews();
        insightStartDate = getTodayDate();
        insightEndDate = getTodayDate();
        setupDateFilterChips();

        pageInsightStartDate = getTodayDate();
        pageInsightEndDate = getTodayDate();
        setupPageDateFilterChips();
        tvPageInsightDate.setText(formatBadgeDate(activePageDateFilterTag));
        tvPageInsightsRetry.setOnClickListener(v ->
                fetchPageInsights(currentPostId, pageInsightStartDate, pageInsightEndDate));

        setupMetricTabs();
        setupAnalyticsTabs();
        setupNavigation();
        setupSwipeRefresh();
        setupRefreshButton();

        // Initial date badge
        tvInsightDate.setText(formatBadgeDate(activeDateFilterTag));

        // Load data
        getProfessionalDahboardAnalytic(TWENTY_EIGHT_DAYS.getValue());
    }

    // =========================================================================
    // BIND VIEWS
    // =========================================================================

    private void bindViews() {
        // AppBar
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // Profile
        profile_image = findViewById(R.id.profile_image);
        profile_name = findViewById(R.id.profile_name);
        tvPercent = findViewById(R.id.tv_percent);
        progressCircle = findViewById(R.id.progress_circle);

        // Analytics
        tab28 = findViewById(R.id.tab28);
        tab7 = findViewById(R.id.tab7);
        tabToday = findViewById(R.id.tabToday);
        tvLikeValue = findViewById(R.id.tvLikeValue);
        tvCommentsValue = findViewById(R.id.tvCommentsValue);
        tvSharesValue = findViewById(R.id.tvSharesValue);
        tvEngagementValue = findViewById(R.id.tvEngagementValue);
        analyticsHead = findViewById(R.id.analyticsHead);
        pageInsightsCard = findViewById(R.id.pageInsightsCard);

        // Content
        contentHead = findViewById(R.id.contentHead);
        imgPost = findViewById(R.id.imagePost);
        videoPost = findViewById(R.id.videoPost);
        postCaption = findViewById(R.id.postCaption);
        likesValue = findViewById(R.id.likesValue);
        earningsValue = findViewById(R.id.earningsValue);
        engagementValue = findViewById(R.id.engagementValue);
        commentsValue = findViewById(R.id.commentsValue);

        // Insights
        tvMinutesViewed = findViewById(R.id.tvMinutesViewed);
        tvVideoViews = findViewById(R.id.tvVideoViews);
        tvThreeSecViews = findViewById(R.id.tvThreeSecViews);
        tvInsightEngagement = findViewById(R.id.tvInsightEngagement);
        tvNetFollowers = findViewById(R.id.tvNetFollowers);
        tvViewsToday = findViewById(R.id.tvViewsToday);
        tvInsightDate = findViewById(R.id.tvInsightDate);
        insightsLoadingState = findViewById(R.id.insightsLoadingState);
        insightsContent = findViewById(R.id.insightsContent);
        insightsErrorState = findViewById(R.id.insightsErrorState);
        tvInsightsRetry = findViewById(R.id.tvInsightsRetry);
        videoInsightsChart = findViewById(R.id.videoInsightsChart);
        tvChartBigNumber = findViewById(R.id.tvChartBigNumber);
        tvChartMetricName = findViewById(R.id.tvChartMetricName);
        tvChartGrowth = findViewById(R.id.tvChartGrowth);

        // Metric tabs
        tabMinutes = findViewById(R.id.tabMinutes);
        tabVideoViews = findViewById(R.id.tabVideoViews);
        tab3SecViews = findViewById(R.id.tab3SecViews);
        tabEngagement = findViewById(R.id.tabEngagement);
        tabNetFollowers = findViewById(R.id.tabNetFollowers);
        metricTabs = Arrays.asList(tabMinutes, tabVideoViews, tab3SecViews, tabEngagement, tabNetFollowers);

        // Date filter chips
        filterToday = findViewById(R.id.filterToday);
        filterYesterday = findViewById(R.id.filterYesterday);
        filter7 = findViewById(R.id.filter7);
        filter28 = findViewById(R.id.filter28);
        filter90 = findViewById(R.id.filter90);
        dateFilters = Arrays.asList(filterToday, filterYesterday, filter7, filter28, filter90);


        /* / ──2. Page Insights bindViews () (append at end) ────────────────────────────────*/

        tvPageImpressions = findViewById(R.id.tvPageImpressions);
        tvPageImpressionsGrowth = findViewById(R.id.tvPageImpressionsGrowth);
        tvPageLikes = findViewById(R.id.tvPageLikes);
        tvPageLikesGrowth = findViewById(R.id.tvPageLikesGrowth);
        tvNewPageLikes = findViewById(R.id.tvNewPageLikes);
        tvNewPageLikesGrowth = findViewById(R.id.tvNewPageLikesGrowth);
        tvViralReach = findViewById(R.id.tvViralReach);
        tvViralReachGrowth = findViewById(R.id.tvViralReachGrowth);
        tvAdImpressions = findViewById(R.id.tvAdImpressions);
        tvAdImpressionsGrowth = findViewById(R.id.tvAdImpressionsGrowth);
        tvAdClicks = findViewById(R.id.tvAdClicks);
        tvAdClicksGrowth = findViewById(R.id.tvAdClicksGrowth);
        tvPageActions = findViewById(R.id.tvPageActions);
        tvPageActionsGrowth = findViewById(R.id.tvPageActionsGrowth);
        tvPageReach = findViewById(R.id.tvPageReach);
        tvPageReachGrowth = findViewById(R.id.tvPageReachGrowth);
        tvPageInsightDate = findViewById(R.id.tvPageInsightDate);
        pageInsightsLoadingState = findViewById(R.id.pageInsightsLoadingState);
        pageInsightsContent = findViewById(R.id.pageInsightsContent);
        pageInsightsErrorState = findViewById(R.id.pageInsightsErrorState);
        tvPageInsightsRetry = findViewById(R.id.tvPageInsightsRetry);
        topPostsContainer = findViewById(R.id.topPostsContainer);

        pageFilterToday = findViewById(R.id.pageFilterToday);
        pageFilterYesterday = findViewById(R.id.pageFilterYesterday);
        pageFilter7 = findViewById(R.id.pageFilter7);
        pageFilter28 = findViewById(R.id.pageFilter28);
        pageFilter90 = findViewById(R.id.pageFilter90);
        pageDateFilters = Arrays.asList(pageFilterToday, pageFilterYesterday, pageFilter7, pageFilter28, pageFilter90);

        if (isProfileType) pageInsightsCard.setVisibility(GONE);
        else pageInsightsCard.setVisibility(View.VISIBLE);

    }

    // =========================================================================
    // DATE FILTER CHIPS
    // =========================================================================

    private void setupDateFilterChips() {
        filterToday.setOnClickListener(v -> {
            activeDateFilterTag = "today";
            insightStartDate = getTodayDate();
            insightEndDate = getTodayDate();
            onDateFilterChanged();
        });
        filterYesterday.setOnClickListener(v -> {
            activeDateFilterTag = "yesterday";
            insightStartDate = getDateBefore(1);
            insightEndDate = getDateBefore(1);
            onDateFilterChanged();
        });
        filter7.setOnClickListener(v -> {
            activeDateFilterTag = "7";
            insightStartDate = getDateBefore(6);
            insightEndDate = getTodayDate();
            onDateFilterChanged();
        });
        filter28.setOnClickListener(v -> {
            activeDateFilterTag = "28";
            insightStartDate = getDateBefore(27);
            insightEndDate = getTodayDate();
            onDateFilterChanged();
        });
        filter90.setOnClickListener(v -> {
            activeDateFilterTag = "90";
            insightStartDate = getDateBefore(89);
            insightEndDate = getTodayDate();
            onDateFilterChanged();
        });

        // Set default chip selected
        selectDateFilter(filterToday);
    }

    private void onDateFilterChanged() {
        // Update UI chip highlight
        switch (activeDateFilterTag) {
            case "today":
                selectDateFilter(filterToday);
                break;
            case "yesterday":
                selectDateFilter(filterYesterday);
                break;
            case "7":
                selectDateFilter(filter7);
                break;
            case "28":
                selectDateFilter(filter28);
                break;
            case "90":
                selectDateFilter(filter90);
                break;
        }
        tvInsightDate.setText(formatBadgeDate(activeDateFilterTag));
        // Re-fetch insights with new range
        fetchVideoInsights(currentPostId, insightStartDate, insightEndDate);
    }

    private void selectDateFilter(TextView selected) {
        for (TextView t : dateFilters) {
            t.setBackgroundResource(R.drawable.bg_tab_unselected);
            t.setTextColor(Color.parseColor("#65676B"));
            t.setTypeface(null, Typeface.NORMAL);
        }
        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(Color.parseColor("#1877F2"));
        selected.setTypeface(null, Typeface.BOLD);
    }

    private String formatBadgeDate(String tag) {
        SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        switch (tag) {
            case "today":
                return "Today · " + out.format(new Date());
            case "yesterday": {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, -1);
                return "Yesterday · " + out.format(c.getTime());
            }
            case "7":
                return "Last 7 days";
            case "28":
                return "Last 28 days";
            case "90":
                return "Last 90 days";
            default:
                return out.format(new Date());
        }
    }

    // =========================================================================
    // METRIC TABS
    // =========================================================================

    private void setupMetricTabs() {
        tabMinutes.setOnClickListener(v -> {
            selectMetricTab(tabMinutes);
            renderChartForMetric("minutes");
        });
        tabVideoViews.setOnClickListener(v -> {
            selectMetricTab(tabVideoViews);
            renderChartForMetric("video_views");
        });
        tab3SecViews.setOnClickListener(v -> {
            selectMetricTab(tab3SecViews);
            renderChartForMetric("three_sec");
        });
        tabEngagement.setOnClickListener(v -> {
            selectMetricTab(tabEngagement);
            renderChartForMetric("engagement");
        });
        tabNetFollowers.setOnClickListener(v -> {
            selectMetricTab(tabNetFollowers);
            renderChartForMetric("followers");
        });
    }

    private void selectMetricTab(TextView selected) {
        for (TextView t : metricTabs) {
            t.setBackgroundResource(R.drawable.bg_tab_unselected);
            t.setTextColor(Color.parseColor("#65676B"));
            t.setTypeface(null, Typeface.NORMAL);
        }
        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(Color.parseColor("#1877F2"));
        selected.setTypeface(null, Typeface.BOLD);
    }

    // =========================================================================
    // ANALYTICS TABS  (28 / 7 / Today)
    // =========================================================================

    private void setupAnalyticsTabs() {
        loader = new CustomLoader(this, android.R.style.Theme_Translucent_NoTitleBar);
        tokenManager = new PreferencesManager(this, 1);
        analyticsTabs = Arrays.asList(tab28, tab7, tabToday);
        selectAnalyticsTab(tab28);

        tab28.setOnClickListener(v -> {
            selectAnalyticsTab(tab28);
            onAnalyticsTabSelected(TWENTY_EIGHT_DAYS);
        });
        tab7.setOnClickListener(v -> {
            selectAnalyticsTab(tab7);
            onAnalyticsTabSelected(SEVEN_DAYS);
        });
        tabToday.setOnClickListener(v -> {
            selectAnalyticsTab(tabToday);
            onAnalyticsTabSelected(TODAY);
        });
    }

    private void selectAnalyticsTab(TextView selected) {
        for (TextView t : analyticsTabs) {
            t.setBackgroundResource(R.drawable.bg_tab_unselected);
            t.setTextColor(Color.parseColor("#65676B"));
        }
        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(Color.parseColor("#1877F2"));
    }

    private void onAnalyticsTabSelected(TimeFilter filter) {
        getProfessionalDahboardAnalytic(filter.getValue());
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    private void setupNavigation() {
        AppCompatImageButton backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        tvInsightsRetry.setOnClickListener(v -> fetchVideoInsights(currentPostId, insightStartDate, insightEndDate));

        findViewById(R.id.analytics_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsDashboard.class);
            intent.putExtra("page_id",
                    !TextUtils.isEmpty(currentPostId)
                            ? currentPostId
                            : "");
            startActivity(intent);
        });
        findViewById(R.id.content_tab).setOnClickListener(v -> startActivity(new Intent(this, AnalyticsContent.class)));
        findViewById(R.id.creator_support_card).setOnClickListener(v -> startActivity(new Intent(this, ComplaintList.class)));
        analyticsHead.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsDashboard.class);
            intent.putExtra("page_id",
                    !TextUtils.isEmpty(currentPostId)
                            ? currentPostId
                            : "");
            startActivity(intent);
        });
        contentHead.setOnClickListener(v -> startActivity(new Intent(this, AnalyticsContent.class)));
    }

    // =========================================================================
    // SWIPE REFRESH
    // =========================================================================

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(Color.parseColor("#1877F2"), Color.parseColor("#1D9E75"), Color.parseColor("#F59E0B"));
        swipeRefresh.setOnRefreshListener(this::refreshAll);
    }

    private void setupRefreshButton() {
        AppCompatImageButton btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            // Animate the icon
            btnRefresh.animate().rotation(btnRefresh.getRotation() + 360f).setDuration(500).start();
            refreshAll();
        });
    }

    private void refreshAll() {
        // Re-fetch analytics (28 day default — preserves current selected tab if needed)
        getProfessionalDahboardAnalytic(TWENTY_EIGHT_DAYS.getValue());
        // Re-fetch insights with currently selected date range
        if (isProfileType) {
            fetchVideoInsights(currentPostId, insightStartDate, insightEndDate);
        } else {
            fetchVideoInsights(currentPostId, insightStartDate, insightEndDate);
            fetchPageInsights(currentPostId, pageInsightStartDate, pageInsightEndDate);
        }
    }

    // =========================================================================
    // VIDEO INSIGHTS API
    // =========================================================================

    private void fetchVideoInsights(String postId, String startDate, String endDate) {
        insightsLoadingState.setVisibility(View.VISIBLE);
        insightsContent.setVisibility(GONE);
        insightsErrorState.setVisibility(GONE);

        UtilMethods.INSTANCE.getVideoInsights(postId, startDate, endDate, new UtilMethods.ApiCallBackMulti() {

            @Override
            public void onSuccess(Object object) {
                VideoInsightsResponse response = (VideoInsightsResponse) object;

                if (response.getStatusCode() != 1) {
                    showInsightsError(response.getResponseText() != null ? response.getResponseText() : "No data available.");
                    return;
                }

                Result result = response.getResult();
                Summary summary = result.getSummary();

                insightMinutes = summary.getMinutesViewed();
                insight1MinViews = summary.getOneMinuteViews();
                insight3SecViews = summary.getThreeSecondViews();
                insightEngagement = summary.getEngagement();
                insightNetFollowers = summary.getNetFollowers();
                insightDateWise = result.getDateWise();

                int viewsTodayTemp = 0;
                if (insightDateWise != null && !insightDateWise.isEmpty()) {
                    viewsTodayTemp = insightDateWise.get(0).getViews();
                }
                final int viewsToday = viewsTodayTemp;

                runOnUiThread(() -> {
                    tvMinutesViewed.setText(formatNumber(insightMinutes));
                    tvVideoViews.setText(formatNumber(insight1MinViews));
                    tvThreeSecViews.setText(formatNumber(insight3SecViews));
                    tvInsightEngagement.setText(formatNumber(insightEngagement));
                    tvNetFollowers.setText(formatNumber(insightNetFollowers));
                    tvViewsToday.setText(formatNumber(viewsToday));

                    // Default: show Minutes Viewed chart
                    selectMetricTab(tabMinutes);
                    renderChartForMetric("minutes");

                    insightsLoadingState.setVisibility(GONE);
                    insightsErrorState.setVisibility(GONE);
                    insightsContent.setVisibility(View.VISIBLE);

                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String msg) {
                Log.e(TAG, "VideoInsights error: " + msg);
                showInsightsError("Could not load insights. Tap Retry.");
                runOnUiThread(() -> {
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void showInsightsError(String message) {
        runOnUiThread(() -> {
            insightsLoadingState.setVisibility(GONE);
            insightsContent.setVisibility(GONE);
            insightsErrorState.setVisibility(View.VISIBLE);
            TextView tvErr = findViewById(R.id.tvInsightsError);
            if (tvErr != null) tvErr.setText(message);
            if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
        });
    }

    // =========================================================================
    // CHART RENDERING
    // =========================================================================

    private void renderChartForMetric(String metric) {
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();

        int summaryTotal;
        String metricLabel;
        String chartColor;

        switch (metric) {
            case "video_views":
                summaryTotal = insight1MinViews;
                metricLabel = "1-Min views";
                chartColor = "#0EA5E9";
                break;
            case "three_sec":
                summaryTotal = insight3SecViews;
                metricLabel = "3-Sec views";
                chartColor = "#8B5CF6";
                break;
            case "engagement":
                summaryTotal = insightEngagement;
                metricLabel = "Engagement";
                chartColor = "#F59E0B";
                break;
            case "followers":
                summaryTotal = insightNetFollowers;
                metricLabel = "Net followers";
                chartColor = "#10B981";
                break;
            default: // "minutes"
                summaryTotal = insightMinutes;
                metricLabel = "Minutes viewed";
                chartColor = "#1877F2";
                break;
        }

        if (insightDateWise != null && !insightDateWise.isEmpty()) {
            for (DateWiseItem dw : insightDateWise) {
                String raw = dw.getInsightDate() != null ? dw.getInsightDate() : "";
                // Trim to "10 Jun" style — first word is the day number
                String lbl = raw.contains(" ") ? raw.substring(0, raw.indexOf(' ')) : raw;
                labels.add(lbl);
                values.add((float) dw.getViews());
            }
        } else {
            labels.add("Today");
            values.add((float) summaryTotal);
        }

        tvChartBigNumber.setText(formatNumber(summaryTotal));
        tvChartMetricName.setText(metricLabel);
        tvChartGrowth.setVisibility(GONE);

        videoInsightsChart.setData(labels, values, chartColor, metricLabel);
    }

    // =========================================================================
    // ANALYTICS DASHBOARD API
    // =========================================================================

    @SuppressLint("SetTextI18n")
    public void getProfessionalDahboardAnalytic(int dateRange) {
        if (loader != null) loader.show();

        UtilMethods.INSTANCE.getProfessionalDahboardAnalytic(dateRange, isProfileType ? "" : currentPostId, new UtilMethods.ApiCallBackMulti() {

            @Override
            public void onSuccess(Object object) {
                if (loader != null && loader.isShowing()) loader.dismiss();

                if (!(object instanceof AnalyticsResponse)) {
                    Log.e(TAG, "Analytics: unexpected response type");
                    return;
                }

                AnalyticsResponse response = (AnalyticsResponse) object;

                if (response.getStatusCode() != 1) {
                    Log.e(TAG, "Analytics: statusCode != 1 — " + response.getResponseText());
                    return;
                }

                if (response.getResult() == null) {
                    Log.e(TAG, "Analytics: result is null");
                    return;
                }

                analyticsResponse = response;

                runOnUiThread(() -> {
                    bindProfile(response.getResult().getProfile());
                    bindAnalytics(response.getResult().getAnalytic());
                    bindLatestPost(response.getResult().getLatestPosts());

                    if (isProfileType) {
                        fetchVideoInsights(currentPostId, insightStartDate, insightEndDate);
                    } else {
                        fetchVideoInsights(currentPostId, insightStartDate, insightEndDate);
                        fetchPageInsights(currentPostId, pageInsightStartDate, pageInsightEndDate);
                    }

                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String msg) {
                if (loader != null && loader.isShowing()) loader.dismiss();
                Log.e(TAG, "Analytics error: " + msg);
                runOnUiThread(() -> {
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    // ── Profile section ──────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    private void bindProfile(Object profileObj) {
        if (profileObj == null) {
            profile_name.setText("—");
            tvPercent.setText("0%");
            progressCircle.setProgress(0);
            Glide.with(this)
                    .load(R.drawable.user_icon)
                    .apply(UtilMethods.INSTANCE.getRequestOption_With_UserIcon())
                    .into(profile_image);
            return;
        }

        // cast back to actual profile type used in original code
        AnalyticsResponse.Profile profile = (AnalyticsResponse.Profile) profileObj;

        String name = profile.getFullName();
        profile_name.setText(!TextUtils.isEmpty(name) ? name : "—");

        String picUrl = profile.getProfilePictureUrl();
        Glide.with(this)
                .load(!TextUtils.isEmpty(picUrl) ? picUrl : null)
                .apply(UtilMethods.INSTANCE.getRequestOption_With_UserIcon())
                .placeholder(R.drawable.user_icon)
                .into(profile_image);

        int progress = profile.getWeelkyProgress();
        ObjectAnimator anim = ObjectAnimator.ofInt(progressCircle, "progress", progress);
        anim.setDuration(700);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
        tvPercent.setText(progress + "%");
    }

    // ── Analytics counts section ─────────────────────────────────────
    private void bindAnalytics(Object analyticObj) {
        if (analyticObj == null) {
            tvLikeValue.setText("0");
            tvCommentsValue.setText("0");
            tvSharesValue.setText("0");
            tvEngagementValue.setText("0");
            return;
        }

        AnalyticsResponse.Analytic analytic = (AnalyticsResponse.Analytic) analyticObj;

        tvLikeValue.setText(String.valueOf(analytic.getTotalLikes()));
        tvCommentsValue.setText(String.valueOf(analytic.getTotalComments()));
        tvSharesValue.setText(String.valueOf(analytic.getTotalShares()));
        tvEngagementValue.setText(String.valueOf(analytic.getTotalEngagement()));
    }

    // ── Latest post section ──────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    private void bindLatestPost(Object postObj) {
        imgPost.setVisibility(GONE);
        videoPost.setVisibility(GONE);

        if (postObj == null) {
            postCaption.setText("");
            likesValue.setText("0");
            earningsValue.setText("₹0");
            engagementValue.setText("0");
            commentsValue.setText("0");
            return;
        }

        AnalyticsResponse.LatestPost post = (AnalyticsResponse.LatestPost) postObj;

        int contentType = post.getContentTypeId();
        String postContent = post.getPostContent();
        String caption = post.getCaption();

        if (!TextUtils.isEmpty(caption)) postCaption.setText(caption);

        if (contentType == 2 && !TextUtils.isEmpty(postContent)) {
            videoPost.setVisibility(View.VISIBLE);
            videoPost.setVideoPath(postContent);
            videoPost.seekTo(1);
        } else if (!TextUtils.isEmpty(postContent)) {
            imgPost.setVisibility(View.VISIBLE);
            Glide.with(this).load(postContent).placeholder(R.drawable.app_logo).into(imgPost);
        } else {
            // no content at all — show placeholder image instead of leaving both hidden
            imgPost.setVisibility(View.VISIBLE);
            imgPost.setImageResource(R.drawable.app_logo);
        }

        likesValue.setText(String.valueOf(post.getTotalLikes()));
        earningsValue.setText("₹" + post.getPostEarning());
        engagementValue.setText(String.valueOf(post.getEngagement()));
        commentsValue.setText(String.valueOf(post.getTotalComments()));
    }

    // PAGE DATE FILTER CHIPS ─────────────────────────────────────────────────
    private void setupPageDateFilterChips() {
        pageFilterToday.setOnClickListener(v -> {
            activePageDateFilterTag = "today";
            pageInsightStartDate = getTodayDate();
            pageInsightEndDate = getTodayDate();
            onPageDateFilterChanged();
        });
        pageFilterYesterday.setOnClickListener(v -> {
            activePageDateFilterTag = "yesterday";
            pageInsightStartDate = getDateBefore(1);
            pageInsightEndDate = getDateBefore(1);
            onPageDateFilterChanged();
        });
        pageFilter7.setOnClickListener(v -> {
            activePageDateFilterTag = "7";
            pageInsightStartDate = getDateBefore(6);
            pageInsightEndDate = getTodayDate();
            onPageDateFilterChanged();
        });
        pageFilter28.setOnClickListener(v -> {
            activePageDateFilterTag = "28";
            pageInsightStartDate = getDateBefore(27);
            pageInsightEndDate = getTodayDate();
            onPageDateFilterChanged();
        });
        pageFilter90.setOnClickListener(v -> {
            activePageDateFilterTag = "90";
            pageInsightStartDate = getDateBefore(89);
            pageInsightEndDate = getTodayDate();
            onPageDateFilterChanged();
        });
        selectPageDateFilter(pageFilterToday);
    }

    private void onPageDateFilterChanged() {
        switch (activePageDateFilterTag) {
            case "today":
                selectPageDateFilter(pageFilterToday);
                break;
            case "yesterday":
                selectPageDateFilter(pageFilterYesterday);
                break;
            case "7":
                selectPageDateFilter(pageFilter7);
                break;
            case "28":
                selectPageDateFilter(pageFilter28);
                break;
            case "90":
                selectPageDateFilter(pageFilter90);
                break;
        }
        tvPageInsightDate.setText(formatBadgeDate(activePageDateFilterTag));
        fetchPageInsights(currentPostId, pageInsightStartDate, pageInsightEndDate);
    }

    private void fetchPageInsights(String postId, String startDate, String endDate) {
        pageInsightsLoadingState.setVisibility(View.VISIBLE);
        pageInsightsContent.setVisibility(GONE);
        pageInsightsErrorState.setVisibility(GONE);

        UtilMethods.INSTANCE.getPageInsights(postId, startDate, endDate, new UtilMethods.ApiCallBackMulti() {

            @Override
            public void onSuccess(Object object) {
                GetPageInsightsResponse response = (GetPageInsightsResponse) object;

                if (response.getStatusCode() != 1) {
                    showPageInsightsError(
                            response.getResponseText() != null ? response.getResponseText() : "No data available.");
                    return;
                }

                GetPageInsightsResponse.Summary s = response.getResult().getSummary();
                List<GetPageInsightsResponse.TopPost> posts = response.getResult().getTopPosts();

                runOnUiThread(() -> {
                    // Summary stats + growth badges
                    tvPageImpressions.setText(formatNumber(s.getImpressions()));
                    setGrowthText(tvPageImpressionsGrowth, s.getImpressionsGrowth());

                    tvPageLikes.setText(formatNumber(s.getPageLikes()));
                    setGrowthText(tvPageLikesGrowth, s.getPageLikesGrowth());

                    tvNewPageLikes.setText(formatNumber(s.getNewPageLikes()));
                    setGrowthText(tvNewPageLikesGrowth, s.getNewPageLikesGrowth());

                    tvViralReach.setText(formatNumber(s.getViralReach()));
                    setGrowthText(tvViralReachGrowth, s.getViralReachGrowth());

                    tvAdImpressions.setText(formatNumber(s.getImpressions())); // reuse impressions for ad row label
                    setGrowthText(tvAdImpressionsGrowth, s.getImpressionsGrowth());

                    tvAdClicks.setText(formatNumber(s.getAdClicks()));
                    setGrowthText(tvAdClicksGrowth, s.getAdClicksGrowth());

                    tvPageActions.setText(formatNumber(s.getActions()));
                    setGrowthText(tvPageActionsGrowth, s.getActionsGrowth());

                    tvPageReach.setText(formatNumber(s.getReach()));
                    setGrowthText(tvPageReachGrowth, s.getReachGrowth());

                    // Render top posts
                    renderTopPosts(posts);

                    pageInsightsLoadingState.setVisibility(GONE);
                    pageInsightsErrorState.setVisibility(GONE);
                    pageInsightsContent.setVisibility(View.VISIBLE);

                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String msg) {
                Log.e(TAG, "PageInsights error: " + msg);
                showPageInsightsError("Could not load page insights. Tap Retry.");
                runOnUiThread(() -> {
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void showPageInsightsError(String message) {
        runOnUiThread(() -> {
            pageInsightsLoadingState.setVisibility(GONE);
            pageInsightsContent.setVisibility(GONE);
            pageInsightsErrorState.setVisibility(View.VISIBLE);
            TextView tvErr = findViewById(R.id.tvPageInsightsError);
            if (tvErr != null) tvErr.setText(message);
            if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
        });
    }

    @SuppressLint("SetTextI18n")
    private void setGrowthText(TextView tv, double growth) {
        if (growth > 0) {
            tv.setText("+" + String.format(Locale.ENGLISH, "%.1f", growth) + "%");
            tv.setTextColor(Color.parseColor("#1D9E75")); // green
        } else if (growth < 0) {
            tv.setText(String.format(Locale.ENGLISH, "%.1f", growth) + "%");
            tv.setTextColor(Color.parseColor("#E02020")); // red
        } else {
            tv.setText("0%");
            tv.setTextColor(Color.parseColor("#90949C")); // neutral grey
        }
    }

    @SuppressLint("SetTextI18n")
    private void renderTopPosts(List<GetPageInsightsResponse.TopPost> posts) {
        topPostsContainer.removeAllViews();
        if (posts == null || posts.isEmpty()) return;

        for (GetPageInsightsResponse.TopPost post : posts) {
            // Inflate a row programmatically (avoids needing a separate item layout file)
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int hPad = (int) (16 * getResources().getDisplayMetrics().density);
            int vPad = (int) (10 * getResources().getDisplayMetrics().density);
            row.setPadding(hPad, vPad, hPad, vPad);

            // Thumbnail
            androidx.appcompat.widget.AppCompatImageView thumb =
                    new androidx.appcompat.widget.AppCompatImageView(this);
            int thumbSize = (int) (44 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(thumbSize, thumbSize);
            thumbParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(thumbParams);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundResource(com.payu.ui.R.drawable.rectangle);
            thumb.setClipToOutline(true);
            Glide.with(this)
                    .load(post.getThumbnailUrl())
                    .placeholder(R.drawable.app_logo)
                    .into(thumb);

            // Caption + date column
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f);
            textCol.setLayoutParams(textParams);

            TextView tvCaption = new TextView(this);
            String captionText = (post.getCaption() != null && !post.getCaption().isEmpty())
                    ? post.getCaption() : "—";
            tvCaption.setText(captionText);
            tvCaption.setTextColor(Color.parseColor("#1C1E21"));
            tvCaption.setTextSize(12f);
            tvCaption.setMaxLines(1);
            tvCaption.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView tvDate = new TextView(this);
            tvDate.setText(post.getCreatedAt() != null ? post.getCreatedAt() : "");
            tvDate.setTextColor(Color.parseColor("#90949C"));
            tvDate.setTextSize(10f);

            textCol.addView(tvCaption);
            textCol.addView(tvDate);

            // Reach / Likes / Shares stat cells
            TextView tvReach = makeStatCell(formatNumber(post.getReach()));
            TextView tvLikes = makeStatCell(formatNumber(post.getLikes()));
            TextView tvShares = makeStatCell(formatNumber(post.getShares()));

            row.addView(thumb);
            row.addView(textCol);
            row.addView(tvReach);
            row.addView(tvLikes);
            row.addView(tvShares);

            topPostsContainer.addView(row);

            // Divider
            View divider = new View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(Color.parseColor("#E4E6EA"));
            topPostsContainer.addView(divider);
        }
    }

    private TextView makeStatCell(String text) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(p);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#1C1E21"));
        tv.setTextSize(12f);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

    private void selectPageDateFilter(TextView selected) {
        for (TextView t : pageDateFilters) {
            t.setBackgroundResource(R.drawable.bg_tab_unselected);
            t.setTextColor(Color.parseColor("#65676B"));
            t.setTypeface(null, Typeface.NORMAL);
        }
        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(Color.parseColor("#1877F2"));
        selected.setTypeface(null, Typeface.BOLD);
    }
    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * "10 June 2026" — format expected by API
     */
    private String getTodayDate() {
        return new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(new Date());
    }

    /**
     * N days before today, same format
     */
    private String getDateBefore(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(cal.getTime());
    }

    /**
     * Format large numbers: 1500 → "1.5k", 1000000 → "1M"
     */
    private String formatNumber(int n) {
        if (n >= 1_000_000) return String.format(Locale.ENGLISH, "%.1fM", n / 1_000_000f);
        if (n >= 1_000) return String.format(Locale.ENGLISH, "%.1fk", n / 1_000f);
        return String.valueOf(n);
    }
}