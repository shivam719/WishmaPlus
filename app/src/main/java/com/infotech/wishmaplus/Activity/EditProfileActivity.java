package com.infotech.wishmaplus.Activity;

import static android.widget.Toast.LENGTH_SHORT;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.infotech.wishmaplus.Api.Object.BankResult;
import com.infotech.wishmaplus.Api.Object.CityResult;
import com.infotech.wishmaplus.Api.Object.StateResult;
import com.infotech.wishmaplus.Api.Request.UpdateUserRequest;
import com.infotech.wishmaplus.Api.Response.BasicListResponse;
import com.infotech.wishmaplus.Api.Response.SignUpResponse;
import com.infotech.wishmaplus.Api.Response.UserDetailResponse;
import com.infotech.wishmaplus.CategoryPickerBottomSheet;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.ApiClient;
import com.infotech.wishmaplus.Utils.ApiHandler;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.EndPointInterface;
import com.infotech.wishmaplus.Utils.FileUtils;
import com.infotech.wishmaplus.Utils.PreferencesManager;
import com.infotech.wishmaplus.Utils.UpdatePageRequest;
import com.infotech.wishmaplus.Utils.UtilMethods;
import com.wishmaplus.image.picker.ImagePicker;

import java.io.File;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_IMAGE = 7676;
    // ─── Data ──────────────────────────────────────────────────────────────
    UserDetailResponse userDetailResponse;
    int isCoverPhoto = 0;
    // ─── User Profile Fields ───────────────────────────────────────────────
    private EditText etName, etGender, etLastName, etBio, etAddress,
            etState, etCity, etBank, etBranch, etIfsc, etAccountNo, etAccountName;
    // ─── Page Fields ───────────────────────────────────────────────────────
    private EditText etPageName, etPageBio, etPageWebsite,
            etPageEmail, etPagePhone, etPageAddress,et_page_category,etDate;

    // ─── Shared Views ──────────────────────────────────────────────────────
    private ImageView profile_picture, cover_photo;
    // ─── Containers (to show/hide sections) ───────────────────────────────
    private View layoutUserFields;   // group wrapping all user-only fields
    private View layoutPageFields;   // group wrapping all page-only fields
    private RequestOptions requestOptionsUserImage;
    private RequestOptions requestOptionsCoverImage;
    private PreferencesManager tokenManager;
    private int selectedGender;
    private int selectedStateId;
    private int selectedBankId = 0;
    private int selectedCityId;
    private CustomLoader loader;
    private ArrayList<StateResult> stateList = new ArrayList<>();
    private ArrayList<CityResult> cityList = new ArrayList<>();
    private ArrayList<BankResult> bankList = new ArrayList<>();
    private ImagePicker imagePicker;
    private Snackbar mSnackBar;
    /**
     * true  → normal user profile
     * false → page profile
     */
    private boolean isUserProfile = true;
    private String pageId = null;
    private String selectedPageCategoryIds="";

    // ──────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
