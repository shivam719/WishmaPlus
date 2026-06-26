package com.infotech.wishmaplus.Activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.infotech.wishmaplus.Adapter.AudienceAdapter;
import com.infotech.wishmaplus.Adapter.GoalAdapter;
import com.infotech.wishmaplus.Api.Object.PgKeyVals;
import com.infotech.wishmaplus.Api.Request.InitiateBoostRequest;
import com.infotech.wishmaplus.Api.Response.BoostResponse;
import com.infotech.wishmaplus.Api.Response.EstimateResponse;
import com.infotech.wishmaplus.Api.Response.GetContentDetailsToBoostResponse;
import com.infotech.wishmaplus.Api.Response.UpgradePackageResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.ApiClient;
import com.infotech.wishmaplus.Utils.CheckoutProWebChromeClient;
import com.infotech.wishmaplus.Utils.CheckoutProWebViewClient;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.EndPointInterface;
import com.infotech.wishmaplus.Utils.PreferencesManager;
import com.infotech.wishmaplus.Utils.UtilMethods;
import com.payu.base.models.CardType;
import com.payu.base.models.ErrorResponse;
import com.payu.base.models.PayUPaymentParams;
import com.payu.base.models.PaymentType;
import com.payu.checkoutpro.PayUCheckoutPro;
import com.payu.checkoutpro.models.PayUCheckoutProConfig;
import com.payu.checkoutpro.utils.PayUCheckoutProConstants;
import com.payu.custombrowser.Bank;
import com.payu.ui.model.listeners.PayUCheckoutProListener;
import com.payu.ui.model.listeners.PayUHashGenerationListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNewAd extends AppCompatActivity {

    BottomSheetDialog bottomGoalDialogReport, bottomSpecialCatDialogReport,
            bottomChooseAudienceCatDialogReport, bottomIsSecureCatDialogReport,
            bottomPlacementsCatDialogReport, bottomBudgetCatDialogReport,
            bottomPaymentCatDialogReport;

    androidx.appcompat.widget.AppCompatImageView profile, containerImage;
    androidx.appcompat.widget.AppCompatTextView nameTv, timeTv, postTxt;
    View containerVideo, rbContinuous, rbChoose, goalTypeLayout;
    VideoView videoView;

    private CustomLoader loader;

    GetContentDetailsToBoostResponse getContentDetailsToBoostResponse =
            new GetContentDetailsToBoostResponse();
    BoostResponse boostResponse = new BoostResponse();
    EstimateResponse estimateResponse = new EstimateResponse();

    String postId = "";
    int boostStatus;

    RecyclerView rvGoals, rvAudience;
    GoalAdapter adapter;
    AudienceAdapter audienceAdapter;

    TextView linkClicks, postEngagements, peopleReached, textView, tvPeopleReached,
            tvBudgetPrice, tvCostPrice, tvSubPrice, tvGstPrice, tvPrice,
            tvDials, userNameTitle, tvAdd;

    LinearLayout layoutCall, layoutUrl, tvBudgetValue;
    SeekBar seekBar;

    RadioButton rbChooseAd, rbRun;

    int audienceId = 1;
    private long mLastClickTime;
    private PreferencesManager tokenManager;

    private FrameLayout goalOverlay, postOverlay, placMentOverlay, audienceOverlay;
    private TextView tvDays, tvDate, tvInfo, tvLine1, textView3, textView2,
            tvSummarySubtitle, tvLinkClicks;
    private ImageButton btnPlus, btnMinus;
    private LinearLayout layoutDate, daysPicker, llInfo, editTextLayout, callNow, bookNow;

    private int days = 1;
    private Calendar startDate;
    private Calendar endDate;

    EditText etBudget, etPhone, etUrl;

    int minAge = 18;
    int maxAge = 65;
    String gender = "All";
    String xmlType = "";

    double budgetGlobal = 0.0;
    double estimatedCost = 0.0;
    double gstAmount = 0.0;
    double subTotal = 0.0;
    double total = 0.0;

    // FIX: minBudget/maxBudget start at 0; setupBudgetUI() is only called after API response
    double minBudget = 0.0;
    double maxBudget = 0.0;

    Button btnPromoteNow;
    int boostId;

    // -----------------------------------------------------------------------
    // onCreate
    // -----------------------------------------------------------------------
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_new_ad);

        postId = getIntent().getStringExtra("postId");
        boostStatus = getIntent().getIntExtra("boostStatus", 0);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tokenManager = new PreferencesManager(this, 1);

        // ---------- View bindings ----------
        goalOverlay      = findViewById(R.id.goalOverlay);
        placMentOverlay  = findViewById(R.id.placMentOverlay);
        audienceOverlay  = findViewById(R.id.audienceOverlay);

        if (boostStatus == 2) {
            goalOverlay.setVisibility(View.VISIBLE);
            placMentOverlay.setVisibility(View.VISIBLE);
            audienceOverlay.setVisibility(View.VISIBLE);
        } else {
            goalOverlay.setVisibility(View.GONE);
            placMentOverlay.setVisibility(View.GONE);
            audienceOverlay.setVisibility(View.GONE);
        }

        rvGoals         = findViewById(R.id.rvGoals);
        rvAudience      = findViewById(R.id.rvAudience);
        seekBar         = findViewById(R.id.budgetSeekBar);
        textView        = findViewById(R.id.tvBudgetText);
        tvPeopleReached = findViewById(R.id.tvPeopleReached);
        tvBudgetPrice   = findViewById(R.id.tvBudgetPrice);
        tvCostPrice     = findViewById(R.id.tvCostPrice);
        tvSubPrice      = findViewById(R.id.tvSubPrice);
        tvGstPrice      = findViewById(R.id.tvGstPrice);
        tvPrice         = findViewById(R.id.tvPrice);
        rbRun           = findViewById(R.id.rbRun);
        rbChooseAd      = findViewById(R.id.rbChooseAd);
        daysPicker      = findViewById(R.id.daysPicker);
        llInfo          = findViewById(R.id.llInfo);
        tvLine1         = findViewById(R.id.tvLine1);
        rbChoose        = findViewById(R.id.rbChoose);
        rbContinuous    = findViewById(R.id.rbContinuous);
        etBudget        = findViewById(R.id.etBudget);
        tvBudgetValue   = findViewById(R.id.tvBudgetValue);
        textView3       = findViewById(R.id.textView3);
        textView2       = findViewById(R.id.textView2);
        editTextLayout  = findViewById(R.id.editTextLayout);
        tvSummarySubtitle = findViewById(R.id.tvSummarySubtitle);
        tvLinkClicks    = findViewById(R.id.tvLinkClicks);
        goalTypeLayout  = findViewById(R.id.goalTypeLayout);
        tvDials         = findViewById(R.id.tvDials);
        userNameTitle   = findViewById(R.id.userNameTitle);
        callNow         = findViewById(R.id.callNow);
        bookNow         = findViewById(R.id.bookNow);
        tvAdd           = findViewById(R.id.tvAdd);
        etPhone         = findViewById(R.id.etPhone);
        etUrl           = findViewById(R.id.etUrl);
        btnPromoteNow   = findViewById(R.id.btnPromoteNow);
        tvDays          = findViewById(R.id.tvDays);
        tvDate          = findViewById(R.id.tvDate);
        tvInfo          = findViewById(R.id.tvInfo);
        btnPlus         = findViewById(R.id.btnPlus);
        btnMinus        = findViewById(R.id.btnMinus);
        layoutDate      = findViewById(R.id.layoutDate);
        profile         = findViewById(R.id.profile);
        nameTv          = findViewById(R.id.nameTv);
        timeTv          = findViewById(R.id.timeTv);
        postTxt         = findViewById(R.id.postTxt);
        containerVideo  = findViewById(R.id.containerVideo);
        videoView       = findViewById(R.id.videoView);
        containerImage  = findViewById(R.id.containerImage);
        linkClicks      = findViewById(R.id.linkClicks);
        postEngagements = findViewById(R.id.postEngagements);
        peopleReached   = findViewById(R.id.peopleReached);
        layoutCall      = findViewById(R.id.layoutCall);
        layoutUrl       = findViewById(R.id.layoutUrl);

        btnPromoteNow.setText(boostStatus == 2 ? "Extend Boost Budget" : "Promote Now");
        goalTypeLayout.setVisibility(GONE);
        editTextLayout.setVisibility(GONE);

        // ---------- Spinner ----------
        Spinner spinner = findViewById(R.id.spCountry);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"India (+91)"});
        spinner.setAdapter(spinnerAdapter);

        // ---------- Listeners ----------
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.tvGiveFeedback).setOnClickListener(v -> {
            startActivity(new Intent(this, ComplaintList.class));
        });
        findViewById(R.id.imgInfoGoal).setOnClickListener(v -> openGoalBottomSheetDialog(this));
        findViewById(R.id.imgInfo).setOnClickListener(v -> openSpecialCatBottomSheetDialog(this));
        findViewById(R.id.infoAudience).setOnClickListener(v -> openChooseAudienceBottomSheetDialog(this));
        findViewById(R.id.ivInfo).setOnClickListener(v -> openPlacementsBottomSheetDialog(this));
        findViewById(R.id.infoBudget).setOnClickListener(v -> openBudgetBottomSheetDialog(this));
        findViewById(R.id.ivInfoPayment).setOnClickListener(v -> openPaymentBottomSheetDialog(this));

        btnPromoteNow.setOnClickListener(view ->
                getInitiatePostBoost(null, null, null, null));

        // ---------- RecyclerView ----------
        rvGoals.setLayoutManager(new LinearLayoutManager(this));
        rvGoals.setHasFixedSize(true);
        rvAudience.setLayoutManager(new LinearLayoutManager(this));
        rvAudience.setHasFixedSize(true);

        loader = new CustomLoader(this, android.R.style.Theme_Translucent_NoTitleBar);

        // ---------- Duration / Date ----------
        startDate = Calendar.getInstance();
        endDate   = Calendar.getInstance();

        rbChooseAd.setChecked(true);
        rbRun.setChecked(false);
        daysPicker.setVisibility(VISIBLE);
        llInfo.setVisibility(GONE);
        tvSummarySubtitle.setVisibility(GONE);

        // FIX: Only update labels here; DO NOT call getEstimateBoostReach yet
        //      because minBudget is still 0.  The real call happens inside
        //      getContentDetailsToBoostResponse -> onSuccess after setupBudgetUI().
        updateDurationLabelsOnly();

        btnPlus.setOnClickListener(v -> {
            days++;
            updateDurationLabelsOnly();
            // FIX: guard — only call estimate when minBudget is known
            if (minBudget > 0) {
                getEstimateBoostReach(minBudget + seekBar.getProgress(), days, audienceId);
            }
        });

        btnMinus.setOnClickListener(v -> {
            if (days > 1) {
                days--;
                updateDurationLabelsOnly();
                // FIX: guard
                if (minBudget > 0) {
                    getEstimateBoostReach(minBudget + seekBar.getProgress(), days, audienceId);
                }
            }
        });

        layoutDate.setOnClickListener(v -> openDatePicker());

        rbChoose.setOnClickListener(view -> {
            daysPicker.setVisibility(VISIBLE);
            llInfo.setVisibility(GONE);
            tvSummarySubtitle.setVisibility(GONE);
            days = 1;
            rbChooseAd.setChecked(true);
            rbRun.setChecked(false);
            updateDurationLabelsOnly();
            // FIX: guard
            if (minBudget > 0) {
                getEstimateBoostReach(minBudget + seekBar.getProgress(), days, audienceId);
            }
        });

        rbContinuous.setOnClickListener(view -> {
            llInfo.setVisibility(VISIBLE);
            tvSummarySubtitle.setVisibility(VISIBLE);
            daysPicker.setVisibility(GONE);
            days = -1;
            rbRun.setChecked(true);
            rbChooseAd.setChecked(false);
            // FIX: pass minBudget + progress (not raw progress), and guard
            if (minBudget > 0) {
                getEstimateBoostReach(minBudget + seekBar.getProgress(), days, audienceId);
            }
        });

        // ---------- SeekBar ----------
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double actualBudget = minBudget + progress;
                @SuppressLint("DefaultLocale")
                String formatted = String.format("%,.2f", actualBudget);
                textView.setText("₹" + formatted);
                etBudget.setText(String.valueOf((int) actualBudget));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // FIX: use actual budget (minBudget + progress), guard
                if (minBudget > 0) {
                    double actualBudget = minBudget + seekBar.getProgress();
                    getEstimateBoostReach(actualBudget, days, audienceId);
                }
            }
        });

        // ---------- Budget edit-text toggle ----------
        tvBudgetValue.setOnClickListener(view -> {
            etBudget.setText(String.valueOf((int)(minBudget + seekBar.getProgress())));
            tvBudgetValue.setVisibility(GONE);
            textView3.setVisibility(GONE);
            seekBar.setVisibility(GONE);
            textView2.setVisibility(GONE);
            editTextLayout.setVisibility(VISIBLE);
        });

        etBudget.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String value = etBudget.getText().toString().trim();
                if (value.isEmpty()) {
                    etBudget.setError("Required");
                    return true;
                }
                int budget = Integer.parseInt(value);
                if (budget >= minBudget && budget <= maxBudget) {
                    int progress = (int) (budget - minBudget);
                    seekBar.setProgress(progress);
                    @SuppressLint("DefaultLocale")
                    String formatted = String.format("%,.2f", (double) budget);
                    textView.setText("₹" + formatted);
                }
                tvBudgetValue.setVisibility(VISIBLE);
                textView3.setVisibility(VISIBLE);
                seekBar.setVisibility(VISIBLE);
                textView2.setVisibility(VISIBLE);
                editTextLayout.setVisibility(GONE);

                // FIX: guard
                if (minBudget > 0) {
                    getEstimateBoostReach(minBudget + seekBar.getProgress(), days, audienceId);
                }

                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etBudget.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // ---------- Fetch content (async — setupBudgetUI called inside onSuccess) ----------
        getContentDetailsToBoostResponse(postId);
    }

    // -----------------------------------------------------------------------
    // FIX: Split original updateFromDays() into two methods:
    //   1. updateDurationLabelsOnly()  — safe to call any time (no estimate call)
    //   2. updateFromDays()            — calls estimate (only after minBudget known)
    // -----------------------------------------------------------------------

    /** Only updates day count and end-date labels. Does NOT call estimate API. */
    private void updateDurationLabelsOnly() {
        tvDays.setText(String.valueOf(days));
        endDate = (Calendar) startDate.clone();
        endDate.add(Calendar.DAY_OF_YEAR, Math.max(days, 1));

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(endDate.getTime()));
        tvInfo.setText("Your ad will be published today and run for " + days + " day"
                + (days > 1 ? "s" : "") + " ending on " + sdf.format(endDate.getTime()) + ".");
    }

    /** Updates labels AND fires estimate. Only called when minBudget is known (> 0). */
    private void updateFromDays() {
        updateDurationLabelsOnly();
        // Safe to call here because this method is only invoked after content API response
        getEstimateBoostReach(minBudget + seekBar.getProgress(), days, audienceId);
    }

    private void updateFromDate() {
        long diff = endDate.getTimeInMillis() - startDate.getTimeInMillis();
        days = (int) TimeUnit.MILLISECONDS.toDays(diff);
        if (days < 1) days = 1;
        tvDays.setText(String.valueOf(days));
        // FIX: guard
        if (minBudget > 0) {
            updateFromDays();
        } else {
            updateDurationLabelsOnly();
        }
    }

    // -----------------------------------------------------------------------
    // setupBudgetUI  — only called AFTER minBudget / maxBudget are set from API
    // -----------------------------------------------------------------------
    @SuppressLint("SetTextI18n")
    private void setupBudgetUI() {
        seekBar.setMax((int) (maxBudget - minBudget));
        seekBar.setProgress(0); // default = minBudget

        @SuppressLint("DefaultLocale") String minFormatted = String.format("%,.2f", minBudget);
        @SuppressLint("DefaultLocale") String maxFormatted = String.format("%,.2f", maxBudget);

        textView.setText("₹" + minFormatted);
        textView3.setText("₹" + minFormatted);
        textView2.setText("₹" + maxFormatted);
        etBudget.setText(String.valueOf((int) minBudget));
    }

    // -----------------------------------------------------------------------
    // Activity result launcher for EditAudience
    // -----------------------------------------------------------------------
    ActivityResultLauncher<Intent> editAudienceLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int minAge  = data.getIntExtra("minAge", 18);
                    int maxAge  = data.getIntExtra("maxAge", 65);
                    String gender = data.getStringExtra("gender");
                    audienceAdapter.updateAudience(minAge, maxAge, gender);
                }
            });

    // -----------------------------------------------------------------------
    // Date picker
    // -----------------------------------------------------------------------
    private void openDatePicker() {
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.DAY_OF_YEAR, 1);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            endDate.set(year, month, dayOfMonth);
            if (!endDate.after(startDate)) {
                endDate = (Calendar) startDate.clone();
                endDate.add(Calendar.DAY_OF_YEAR, 1);
            }
            updateFromDate();
        }, endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH),
                endDate.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        dialog.show();
    }

    // -----------------------------------------------------------------------
    // API — GetContentDetailsToBoost
    // -----------------------------------------------------------------------
    public void getContentDetailsToBoostResponse(String postId) {
        loader.show();
        UtilMethods.INSTANCE.getContentDetailsToBoost(postId, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                if (loader != null && loader.isShowing()) loader.dismiss();

                getContentDetailsToBoostResponse = (GetContentDetailsToBoostResponse) object;

                if (getContentDetailsToBoostResponse.getStatusCode() == 1) {

                    if (getContentDetailsToBoostResponse.getAudience() != null
                            && !getContentDetailsToBoostResponse.getAudience().isEmpty()) {
                        audienceId = getContentDetailsToBoostResponse
                                .getAudience().get(0).getAudienceId();
                    }

                    GetContentDetailsToBoostResponse.PostInsights postInsights =
                            getContentDetailsToBoostResponse.getPostInsights();

                    Glide.with(CreateNewAd.this)
                            .load(postInsights.getProfilePictureUrl())
                            .placeholder(R.drawable.user_icon)
                            .into(profile);

                    nameTv.setText(postInsights.getUserName());
                    userNameTitle.setText(postInsights.getUserName());
                    boostId = postInsights.getBoostId();
                    timeTv.setText("Sponsored");

                    // FIX: Set minBudget/maxBudget from API FIRST
                    minBudget = postInsights.getMinBudget();
                    maxBudget = postInsights.getMaxBudget();

                    // FIX: Now it is safe to call setupBudgetUI()
                    setupBudgetUI();

                    // FIX: Now it is safe to call estimate with the real minBudget
                    getEstimateBoostReach(minBudget, days, audienceId);

                    if (postInsights.getCaption() != null) {
                        postTxt.setText(postInsights.getCaption());
                    }

                    if (postInsights.getContentTypeId() == 1) {
                        containerVideo.setVisibility(GONE);
                        containerImage.setVisibility(GONE);
                    } else if (postInsights.getContentTypeId() == 2) {
                        containerVideo.setVisibility(VISIBLE);
                        containerImage.setVisibility(GONE);
                        videoView.setVideoPath(postInsights.getPostContent());
                    } else if (postInsights.getContentTypeId() == 3) {
                        containerVideo.setVisibility(GONE);
                        containerImage.setVisibility(VISIBLE);
                        Glide.with(CreateNewAd.this)
                                .load(postInsights.getPostContent())
                                .placeholder(R.drawable.app_logo)
                                .into(containerImage);
                    }

                    peopleReached.setText(
                            "" + getContentDetailsToBoostResponse.getPostInsights().getPeopleReach());
                    postEngagements.setText(
                            "" + getContentDetailsToBoostResponse.getPostInsights().getEngagement());

                    // Goals adapter
                    adapter = new GoalAdapter(CreateNewAd.this,
                            getContentDetailsToBoostResponse.getGoal(),
                            (position, goal) -> {
                                String icon = goal.getIconName().toLowerCase();
                                if (icon.equals("visitors")) {
                                    layoutCall.setVisibility(GONE);
                                    layoutUrl.setVisibility(VISIBLE);
                                    goalTypeLayout.setVisibility(VISIBLE);
                                    tvDials.setText("VISIT");
                                    callNow.setVisibility(GONE);
                                    bookNow.setVisibility(VISIBLE);
                                    xmlType = "url";
                                } else if (icon.equals("calls")) {
                                    layoutCall.setVisibility(VISIBLE);
                                    layoutUrl.setVisibility(GONE);
                                    goalTypeLayout.setVisibility(VISIBLE);
                                    tvDials.setText("DIALS");
                                    callNow.setVisibility(VISIBLE);
                                    bookNow.setVisibility(GONE);
                                    xmlType = "call";
                                } else {
                                    layoutCall.setVisibility(GONE);
                                    layoutUrl.setVisibility(GONE);
                                    goalTypeLayout.setVisibility(GONE);
                                    xmlType = "";
                                }
                            });
                    rvGoals.setAdapter(adapter);

                    // Audience adapter
                    audienceAdapter = new AudienceAdapter(
                            CreateNewAd.this,
                            getContentDetailsToBoostResponse.getAudience(),
                            new AudienceAdapter.OnAudienceClickListener() {
                                @Override
                                public void onAudienceClick(int position,
                                                            GetContentDetailsToBoostResponse.Audience goal) {
                                    audienceId = goal.getAudienceId();
                                    // FIX: use actual budget, guard
                                    if (minBudget > 0) {
                                        getEstimateBoostReach(
                                                minBudget + seekBar.getProgress(),
                                                days, audienceId);
                                    }
                                }

                                @Override
                                public void onAudienceEditClick(int position,
                                                                GetContentDetailsToBoostResponse.Audience goal,
                                                                int minAge, int maxAge, String gender) {
                                    Intent intent = new Intent(
                                            CreateNewAd.this, EditAudience.class);
                                    intent.putExtra("minAge", minAge);
                                    intent.putExtra("maxAge", maxAge);
                                    intent.putExtra("gender", gender);
                                    intent.putExtra("audience", goal.getAudienceName());
                                    editAudienceLauncher.launch(intent);
                                }
                            }, minAge, maxAge, gender);
                    rvAudience.setAdapter(audienceAdapter);
                }
            }

            @Override
            public void onError(String msg) {
                if (loader != null && loader.isShowing()) loader.dismiss();
            }
        });
    }

    // -----------------------------------------------------------------------
    // API — GetEstimateBoostReach
    // -----------------------------------------------------------------------
    public void getEstimateBoostReach(double budget, int days, int audienceId) {
        loader.show();
        UtilMethods.INSTANCE.getEstimateBoostReach(budget, days, audienceId,
                new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                        if (loader != null && loader.isShowing()) loader.dismiss();

                        estimateResponse = (EstimateResponse) object;

                        // FIX: Check BOTH outer statusCode AND inner result statusCode
                        if (estimateResponse.getStatusCode() == 1
                                && estimateResponse.getResult() != null
                                && estimateResponse.getResult().getStatusCode() == 1) {

                            tvLine1.setText("Your ad will run continuously with a daily budget of ₹"
                                    + (int)(minBudget + seekBar.getProgress())
                                    + ". Actual amount spent daily may vary.");

                            tvPeopleReached.setText(estimateResponse.getResult().getReach());
                            tvLinkClicks.setText(estimateResponse.getResult().getLinkClick());
                            tvAdd.setText("₹" + estimateResponse.getResult().getUserBalance());
                            tvBudgetPrice.setText("₹" + estimateResponse.getResult().getBudget());
                            tvCostPrice.setText("₹" + estimateResponse.getResult().getEstimatedCost());
                            tvSubPrice.setText("₹" + estimateResponse.getResult().getSubTotal());
                            tvGstPrice.setText("₹" + estimateResponse.getResult().getGst());
                            tvPrice.setText("₹" + estimateResponse.getResult().getTotal());

                            budgetGlobal  = estimateResponse.getResult().getBudget();
                            estimatedCost = estimateResponse.getResult().getEstimatedCost();
                            gstAmount     = estimateResponse.getResult().getGst();
                            subTotal      = estimateResponse.getResult().getSubTotal();
                            total         = estimateResponse.getResult().getTotal();

                        }
                        // FIX: If inner statusCode == -1 ("Budget out of allowed range"),
                        //      do nothing — UI stays as-is, no corrupt data shown.
                    }

                    @Override
                    public void onError(String msg) {
                        if (loader != null && loader.isShowing()) loader.dismiss();
                    }
                });
    }

    // -----------------------------------------------------------------------
    // Helper — format date for API
    // -----------------------------------------------------------------------
    public static String formatDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat =
                    new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH);
            SimpleDateFormat outputFormat =
                    new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    // -----------------------------------------------------------------------
    // API — InitiatePostBoost
    // -----------------------------------------------------------------------
    public void getInitiatePostBoost(String tid, String hashData, String hashName,
                                     PayUHashGenerationListener hashGenerationListener) {

        String phoneNo = (!etPhone.getText().toString().isEmpty())
                ? "+91" + etPhone.getText().toString()
                : "";
        String url    = etUrl.getText().toString();
        String endDateStr = formatDate(tvDate.getText().toString());

        int genderType = 0;
        if (gender.equals("Male"))   genderType = 1;
        else if (gender.equals("Female")) genderType = 1;

        loader.show();

        InitiateBoostRequest request = new InitiateBoostRequest(
                tid, hashData, boostId, postId, url, phoneNo, xmlType,
                budgetGlobal, estimatedCost, subTotal, gstAmount, total,
                days, endDateStr, audienceId, minAge, maxAge, genderType, "Wishma Plus");

        UtilMethods.INSTANCE.initiateBoostPost(request, boostStatus,
                new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                        if (loader != null && loader.isShowing()) loader.dismiss();

                        boostResponse = (BoostResponse) object;

                        if (boostResponse == null) return;

                        if (boostResponse.getStatusCode() == 1) {
                            boostId = boostResponse.getBoostId();

                            if (boostResponse.isPgActive() && boostResponse.getData() != null) {
                                if (boostResponse.getData().getStatusCode() == 1
                                        && boostResponse.getData().getPgResponse() != null) {
                                    if (boostResponse.getData().getPgResponse().getKeyVals() != null) {
                                        if (hashData == null || hashData.isEmpty()) {
                                            startPayUPayment(
                                                    boostResponse.getData().getPgResponse().getKeyVals());
                                        } else {
                                            String hash = boostResponse.getData()
                                                    .getPgResponse().getKeyVals().getHash();
                                            if (hash != null && !hash.isEmpty()) {
                                                HashMap<String, String> dataMap = new HashMap<>();
                                                dataMap.put(hashName, hash);
                                                hashGenerationListener.onHashGenerated(dataMap);
                                            } else {
                                                UtilMethods.INSTANCE.Error(
                                                        CreateNewAd.this, "Problem in Hash generation");
                                            }
                                        }
                                    } else {
                                        UtilMethods.INSTANCE.Error(
                                                CreateNewAd.this, "Transaction data is not available");
                                    }
                                } else {
                                    UtilMethods.INSTANCE.Error(
                                            CreateNewAd.this,
                                            boostResponse.getData().getResponseText());
                                }
                            } else {
                                Toast.makeText(CreateNewAd.this,
                                        boostResponse.getResponseText(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            UtilMethods.INSTANCE.Error(
                                    CreateNewAd.this, boostResponse.getResponseText());
                        }
                    }

                    @Override
                    public void onError(String msg) {
                        if (loader != null && loader.isShowing()) loader.dismiss();
                    }
                });
    }

    // -----------------------------------------------------------------------
    // PayU helpers
    // -----------------------------------------------------------------------
    private void startPayUPayment(PgKeyVals keyVals) {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return;
        mLastClickTime = SystemClock.elapsedRealtime();
        if (validateSDKParams(keyVals)) {
            initUiSdk(preparePayUBizParams(keyVals), keyVals);
        }
    }

    private boolean validateSDKParams(PgKeyVals mKeyVals) {
        if (TextUtils.isEmpty(mKeyVals.getKey())) {
            UtilMethods.INSTANCE.Error(this, "Invalid or empty Key"); return false;
        }
        if (TextUtils.isEmpty(mKeyVals.getHash())) {
            UtilMethods.INSTANCE.Error(this, "Invalid or empty Hash"); return false;
        }
        if (TextUtils.isEmpty(mKeyVals.getTxnid())) {
            UtilMethods.INSTANCE.Error(this, "Invalid or empty Transaction Id"); return false;
        }
        if (TextUtils.isEmpty(mKeyVals.getEmail())) {
            UtilMethods.INSTANCE.Error(this, "Invalid or empty Mail Id"); return false;
        }
        if (TextUtils.isEmpty(mKeyVals.getFirstname())) {
            UtilMethods.INSTANCE.Error(this, "Invalid or empty Name"); return false;
        }
        if (TextUtils.isEmpty(mKeyVals.getSurl())) {
            UtilMethods.INSTANCE.Error(this, "Invalid or empty Success URL"); return false;
        }
        if (TextUtils.isEmpty(mKeyVals.getFurl())) {
            UtilMethods.INSTANCE.Error(this, "Invalid or empty Fail URL"); return false;
        }
        return true;
    }

    private PayUPaymentParams preparePayUBizParams(PgKeyVals mKeyVals) {
        HashMap<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(PayUCheckoutProConstants.CP_UDF1, "udf1");
        additionalParams.put(PayUCheckoutProConstants.CP_UDF2, "udf2");
        additionalParams.put(PayUCheckoutProConstants.CP_UDF3, "udf3");
        additionalParams.put(PayUCheckoutProConstants.CP_UDF4, "udf4");
        additionalParams.put(PayUCheckoutProConstants.CP_UDF5, "udf5");

        return new PayUPaymentParams.Builder()
                .setAmount(mKeyVals.getAmount())
                .setIsProduction(mKeyVals.isProdcution())
                .setProductInfo(mKeyVals.getProductinfo())
                .setKey(mKeyVals.getKey())
                .setPhone(mKeyVals.getPhone())
                .setTransactionId(mKeyVals.getTxnid())
                .setFirstName(mKeyVals.getFirstname())
                .setEmail(mKeyVals.getEmail())
                .setSurl(mKeyVals.getSurl())
                .setFurl(mKeyVals.getFurl())
                .setAdditionalParams(additionalParams)
                .setUserCredential(mKeyVals.getKey() + mKeyVals.getEmail())
                .setPayUSIParams(null)
                .build();
    }

    private void initUiSdk(PayUPaymentParams payUPaymentParams, PgKeyVals mKeyVals) {
        PayUCheckoutPro.open(this, payUPaymentParams, getCheckoutProConfig(mKeyVals),
                new PayUCheckoutProListener() {

                    @Override
                    public void onPaymentSuccess(@NotNull Object response) {
                        payuStatusUpdate(mKeyVals.getTxnid());
                    }

                    @Override
                    public void onPaymentFailure(Object response) {
                        payuStatusUpdate(mKeyVals.getTxnid());
                    }

                    @Override
                    public void onPaymentCancel(boolean isTxnInitiated) {
                        showSnackBar(getResources().getString(R.string.transaction_cancelled_by_user));
                    }

                    @Override
                    public void onError(ErrorResponse errorResponse) {
                        String errorMessage = errorResponse.getErrorMessage();
                        if (TextUtils.isEmpty(errorMessage))
                            errorMessage = getResources().getString(R.string.some_thing_error);
                        showSnackBar(errorMessage);
                    }

                    @Override
                    public void setWebViewProperties(@Nullable WebView webView, @Nullable Object o) {
                        webView.setWebChromeClient(new CheckoutProWebChromeClient((Bank) o));
                        webView.setWebViewClient(
                                new CheckoutProWebViewClient((Bank) o, mKeyVals.getKey()));
                    }

                    @Override
                    public void generateHash(HashMap<String, String> valueMap,
                                             PayUHashGenerationListener hashGenerationListener) {
                        String hashName = valueMap.get(PayUCheckoutProConstants.CP_HASH_NAME);
                        String hashData = valueMap.get(PayUCheckoutProConstants.CP_HASH_STRING);
                        if (!TextUtils.isEmpty(hashName) && !TextUtils.isEmpty(hashData)) {
                            Log.e("CP_HASH_STRING", hashData);
                            Log.e("CP_HASH_NAME", hashName);
                            getInitiatePostBoost(
                                    mKeyVals.getTxnid(), hashData, hashName, hashGenerationListener);
                        }
                    }
                });
    }

    private PayUCheckoutProConfig getCheckoutProConfig(PgKeyVals mKeyVals) {
        PayUCheckoutProConfig config = new PayUCheckoutProConfig();
        config.setMerchantName(getString(R.string.app_name));
        config.setEnforcePaymentList(getEnforcePaymentList(mKeyVals));
        config.setShowCbToolbar(false);
        config.setAutoSelectOtp(false);
        config.setAutoApprove(false);
        config.setMerchantSmsPermission(true);
        config.setShowExitConfirmationOnPaymentScreen(true);
        config.setShowExitConfirmationOnCheckoutScreen(true);
        config.setMerchantLogo(R.drawable.app_logo);
        config.setMerchantResponseTimeout(10000);
        config.setWaitingTime(30000);
        return config;
    }

    private ArrayList<HashMap<String, String>> getEnforcePaymentList(PgKeyVals mKeyVals) {
        ArrayList<HashMap<String, String>> enforceList = new ArrayList<>();
        if (mKeyVals.getEnforce_paymethod() != null
                && !mKeyVals.getEnforce_paymethod().isEmpty()) {
            HashMap<String, String> map = new HashMap<>();
            String method = mKeyVals.getEnforce_paymethod().toLowerCase();
            if (method.contains("debitcard") || method.contains("debit card")) {
                map.put(PayUCheckoutProConstants.CP_PAYMENT_TYPE, PaymentType.CARD.name());
                map.put(PayUCheckoutProConstants.CP_CARD_TYPE, CardType.DC.name());
            } else if (method.contains("creditcard") || method.contains("credit card")) {
                map.put(PayUCheckoutProConstants.CP_PAYMENT_TYPE, PaymentType.CARD.name());
                map.put(PayUCheckoutProConstants.CP_CARD_TYPE, CardType.CC.name());
            } else if (method.contains("upi")) {
                map.put(PayUCheckoutProConstants.CP_PAYMENT_TYPE, PaymentType.UPI_INTENT.name());
            } else if (method.contains("net banking") || method.contains("netbanking")) {
                map.put(PayUCheckoutProConstants.CP_PAYMENT_TYPE, PaymentType.NB.name());
            } else if (method.contains("wallet")) {
                map.put(PayUCheckoutProConstants.CP_PAYMENT_TYPE, PaymentType.WALLET.name());
            }
            enforceList.add(map);
        }
        return enforceList;
    }

    // -----------------------------------------------------------------------
    // API — PayU transaction status update
    // -----------------------------------------------------------------------
    public void payuStatusUpdate(String tid) {
        try {
            loader.show();
            EndPointInterface git = ApiClient.getClient().create(EndPointInterface.class);
            Call<UpgradePackageResponse> call =
                    git.payUTransactionUpdate("Bearer " + tokenManager.getAccessToken(), tid);
            call.enqueue(new Callback<UpgradePackageResponse>() {
                @Override
                public void onResponse(@NonNull Call<UpgradePackageResponse> call,
                                       @NonNull Response<UpgradePackageResponse> response) {
                    if (loader != null && loader.isShowing()) loader.dismiss();
                    try {
                        UpgradePackageResponse packageResponse = response.body();
                        if (packageResponse != null) {
                            if (packageResponse.getStatusCode() == 1) {
                                UtilMethods.INSTANCE.SuccessWithOkay(
                                        CreateNewAd.this, packageResponse.getResponseText(), false);
                            } else {
                                UtilMethods.INSTANCE.Error(
                                        CreateNewAd.this, packageResponse.getResponseText());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        UtilMethods.INSTANCE.Error(CreateNewAd.this, e.getMessage());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UpgradePackageResponse> call,
                                      @NonNull Throwable t) {
                    if (loader != null && loader.isShowing()) loader.dismiss();
                    try {
                        UtilMethods.INSTANCE.apiFailureError(CreateNewAd.this, t);
                    } catch (IllegalStateException ise) {
                        UtilMethods.INSTANCE.Error(CreateNewAd.this, ise.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (loader != null && loader.isShowing()) loader.dismiss();
            UtilMethods.INSTANCE.Error(CreateNewAd.this, e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Bottom-sheet dialogs
    // -----------------------------------------------------------------------
    public void openGoalBottomSheetDialog(Activity context) {
        if (bottomGoalDialogReport != null && bottomGoalDialogReport.isShowing()) return;
        bottomGoalDialogReport = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_goal_list, null);
        bottomGoalDialogReport.setContentView(sheetView);
        BottomSheetBehavior.from(bottomGoalDialogReport.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomGoalDialogReport.show();
    }

    public void openSpecialCatBottomSheetDialog(Activity context) {
        if (bottomSpecialCatDialogReport != null && bottomSpecialCatDialogReport.isShowing()) return;
        bottomSpecialCatDialogReport = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_special_category, null);
        bottomSpecialCatDialogReport.setContentView(sheetView);
        BottomSheetBehavior.from(bottomSpecialCatDialogReport.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSpecialCatDialogReport.show();
    }

    public void openChooseAudienceBottomSheetDialog(Activity context) {
        if (bottomChooseAudienceCatDialogReport != null
                && bottomChooseAudienceCatDialogReport.isShowing()) return;
        bottomChooseAudienceCatDialogReport = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_choose_audience, null);
        bottomChooseAudienceCatDialogReport.setContentView(sheetView);
        BottomSheetBehavior.from(bottomChooseAudienceCatDialogReport.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomChooseAudienceCatDialogReport.show();
    }

    public void openIsSecureBottomSheetDialog(Activity context) {
        if (bottomIsSecureCatDialogReport != null && bottomIsSecureCatDialogReport.isShowing()) return;
        bottomIsSecureCatDialogReport = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_is_secure, null);
        bottomIsSecureCatDialogReport.setContentView(sheetView);
        BottomSheetBehavior.from(bottomIsSecureCatDialogReport.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomIsSecureCatDialogReport.show();
    }

    public void openPlacementsBottomSheetDialog(Activity context) {
        if (bottomPlacementsCatDialogReport != null
                && bottomPlacementsCatDialogReport.isShowing()) return;
        bottomPlacementsCatDialogReport = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_placements, null);
        bottomPlacementsCatDialogReport.setContentView(sheetView);
        BottomSheetBehavior.from(bottomPlacementsCatDialogReport.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomPlacementsCatDialogReport.show();
    }

    public void openBudgetBottomSheetDialog(Activity context) {
        if (bottomBudgetCatDialogReport != null && bottomBudgetCatDialogReport.isShowing()) return;
        bottomBudgetCatDialogReport = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_budget, null);
        bottomBudgetCatDialogReport.setContentView(sheetView);
        BottomSheetBehavior.from(bottomBudgetCatDialogReport.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomBudgetCatDialogReport.show();
    }

    public void openPaymentBottomSheetDialog(Activity context) {
        if (bottomPaymentCatDialogReport != null && bottomPaymentCatDialogReport.isShowing()) return;
        bottomPaymentCatDialogReport = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_payment, null);
        sheetView.findViewById(R.id.imageView2).setOnClickListener(v -> finish());
        bottomPaymentCatDialogReport.setContentView(sheetView);
        BottomSheetBehavior.from(bottomPaymentCatDialogReport.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet))
                .setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomPaymentCatDialogReport.show();
    }

    // -----------------------------------------------------------------------
    // Snackbar
    // -----------------------------------------------------------------------
    private void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
    }
}