/*

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editProfileV), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
*/

        userDetailResponse = getIntent().getParcelableExtra("UserDetail");
        tokenManager = new PreferencesManager(this, 1);
        loader = new CustomLoader(this, android.R.style.Theme_Translucent_NoTitleBar);

        if (userDetailResponse == null) {
            userDetailResponse = UtilMethods.INSTANCE.getUserDetailResponse(tokenManager);
        }

        // ── Determine mode from intent ──────────────────────────────────
        // Caller sets isProfile = true  → user's own profile
        //                  isProfile = false → page profile
        isUserProfile = getIntent().getBooleanExtra("isProfile", true);
        pageId = getIntent().getStringExtra("pageId");

        if (requestOptionsUserImage == null)
            requestOptionsUserImage = UtilMethods.INSTANCE.getRequestOption_With_UserIcon();
        if (requestOptionsCoverImage == null)
            requestOptionsCoverImage = UtilMethods.INSTANCE.getRequestOption_With_CoverImage();

        findViews();
        applyMode();   // show/hide sections based on isUserProfile
        init();
    }

    // ──────────────────────────────────────────────────────────────────────
    private void findViews() {
        // shared
        profile_picture = findViewById(R.id.profile_picture);
        cover_photo = findViewById(R.id.cover_photo);

        // ── user fields
        layoutUserFields = findViewById(R.id.layoutUserFields);
        etName = findViewById(R.id.et_name);
        etLastName = findViewById(R.id.et_LastName);
        etDate = findViewById(R.id.etDate);
        etGender = findViewById(R.id.et_gender);
        etBio = findViewById(R.id.et_bio);
        etAddress = findViewById(R.id.et_address);
        etState = findViewById(R.id.et_state);
        etCity = findViewById(R.id.et_city);
        etBank = findViewById(R.id.et_bank);
        etBranch = findViewById(R.id.et_branch);
        etIfsc = findViewById(R.id.et_ifsc);
        etAccountNo = findViewById(R.id.et_account_number);
        etAccountName = findViewById(R.id.et_account_name);

        // ── page fields
        layoutPageFields = findViewById(R.id.layoutPageFields);
        etPageName = findViewById(R.id.et_page_name);
        etPageBio = findViewById(R.id.et_page_bio);
        etPageWebsite = findViewById(R.id.et_page_website);
        etPageEmail = findViewById(R.id.et_page_email);
        etPagePhone = findViewById(R.id.et_page_phone);
        etPageAddress = findViewById(R.id.et_page_address);
        et_page_category = findViewById(R.id.et_page_category);


        // ── back button
        findViewById(R.id.back_button).setOnClickListener(view -> finish());

        // ── profile photo edit button
        findViewById(R.id.edit_profile_icon).setOnClickListener(view -> {
            isCoverPhoto = 0;
            checkAndPickImage();
        });

        // ── cover photo edit button
        findViewById(R.id.edit_cover_icon).setOnClickListener(view -> {
            isCoverPhoto = 1;
            checkAndPickImage();
        });

        // ── gender picker (user only)
        ArrayList<String> genderList = new ArrayList<>(Arrays.asList("Male", "Female", "Prefer Not to Say"));
        if (etGender != null) {
            etGender.setOnClickListener(view ->
                    UtilMethods.INSTANCE.openListBottomSheetDialog(this, "Select Gender", genderList, value -> {
                        etGender.setText(value);
                        selectedGender = genderList.indexOf(value) + 1;
                    }));
        }
        if (etDate != null) {
            etDate.setOnClickListener(view -> {
                Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR);
                int currentMonth = calendar.get(Calendar.MONTH);
                int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

                // Max selectable date = today minus 18 years (blocks future dates AND under-18 dates)
                Calendar maxDateCal = Calendar.getInstance();
                maxDateCal.set(currentYear - 18, currentMonth, currentDay);

                // Min selectable date = today minus 100 years (reasonable lower bound)
                Calendar minDateCal = Calendar.getInstance();
                minDateCal.set(currentYear - 100, currentMonth, currentDay);

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        this,
                        (datePicker, year, month, dayOfMonth) -> {
                            Calendar selectedCal = Calendar.getInstance();
                            selectedCal.set(year, month, dayOfMonth);

                            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
                            String selectedDate = outputFormat.format(selectedCal.getTime());
                            etDate.setText(selectedDate);
                        },
                        maxDateCal.get(Calendar.YEAR),
                        maxDateCal.get(Calendar.MONTH),
                        maxDateCal.get(Calendar.DAY_OF_MONTH)
                );

                datePickerDialog.getDatePicker().setMaxDate(maxDateCal.getTimeInMillis());
                datePickerDialog.getDatePicker().setMinDate(minDateCal.getTimeInMillis());

                datePickerDialog.show();
            });
        }

        // ── state picker (user only)
        if (etState != null) {
            etState.setOnClickListener(view ->
                    UtilMethods.INSTANCE.openListBottomSheetDialog(this, "Select State", stateList, value -> {
                        etState.setText(value.getStateName());
                        selectedStateId = value.getStateId();
                        getCity(this);
                    }));
        }

        // ── city picker (user only)
        if (etCity != null) {
            etCity.setOnClickListener(view ->
                    UtilMethods.INSTANCE.openListBottomSheetDialog(this, "Select City", cityList, value -> {
                        etCity.setText(value.getCityName());
                        selectedCityId = value.getCityId();
                    }));
        }

        // ── bank picker (user only)
        if (etBank != null) {
            etBank.setOnClickListener(view ->
                    UtilMethods.INSTANCE.openListBottomSheetDialog(this, "Select Bank", bankList, value -> {
                        etBank.setText(value.getBranchName());
                        selectedBankId = value.getBankId();
                        if (etIfsc != null) etIfsc.setText(value.getIfsc());
                    }));
        }

        // ── save button
        findViewById(R.id.bt_save).setOnClickListener(v -> {
            if (isUserProfile) {
                saveUserProfile();
            } else {
                savePageProfile();
            }
        });
    }

    // ── Show / hide sections based on mode ─────────────────────────────────
    private void applyMode() {
        if (isUserProfile) {
            // Normal user profile
            if (layoutUserFields != null) layoutUserFields.setVisibility(View.VISIBLE);
            if (layoutPageFields != null) layoutPageFields.setVisibility(View.GONE);

            // Update title
            TextView title = findViewById(R.id.user_title);
            if (title != null) title.setText("Edit Profile");

        } else {
            // Page profile
            if (layoutUserFields != null) layoutUserFields.setVisibility(View.GONE);
            if (layoutPageFields != null) layoutPageFields.setVisibility(View.VISIBLE);

            // Update title
            TextView title = findViewById(R.id.user_title);
            if (title != null) title.setText("Edit Page");
        }
    }

    // ── Populate fields from userDetailResponse ────────────────────────────
    void init() {
        // Load profile picture (shared)
        Glide.with(this)
                .load(userDetailResponse.getProfilePictureUrl())
                .apply(requestOptionsUserImage)
                .into(profile_picture);

        // Load cover photo (shared)
        Glide.with(this)
                .load(userDetailResponse.getCoverPictureUrl())
                .apply(requestOptionsCoverImage)
                .into(cover_photo);

        if (isUserProfile) {
            // ── User fields
            etName.setText(userDetailResponse.getFisrtName());
            etLastName.setText(userDetailResponse.getLastName());
            etBio.setText(userDetailResponse.getBio());
            etGender.setText(userDetailResponse.getGender());
            etDate.setText(userDetailResponse.getDob());
            etAddress.setText(userDetailResponse.getAddress());
            etState.setText(userDetailResponse.getStateName());
            etCity.setText(userDetailResponse.getCityName());
            selectedGender = userDetailResponse.getGenderId();
            selectedCityId = userDetailResponse.getCityId();
            selectedStateId = userDetailResponse.getStateId();
            selectedBankId = userDetailResponse.getBankId();
            if (etAccountName != null) etAccountName.setText(userDetailResponse.getAccountHolder());
            if (etAccountNo != null) etAccountNo.setText(userDetailResponse.getAccountNumber());
            if (etBranch != null) etBranch.setText(userDetailResponse.getBranchName());
            if (etIfsc != null) etIfsc.setText(userDetailResponse.getIfsc());

            getState(this);
            getBank(this);
            if (selectedStateId > 0) getCity(this);

        } else {
            // ── Page fields – data comes from userDetailResponse.getResult() (GroupDetailsResponse result)
            // Adjust getters based on your actual model
            if (userDetailResponse != null) {
                if (etPageName != null)
                    etPageName.setText(userDetailResponse.getFisrtName());
                if (etPageBio != null)
                     etPageBio.setText(userDetailResponse.getBio());
                if (etPageWebsite != null)
                     etPageWebsite.setText(userDetailResponse.getWebsite());
                if (etPageEmail != null)
                     etPageEmail.setText(userDetailResponse.getEmail());
                if (etPagePhone != null)
                     etPagePhone.setText(userDetailResponse.getMobileNo());
                if (etPageAddress != null)
                     etPageAddress.setText(userDetailResponse.getAddress());
                if (et_page_category != null) {
                    selectedPageCategoryIds = userDetailResponse.getPageCategoryId();
                    et_page_category.setText(userDetailResponse.getCategoryName());
                }
            }
        }
        if (et_page_category != null) {
            et_page_category.setOnClickListener(view -> {
                CategoryPickerBottomSheet sheet =
                        CategoryPickerBottomSheet.newInstance(selectedPageCategoryIds);

                sheet.setOnCategoriesSelectedListener((ids, names) -> {
                    selectedPageCategoryIds = ids;          // "1,5,12"
                    et_page_category.setText(names);        // "Local Business, Cafe, Gym"
                });

                sheet.show(getSupportFragmentManager(), "CategoryPicker");
            });
        }
        // ── Image Picker (shared)
        imagePicker = new ImagePicker(this, null, imageUri -> {
            if (imageUri == null) return;

            String filePath = FileUtils.getPath(this, imageUri);
            if (filePath == null) return;

            if (isCoverPhoto == 1) {
                userDetailResponse.setCoverPictureUrl(filePath);
                Glide.with(this).load(filePath).apply(requestOptionsCoverImage).into(cover_photo);
            } else {
                userDetailResponse.setProfilePictureUrl(filePath);
                Glide.with(this).load(filePath).apply(requestOptionsUserImage).into(profile_picture);
            }

            if (isUserProfile) {
                // User profile picture update
                updateUserProfilePicture(new File(filePath));
            } else {
                // Page profile picture update
                updatePageProfilePicture(new File(filePath));
            }

        }).setWithImageCrop();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Save – User Profile
    // ──────────────────────────────────────────────────────────────────────
    private void saveUserProfile() {
        if (!validateFirstName()) return;
        if (!validateLastName()) return;

        if (selectedBankId != 0) {
            if (etBranch.getText().toString().trim().length() < 3) {
                Toast.makeText(this, "Enter Valid Branch name", LENGTH_SHORT).show();
                return;
            }
            if (etIfsc.getText().toString().trim().length() < 7) {
                Toast.makeText(this, "Enter Valid IFSC Code", LENGTH_SHORT).show();
                return;
            }
            if (etAccountNo.getText().toString().trim().length() < 8) {
                Toast.makeText(this, "Enter Valid Account Number", LENGTH_SHORT).show();
                return;
            }
            if (etAccountName.getText().toString().trim().length() < 3) {
                Toast.makeText(this, "Enter Valid Account Holder name", LENGTH_SHORT).show();
                return;
            }
        }

        try {
            loader.show();
            EndPointInterface git = ApiClient.getClient().create(EndPointInterface.class);
            Call<SignUpResponse> call = git.updateUser(
                    "Bearer " + tokenManager.getAccessToken(),
                    new UpdateUserRequest(
                            etName.getText().toString().trim(),
                            etLastName.getText().toString().trim(),
                            selectedCityId,
                            etCity.getText().toString().trim(),
                            selectedStateId,
                            etState.getText().toString().trim(),
                            etAddress.getText().toString().trim(),
                            etBio.getText().toString().trim(),
                            selectedGender,
                            selectedBankId,
                            etBranch.getText().toString().trim(),
                            etIfsc.getText().toString().trim(),
                            etAccountNo.getText().toString().trim(),
                            etAccountName.getText().toString().trim(),
                            etDate.getText().toString().trim()
                    ));

            call.enqueue(new Callback<SignUpResponse>() {
                @Override
                public void onResponse(@NonNull Call<SignUpResponse> call,
                                       @NonNull Response<SignUpResponse> response) {
                    dismissLoader();
                    try {
                        SignUpResponse res = response.body();
                        if (res != null) {
                            if (res.getStatusCode() == 1) {
                                setResult(RESULT_OK);
                                UtilMethods.INSTANCE.SuccessfulWithDismiss(
                                        EditProfileActivity.this, res.getResponseText());
                            } else {
                                UtilMethods.INSTANCE.Error(EditProfileActivity.this, res.getResponseText());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        UtilMethods.INSTANCE.Error(EditProfileActivity.this, e.getMessage());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<SignUpResponse> call, @NonNull Throwable t) {
                    dismissLoader();
                    UtilMethods.INSTANCE.apiFailureError(EditProfileActivity.this, t);
                }
            });

        } catch (Exception e) {
            dismissLoader();
            e.printStackTrace();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Save – Page Profile
    // ──────────────────────────────────────────────────────────────────────
    private void savePageProfile() {
        String pName = etPageName != null ? etPageName.getText().toString().trim() : "";
        String pBio = etPageBio != null ? etPageBio.getText().toString().trim() : "";
        String pWeb = etPageWebsite != null ? etPageWebsite.getText().toString().trim() : "";
        String pMail = etPageEmail != null ? etPageEmail.getText().toString().trim() : "";
        String pPh = etPagePhone != null ? etPagePhone.getText().toString().trim() : "";
        String pAddr = etPageAddress != null ? etPageAddress.getText().toString().trim() : "";
        String pCat = selectedPageCategoryIds != null ? selectedPageCategoryIds : "";

        if (pName.isEmpty()) {
            Toast.makeText(this, "Page name cannot be empty", LENGTH_SHORT).show();
            return;
        }

        try {
            loader.show();
            UpdatePageRequest request = new UpdatePageRequest();
            request.setPageId(pageId != null ? pageId : "");
            request.setPageName(pName);
            request.setBio(pBio);
            request.setWebsite(pWeb);
            request.setEmail(pMail);
            request.setPhone(pPh);
            request.setAddress(pAddr);
            request.setCategoryId(pCat);

            EndPointInterface git = ApiClient.getClient().create(EndPointInterface.class);
            Call<SignUpResponse> call = git.UpdatePageDetails(
                    "Bearer " + tokenManager.getAccessToken(), request);

            call.enqueue(new Callback<SignUpResponse>() {
                @Override
                public void onResponse(@NonNull Call<SignUpResponse> call,
                                       @NonNull Response<SignUpResponse> response) {
                    dismissLoader();
                    ApiHandler.handleResponse(response, EditProfileActivity.this,
                            new ApiHandler.ApiSuccess<SignUpResponse>() {
                                @Override
                                public void onSuccess(SignUpResponse res) {

                                    if (res.getStatusCode() == 1) {
                                        setResult(RESULT_OK);
                                        UtilMethods.INSTANCE.SuccessfulWithDismiss(
                                                EditProfileActivity.this,
                                                res.getResponseText()
                                        );
                                    } else {
                                        UtilMethods.INSTANCE.Error(
                                                EditProfileActivity.this,
                                                res.getResponseText()
                                        );
                                    }
                                }
                            });
                }

                @Override
                public void onFailure(@NonNull Call<SignUpResponse> call, @NonNull Throwable t) {
                    dismissLoader();

                    if (t instanceof java.net.UnknownHostException ||
                            t instanceof java.net.SocketTimeoutException) {

                        UtilMethods.INSTANCE.Error(EditProfileActivity.this,
                                "No Internet Connection");

                    } else {
                        UtilMethods.INSTANCE.apiFailureError(EditProfileActivity.this, t);
                    }
                }
            });

        } catch (Exception e) {
            dismissLoader();
            e.printStackTrace();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Update Profile Picture – User
    // ──────────────────────────────────────────────────────────────────────
    private void updateUserProfilePicture(File file) {
        try {
            loader.show();
            MultipartBody.Part part = buildFilePart(file);
            EndPointInterface git = ApiClient.getClient().create(EndPointInterface.class);
            Call<SignUpResponse> call = git.updateProfilePicture(
                    "Bearer " + tokenManager.getAccessToken(), isCoverPhoto, part);

            call.enqueue(new Callback<SignUpResponse>() {
                @Override
                public void onResponse(@NonNull Call<SignUpResponse> call,
                                       @NonNull Response<SignUpResponse> response) {
                    dismissLoader();
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().getStatusCode() == 1) {
                            setResult(RESULT_OK);
                            UtilMethods.INSTANCE.SuccessfulWithDismiss(
                                    EditProfileActivity.this, response.body().getResponseText());
                        } else {
                            Toast.makeText(EditProfileActivity.this,
                                    "Failed: " + response.body().getResponseText(), LENGTH_SHORT).show();
                        }
                    } else {
                        UtilMethods.INSTANCE.apiErrorHandle(
                                EditProfileActivity.this, response.code(), response.message());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<SignUpResponse> call, @NonNull Throwable t) {
                    dismissLoader();
                    UtilMethods.INSTANCE.apiFailureError(EditProfileActivity.this, t);
                }
            });
        } catch (Exception e) {
            dismissLoader();
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Update Profile Picture – Page
    // ──────────────────────────────────────────────────────────────────────
    private void updatePageProfilePicture(File file) {
        try {
            loader.show();

            if (file == null || !file.exists()) {
                Toast.makeText(this, "Invalid file", Toast.LENGTH_SHORT).show();
                return;
            }

            // FIX: Create RequestBody for the file with correct MediaType
            RequestBody requestFile = RequestBody.create(
                    file,
                    okhttp3.MediaType.get("image/*")
            );

            // FIX: Create MultipartBody.Part with the correct field name "ProfilePicture"
            // The second parameter is the filename, third is the RequestBody
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "ProfilePicture",  // Field name must exactly match server expectation
                    file.getName(),    // Filename
                    requestFile        // RequestBody with file data
            );

            // PageId as RequestBody
            RequestBody pageIdBody = RequestBody.create(
                    String.valueOf(pageId),
                    okhttp3.MediaType.parse("text/plain")
            );

            EndPointInterface api = ApiClient.getClient().create(EndPointInterface.class);

            Call<SignUpResponse> call = api.updatePageProfilePicture(
                    "Bearer " + tokenManager.getAccessToken(),
                    pageIdBody,
                    imagePart
            );

            call.enqueue(new Callback<SignUpResponse>() {
                @Override
                public void onResponse(@NonNull Call<SignUpResponse> call,
                                       @NonNull Response<SignUpResponse> response) {

                    dismissLoader();

                    if (response.isSuccessful() && response.body() != null) {

                        if (response.body().getStatusCode() == 1) {
                            setResult(RESULT_OK);
                            UtilMethods.INSTANCE.SuccessfulWithDismiss(
                                    EditProfileActivity.this,
                                    response.body().getResponseText()
                            );
                        } else {
                            Toast.makeText(EditProfileActivity.this,
                                    "Failed: " + response.body().getResponseText(),
                                    Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        // Log error body for debugging
                        try {
                            String errorBody = response.errorBody() != null ?
                                    response.errorBody().string() : "null";
                            Log.e("API_ERROR", "Error body: " + errorBody);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        UtilMethods.INSTANCE.apiErrorHandle(
                                EditProfileActivity.this,
                                response.code(),
                                response.message()
                        );
                    }
                }

                @Override
                public void onFailure(@NonNull Call<SignUpResponse> call, @NonNull Throwable t) {
                    dismissLoader();
                    UtilMethods.INSTANCE.apiFailureError(EditProfileActivity.this, t);
                }
            });

        } catch (Exception e) {
            dismissLoader();
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────
    private MultipartBody.Part buildFilePart(File file) {
        String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if (mimeType == null) mimeType = "application/octet-stream";
        RequestBody requestFile = RequestBody.create(file, MediaType.parse(mimeType));
        return MultipartBody.Part.createFormData("model", file.getName(), requestFile);
    }

    private void dismissLoader() {
        if (loader != null && loader.isShowing()) loader.dismiss();
    }

    private void checkAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_PERMISSIONS_IMAGE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS_IMAGE);
        } else {
            imagePicker.choosePictureWithoutPermission(true, true);
        }
    }

    private boolean validateFirstName() {
        if (!isUserProfile) return true; // not needed for page
        String firstName = etName.getText().toString().trim();
        if (firstName.isEmpty()) {
            etName.setError("Please enter name");
            return false;
        }
        if (firstName.length() < 2 || firstName.length() > 50) {
            etName.setError("Name must be between 2 and 50 characters");
            return false;
        }
        if (!firstName.matches("^[A-Za-z ]+$")) {
            etName.setError("Only letters and spaces allowed");
            return false;
        }
        return true;
    }

    private boolean validateLastName() {
        if (!isUserProfile) return true;
        String lastName = etLastName.getText().toString().trim();
        if (lastName.isEmpty()) return true; // optional
        if (lastName.length() < 2 || lastName.length() > 50) {
            etLastName.setError("Last name must be between 2 and 50 characters");
            etLastName.requestFocus();
            return false;
        }
        if (!lastName.matches("^[A-Za-z ]+$")) {
            etLastName.setError("Only alphabets and spaces are allowed");
            etLastName.requestFocus();
            return false;
        }
        etLastName.setError(null);
        return true;
    }

    // ── State / City / Bank loaders ─────────────────────────────────────
    private void getState(Activity context) {
        try {
            loader.show();
            EndPointInterface git = ApiClient.getClient().create(EndPointInterface.class);
            git.getState("Bearer " + tokenManager.getAccessToken())
                    .enqueue(new Callback<BasicListResponse<StateResult>>() {
                        @Override
                        public void onResponse(@NonNull Call<BasicListResponse<StateResult>> call,
                                               @NonNull Response<BasicListResponse<StateResult>> response) {
                            dismissLoader();
                            try {
                                BasicListResponse<StateResult> res = response.body();
                                if (res != null && res.getStatusCode() == 1 &&
                                        res.getResult() != null && !res.getResult().isEmpty()) {
                                    stateList = res.getResult();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<BasicListResponse<StateResult>> call,
                                              @NonNull Throwable t) {
                            dismissLoader();
                            UtilMethods.INSTANCE.apiFailureError(context, t);
                        }
                    });
        } catch (Exception e) {
            dismissLoader();
            e.printStackTrace();
        }
    }

    private void getCity(Activity context) {
        try {
            loader.show();
            EndPointInterface git = ApiClient.getClient().create(EndPointInterface.class);
            git.getCity("Bearer " + tokenManager.getAccessToken(), selectedStateId)
                    .enqueue(new Callback<BasicListResponse<CityResult>>() {
                        @Override
                        public void onResponse(@NonNull Call<BasicListResponse<CityResult>> call,
                                               @NonNull Response<BasicListResponse<CityResult>> response) {
                            dismissLoader();
                            try {
                                BasicListResponse<CityResult> res = response.body();
                                if (res != null && res.getStatusCode() == 1 &&
                                        res.getResult() != null && !res.getResult().isEmpty()) {
                                    cityList = res.getResult();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<BasicListResponse<CityResult>> call,
                                              @NonNull Throwable t) {
                            dismissLoader();
                            UtilMethods.INSTANCE.apiFailureError(context, t);
                        }
                    });
        } catch (Exception e) {
            dismissLoader();
            e.printStackTrace();
        }
    }

    private void getBank(Activity context) {
        try {
            loader.show();
            EndPointInterface git = ApiClient.getClient().create(EndPointInterface.class);
            git.getBank("Bearer " + tokenManager.getAccessToken(), selectedBankId)
                    .enqueue(new Callback<BasicListResponse<BankResult>>() {
                        @Override
                        public void onResponse(@NonNull Call<BasicListResponse<BankResult>> call,
                                               @NonNull Response<BasicListResponse<BankResult>> response) {
                            dismissLoader();
                            try {
                                BasicListResponse<BankResult> res = response.body();
                                if (res != null && res.getStatusCode() == 1 &&
                                        res.getResult() != null && !res.getResult().isEmpty()) {
                                    bankList = res.getResult();
                                    if (selectedBankId != 0 && etBank != null) {
                                        etBank.setText(
                                                bankList.stream()
                                                        .filter(it -> it.getBankId() == selectedBankId)
                                                        .findFirst()
                                                        .map(BankResult::getBranchName)
                                                        .orElse(getString(R.string.select_bank))
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<BasicListResponse<BankResult>> call,
                                              @NonNull Throwable t) {
                            dismissLoader();
                            UtilMethods.INSTANCE.apiFailureError(context, t);
                        }
                    });
        } catch (Exception e) {
            dismissLoader();
            e.printStackTrace();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (imagePicker != null) imagePicker.handleActivityResult(resultCode, requestCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_IMAGE) {
            int check = PackageManager.PERMISSION_GRANTED;
            for (int g : grantResults) check += g;
            if (grantResults.length > 0 && check == PackageManager.PERMISSION_GRANTED) {
                imagePicker.choosePictureWithoutPermission(true, true);
            } else {
                showWarningSnack(R.string.str_ShowOnPermisstionDenied, "Enable", true);
            }
        } else {
            if (imagePicker != null) imagePicker.handlePermission(requestCode, grantResults);
        }
    }

    void showWarningSnack(int stringId, String btn, final boolean isForSetting) {
        if (mSnackBar != null && mSnackBar.isShown()) return;
        mSnackBar = Snackbar.make(findViewById(android.R.id.content), stringId,
                Snackbar.LENGTH_INDEFINITE).setAction(btn, v -> {
            if (isForSetting) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            } else {
                checkAndPickImage();
            }
        });
        mSnackBar.setActionTextColor(getResources().getColor(R.color.colorPrimary));
        TextView mainTextView = mSnackBar.getView().findViewById(
                com.google.android.material.R.id.snackbar_text);
        mainTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(com.intuit.ssp.R.dimen._12ssp));
        mainTextView.setMaxLines(4);
        mSnackBar.show();
    }
}