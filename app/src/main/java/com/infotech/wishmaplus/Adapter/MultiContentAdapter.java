package com.infotech.wishmaplus.Adapter;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.infotech.wishmaplus.Activity.Advertisement;
import com.infotech.wishmaplus.Activity.GroupAddPeople;
import com.infotech.wishmaplus.Activity.ImageZoomViewActivity;
import com.infotech.wishmaplus.Activity.MainActivity;
import com.infotech.wishmaplus.Activity.NotEligibleForProfessional;
import com.infotech.wishmaplus.Activity.ProfessionalDashBoardPersonal;
import com.infotech.wishmaplus.Activity.ProfileActivity;
import com.infotech.wishmaplus.Activity.TurnOnProfessionalMode;
import com.infotech.wishmaplus.Activity.VideoViewActivity;
import com.infotech.wishmaplus.Activity.WebViewActivity;
import com.infotech.wishmaplus.Adapter.Interfaces.CountChangeCallBack;
import com.infotech.wishmaplus.Api.Object.ContentResult;
import com.infotech.wishmaplus.Api.Object.GroupResult;
import com.infotech.wishmaplus.Api.Object.StoryResult;
import com.infotech.wishmaplus.Api.Response.BasicResponse;
import com.infotech.wishmaplus.Api.Response.EligibilityModel;
import com.infotech.wishmaplus.Api.Response.LikeResponse;
import com.infotech.wishmaplus.Api.Response.LinkClickResponse;
import com.infotech.wishmaplus.Api.Response.UserDetailResponse;
import com.infotech.wishmaplus.Fragments.ShareDialogFragment;
import com.infotech.wishmaplus.GlideLoadHelper;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.ReelModel;
import com.infotech.wishmaplus.Utils.ApiClient;
import com.infotech.wishmaplus.Utils.ApplicationConstant;
import com.infotech.wishmaplus.Utils.BannerItem;
import com.infotech.wishmaplus.Utils.BannerResponse;
import com.infotech.wishmaplus.Utils.CommentDialog;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.EndPointInterface;
import com.infotech.wishmaplus.Utils.PreferencesManager;
import com.infotech.wishmaplus.Utils.UtilMethods;
import com.infotech.wishmaplus.Utils.Utility;
import com.infotech.wishmaplus.Utils.VideoEdit.AutoPlayVideo.CustomRecyclerView;
import com.infotech.wishmaplus.Utils.VideoEdit.AutoPlayVideo.DownloadManagerService;
import com.infotech.wishmaplus.Utils.VideoEdit.AutoPlayVideo.VideoUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MultiContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Define view types
    public static final int VIEW_TYPE_LOADING = 111;
    public static final int VIEW_TYPE_PROFILE = 222;
    public static final int VIEW_TYPE_POST = 333;
    public static final int VIEW_TYPE_REELS_GRID = 444;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_VIDEO = 2;
    private static final int VIEW_TYPE_IMAGE = 3;
    private final int screenWidth;
    private final String userId, UserUnikID, pageId;
    private final AudioManager am;
    private final List<ContentResult> contentList;
    private final PreferencesManager tokenManager;
    private final Runnable onDetailOpened;
    // ─────────────────────────────────────────────────────────────────────────
    // [TRACKING - NEW CODE]
    // Set to track which postIds have already been tracked in this session.
    // This prevents duplicate API calls — same post tracked only once per load,
    // just like Instagram/Wishma Plus do it.
    // ─────────────────────────────────────────────────────────────────────────
    private final Set<String> trackedPostIds = new HashSet<>();
    FragmentActivity context;
    ClickCallBack clickCallBack;
    int videoMaxHeight;
    boolean showBoosted;
    private RequestOptions requestOptionsUserImage = null;
    private RequestOptions requestOptionsImage, requestOptionsCoverImage, requestOptionsVideo;
    private Dialog alertDialogComment;
    private CustomRecyclerView mRecyclerView;
    private boolean isMute;
    private CommentDialog commentDialog;
    private String isFollowed;
    private int requestSentStatus;
    private boolean isRequestPending, isProfile;

    public MultiContentAdapter(String pageId, String UserUnikID, List<ContentResult> contentList, CustomRecyclerView recyclerView, PreferencesManager tokenManager, FragmentActivity context, ClickCallBack clickCallBack, boolean showBoosted,
                               final Runnable onDetailOpened, boolean isProfile) {
        if (requestOptionsUserImage == null) {
            requestOptionsUserImage = UtilMethods.INSTANCE.getRequestOption_With_UserIcon();
        }
        if (requestOptionsImage == null) {
            requestOptionsImage = UtilMethods.INSTANCE.getRequestOption_With_PlaceHolder();
        }
        if (requestOptionsCoverImage == null) {
            requestOptionsCoverImage = UtilMethods.INSTANCE.getRequestOption_With_CoverImage();
        }

        if (requestOptionsVideo == null) {
            requestOptionsVideo = UtilMethods.INSTANCE.getRequestOption_WithOut_PlaceHolder();
        }
        this.mRecyclerView = recyclerView;
        this.clickCallBack = clickCallBack;
        this.contentList = contentList;
        this.tokenManager = tokenManager;
        this.context = context;
        this.onDetailOpened = onDetailOpened;
        commentDialog = new CommentDialog(context, tokenManager);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        videoMaxHeight = (int) (displayMetrics.heightPixels / 1.4);
        this.userId = tokenManager.getUserId();
        this.UserUnikID = UserUnikID;
        this.pageId = pageId;
        this.showBoosted = showBoosted;
        this.isProfile = isProfile;

        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static void openInAppWebView(Context context, String webUrl, CustomLoader loader, String postId, int clickType) {
        if (webUrl == null || webUrl.isEmpty()) return;
        loader.show();
        UtilMethods.INSTANCE.insertLinkClick(postId, clickType, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                if (loader.isShowing()) {
                    loader.dismiss();
                }
                LinkClickResponse linkClickResponse = (LinkClickResponse) object;
                if (linkClickResponse.getStatusCode() == 1) {
                    Intent intent = new Intent(context, WebViewActivity.class);
                    intent.putExtra(WebViewActivity.EXTRA_URL, webUrl);
                    context.startActivity(intent);
                }

            }

            @Override
            public void onError(String msg) {
                if (loader != null) {
                    if (loader.isShowing()) {
                        loader.dismiss();
                    }
                }

            }
        });


    }

    public static void redirectToCall(Context context, String mobileNumber, CustomLoader loader, String postId, int clickType) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) return;
        loader.show();
        UtilMethods.INSTANCE.insertLinkClick(postId, clickType, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                if (loader != null) {
                    if (loader.isShowing()) {
                        loader.dismiss();
                    }
                }
                LinkClickResponse linkClickResponse = (LinkClickResponse) object;
                if (linkClickResponse.getStatusCode() == 1) {
                    String formattedNumber = mobileNumber.replaceAll("\\s+", "");
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + formattedNumber));
                    context.startActivity(intent);
                }

            }

            @Override
            public void onError(String msg) {
                if (loader != null) {
                    if (loader.isShowing()) {
                        loader.dismiss();
                    }
                }

            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [TRACKING - NEW CODE]
    // trackPostView() — Central method to call the TrackPostView API.
    //
    // Parameters:
    //   postId          — ID of the post being tracked
    //   durationSeconds — How long the user watched (0 for image/text posts,
    //                     actual seconds for video posts)
    //
    // Called from:
    //   - trackVisibleImagePosts() → for IMAGE and TEXT posts (duration = 0)
    //   - VideoViewHolder          → for VIDEO posts (duration = actual watch time)
    // ─────────────────────────────────────────────────────────────────────────
    private void trackPostView(String postId, int durationSeconds) {
        UtilMethods.INSTANCE.trackPostView(postId, durationSeconds, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
            }

            @Override
            public void onError(String msg) {

            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [TRACKING - NEW CODE]
    // getVisibleHeightPercent() — Calculates what fraction of a view is
    // currently visible on screen (0.0 = fully hidden, 1.0 = fully visible).
    //
    // Used by trackVisibleImagePosts() to decide if a post qualifies
    // as "seen" (threshold: 50% visible, same as Meta/Instagram standard).
    // ─────────────────────────────────────────────────────────────────────────
    private float getVisibleHeightPercent(View view) {
        if (view == null || !view.isShown()) return 0f;
        Rect rect = new Rect();
        boolean isVisible = view.getGlobalVisibleRect(rect);
        if (!isVisible) return 0f;
        float visibleHeight = rect.height();
        float totalHeight = view.getHeight();
        if (totalHeight == 0) return 0f;
        return visibleHeight / totalHeight;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [TRACKING - NEW CODE]
    // trackVisibleImagePosts() — Scans all currently visible RecyclerView items
    // and tracks IMAGE and TEXT posts that are 50%+ visible on screen.
    //
    // This is the industry-standard approach (used by Instagram, Wishma Plus):
    //   ✓ Track on actual visibility — NOT on bind
    //   ✓ Track only once per post per session (trackedPostIds Set)
    //   ✓ Video posts are excluded here — they are handled separately
    //     via ExoPlayer listeners with actual duration
    //
    // Called from:
    //   - Activity/Fragment's RecyclerView.OnScrollListener (scroll idle state)
    //   - mRecyclerView.post() after first data load
    // ─────────────────────────────────────────────────────────────────────────
    public void trackVisibleImagePosts() {
        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mRecyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();

        for (int i = first; i <= last; i++) {
            if (i < 0 || i >= contentList.size()) continue;
            ContentResult content = contentList.get(i);

            // Only IMAGE (3) and TEXT (1) posts — VIDEO (2) tracked separately
            int type = content.getContentTypeId();
            if (type != VIEW_TYPE_IMAGE && type != VIEW_TYPE_TEXT) continue;

            // Skip if already tracked in this session
            if (trackedPostIds.contains(content.getPostId())) continue;

            // Get the ViewHolder for this position
            RecyclerView.ViewHolder vh =
                    mRecyclerView.findViewHolderForAdapterPosition(i);
            if (vh == null) continue;

            // Only track if 50% or more of the item is visible
            if (getVisibleHeightPercent(vh.itemView) >= 0.5f) {
                trackedPostIds.add(content.getPostId());   // Mark as tracked
                trackPostView(content.getPostId(), 0);     // Duration 0 for non-video
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [TRACKING - NEW CODE]
    // onViewRecycled() — Called by RecyclerView when a ViewHolder is recycled
    // (i.e., scrolled off screen and reused for another item).
    //
    // For VIDEO ViewHolders: if the video was playing and user scrolled away
    // mid-watch, we flush the accumulated watch duration and send it to the API.
    // This ensures no watch time is lost even when user doesn't finish the video.
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof VideoViewHolder) {
            VideoViewHolder vh = (VideoViewHolder) holder;

            // If video was playing when recycled, accumulate the last segment
            if (vh.videoPlayStartTime > 0) {
                vh.totalWatchedMs += System.currentTimeMillis() - vh.videoPlayStartTime;
                vh.videoPlayStartTime = 0;
            }

            // Send whatever watch time was accumulated (if any)
            if (vh.totalWatchedMs > 0 && vh.currentPostId != null) {
                trackPostView(vh.currentPostId, Math.toIntExact(vh.totalWatchedMs / 1000));
                vh.totalWatchedMs = 0;
            }
        }
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return contentList.get(position).getContentTypeId() > 0 ? contentList.get(position).getContentTypeId() : (context instanceof ProfileActivity) ? VIEW_TYPE_PROFILE : VIEW_TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_LOADING) {
            View view = inflater.inflate(R.layout.adapter_loading, parent, false);
            return new LoadingViewHolder(view);
        } else if (viewType == VIEW_TYPE_POST) {
            View view = inflater.inflate(R.layout.adapter_post, parent, false);
            return new PostViewHolder(view);
        } else if (viewType == VIEW_TYPE_PROFILE) {
            View view = inflater.inflate(R.layout.adapter_user_details, parent, false);
            return new ProfileViewHolder(view);
        } else if (viewType == VIEW_TYPE_TEXT) {
            View view = inflater.inflate(R.layout.adapter_text, parent, false);
            return new TextViewHolder(view);
        } else if (viewType == VIEW_TYPE_VIDEO) {
            View view = inflater.inflate(R.layout.adapter_video, parent, false);
            return new VideoViewHolder(view);
        } else if (viewType == VIEW_TYPE_IMAGE) {
            View view = inflater.inflate(R.layout.adapter_photo, parent, false);
            return new ImageViewHolder(view);
        } else if (viewType == VIEW_TYPE_REELS_GRID) {
            View view = inflater.inflate(R.layout.adapter_profile_reels_grid, parent, false);
            return new ReelsGridViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.adapter_nothing, parent, false);
            return new NothingHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ContentResult content = contentList.get(position);

        if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).bind(content);
        } else if (holder instanceof PostViewHolder) {
            ((PostViewHolder) holder).bind(content);
        } else if (holder instanceof ProfileViewHolder) {
            ((ProfileViewHolder) holder).bind(content, position);
        } else if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).bind(content, position);
        } else if (holder instanceof VideoViewHolder) {
            ((VideoViewHolder) holder).bind(content, position);
        } else if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind(content, position);
        } else if (holder instanceof ReelsGridViewHolder) {
            List<ReelModel> reels = new ArrayList<>();
            if (context instanceof ProfileActivity) {
                reels = ((ProfileActivity) context).getMyReelsList();
            }
            ((ReelsGridViewHolder) holder).bind(reels, pageId);
        } else {
            ((NothingHolder) holder).bind(content, position);
        }
    }

    private void followUser(String userId, AppCompatTextView friendUnfriend, int position) {
        UtilMethods.INSTANCE.doFollow(context, userId, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                LikeResponse likeResponse = (LikeResponse) object;
                if(likeResponse.getStatusCode()==1||likeResponse.getStatusCode()==-1) {
                    context.setResult(RESULT_OK);
                    context.finish();
                    updateFollowUnfollowState(likeResponse.getStatusCode(), friendUnfriend, position);
                } else{
                    UtilMethods.INSTANCE.Error(context, likeResponse.getResponseText());
                }
            }

            @Override
            public void onError(String msg) {
                UtilMethods.INSTANCE.Error(context,msg);
            }
        });


    }

    private void addFriend(String userId, AppCompatTextView addFriend, int position) {
        UtilMethods.INSTANCE.createRequest(context, userId, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                BasicResponse basicResponse = (BasicResponse) object;
                if (basicResponse.getStatusCode() == 1) {
                    updateFollowUnfollowState(0, addFriend, position);
                    context.setResult(RESULT_OK);
                    context.finish();
                } else {
                    UtilMethods.INSTANCE.Error(context, basicResponse.getResponseText());
                }

            }

            @Override
            public void onError(String msg) {

            }
        });


    }

    @SuppressLint("SetTextI18n")
    private void updateFollowUnfollowState(int statusCode, AppCompatTextView friendUnfriend, int position) {
        notifyItemChanged(position);
        if (context instanceof ProfileActivity) {
            ((ProfileActivity) context).refresh();

        }

    }

    private void showPopupMenu(View view, ContentResult content, int position, PlayerView playerView) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.dialog_post_popup, null);

        PopupWindow popupWindow = new PopupWindow(popupView, (int) context.getResources().getDimension(com.intuit.sdp.R.dimen._160sdp), ViewGroup.LayoutParams.WRAP_CONTENT, true);

        TextView edit = popupView.findViewById(R.id.edit);
        TextView delete = popupView.findViewById(R.id.delete);
        TextView report = popupView.findViewById(R.id.report);
        TextView copyLink = popupView.findViewById(R.id.copyLink);
        View editLine = popupView.findViewById(R.id.editLine);
        View deleteLine = popupView.findViewById(R.id.deleteLine);
        View copyLinkLine = popupView.findViewById(R.id.copyLinkLine);

        if (content.getUserId().equalsIgnoreCase(userId) || content.getParsedSharedData() != null && content.getParsedSharedData().getUserId().equalsIgnoreCase(userId)) {
            report.setVisibility(GONE);
            copyLinkLine.setVisibility(GONE);
        } else {
            edit.setVisibility(GONE);
            editLine.setVisibility(GONE);
            delete.setVisibility(GONE);
            deleteLine.setVisibility(GONE);
        }
        edit.setOnClickListener(v -> {
            popupWindow.dismiss();
            if (clickCallBack != null) {
                clickCallBack.onClickProfile(content.getPostId(), content);
            }
        });
        delete.setOnClickListener(v -> {
            popupWindow.dismiss();
            showDeleteConfirmationDialog(content, position, playerView);
        });
        report.setOnClickListener(v -> {
            popupWindow.dismiss();
            UtilMethods.INSTANCE.openReportBottomSheetDialog(context, content.getPostId());
        });
        copyLink.setOnClickListener(v -> {
            popupWindow.dismiss();
            Utility.INSTANCE.setClipboard(context, ApplicationConstant.INSTANCE.postUrl + content.getPostId(), "Copy Link");
        });
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0], location[1] + view.getHeight());

    }

    private void scrollToCenter(int position) {
    }

    private void showDeleteConfirmationDialog(ContentResult content, int position, PlayerView playerView) {
        new AlertDialog.Builder(context).setTitle("Delete Content").setMessage("Are you sure you want to delete this content?").setPositiveButton("Delete", (dialog, which) -> {
            deleteContentFromServer(content.getPostId(), position, playerView);
        }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).show();
    }

    private void updateLikeState(boolean liked, int position, MaterialButton likeBtn, TextView likeCount) {
        contentList.get(position).setLiked(liked);

        int newLikesCount = liked ? contentList.get(position).getTotalLikes() + 1 : contentList.get(position).getTotalLikes() - 1;
        contentList.get(position).setTotalLikes(newLikesCount);

        if (newLikesCount > 0) {
            likeCount.setVisibility(VISIBLE);
            likeCount.setText(newLikesCount + "");
        } else {
            likeCount.setVisibility(GONE);
        }
        if (liked) {
            likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.colorFwd));
            likeBtn.setTextColor(ContextCompat.getColor(context, R.color.colorFwd));
        } else {
            likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_5));
            likeBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
        }
    }

    private void deleteContentFromServer(String postId, int position, PlayerView playerView) {
        EndPointInterface apiService = ApiClient.getClient().create(EndPointInterface.class);
        Call<BasicResponse> call = apiService.deleteComment("Bearer " + tokenManager.getAccessToken(), postId);

        call.enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (response.isSuccessful()) {
                    if (playerView != null) {
                        mRecyclerView.deleteVideo(playerView);
                    }
                    if (clickCallBack != null) {
                        clickCallBack.onDelete(position);
                    }

                    Toast.makeText(context, "Content deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to delete content", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public interface ClickCallBack {
        void onClickCreatePost(String postId);

        void onClickCreateStory(String storyId);

        void onClickProfile(String userId, ContentResult content);

        void onOpenStory(ArrayList<StoryResult> list, int position, StoryResult result);

        void onDelete(int position);
    }

    class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(ContentResult content) {
        }
    }

    class ProfileViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView profile_picture, profile, cover_photo, editProfileIcon, packageImage;
        ImageButton editCoverIcon;

        View line_1, buttons, user_details;
        Button manageGroup, addPeople;

        com.google.android.material.button.MaterialButton btnProfessionalDashboard, btnAdvertise;
        AppCompatTextView addPostTitle, user_name, storyAddBtn, packageTitle, packageName, bioTv, location, posts_tab, photos_tab, videos_tab, noDataTv, searchBar, edit_public_details, joiningDate, followers, subscribers, followersView, dotView, followingView, friendUnfriend, addFriend, reels_tab;
        private CustomLoader loader;

        public ProfileViewHolder(View itemView) {
            super(itemView);
            profile_picture = itemView.findViewById(R.id.profile_picture);
            profile = itemView.findViewById(R.id.profile);
            packageImage = itemView.findViewById(R.id.packageImage);
            packageTitle = itemView.findViewById(R.id.packageTitle);
            packageName = itemView.findViewById(R.id.packageName);
            cover_photo = itemView.findViewById(R.id.cover_photo);
            editProfileIcon = itemView.findViewById(R.id.edit_profile_icon);
            editCoverIcon = itemView.findViewById(R.id.edit_cover_icon);
            followersView = itemView.findViewById(R.id.followersView);
            buttons = itemView.findViewById(R.id.buttons);
            manageGroup = itemView.findViewById(R.id.manageGroup);
            addPeople = itemView.findViewById(R.id.addPeople);
            dotView = itemView.findViewById(R.id.dotView);
            user_details = itemView.findViewById(R.id.user_details);
            followingView = itemView.findViewById(R.id.followingView);
            friendUnfriend = itemView.findViewById(R.id.friendUnfriend);
            addFriend = itemView.findViewById(R.id.addFriend);
            btnProfessionalDashboard = itemView.findViewById(R.id.btnProfessionalDashboard);
            btnAdvertise = itemView.findViewById(R.id.btnAdvertise);
            user_name = itemView.findViewById(R.id.user_name);
            addPostTitle = itemView.findViewById(R.id.addPostTitle);
            line_1 = itemView.findViewById(R.id.line_1);
            bioTv = itemView.findViewById(R.id.bioTv);
            location = itemView.findViewById(R.id.location);
            posts_tab = itemView.findViewById(R.id.posts_tab);
            photos_tab = itemView.findViewById(R.id.photos_tab);
            videos_tab = itemView.findViewById(R.id.videos_tab);
            reels_tab = itemView.findViewById(R.id.reels_tab);
            edit_public_details = itemView.findViewById(R.id.edit_public_details);
            noDataTv = itemView.findViewById(R.id.noDataTv);
            searchBar = itemView.findViewById(R.id.search_bar);
            storyAddBtn = itemView.findViewById(R.id.storyAddBtn);
            joiningDate = itemView.findViewById(R.id.joiningDate);
            followers = itemView.findViewById(R.id.followers);
            subscribers = itemView.findViewById(R.id.subscribers);
            loader = new CustomLoader(context, android.R.style.Theme_Translucent_NoTitleBar);
        }

        @SuppressLint("SetTextI18n")
        public void bind(ContentResult content, int position) {
            if(Objects.equals(tokenManager.getUserId(), content.getUserId())){
                profile.setEnabled(false);
                profile.setClickable(false);
                profile.setFocusable(false);
            }
            if (contentList.size() > 1) {
                noDataTv.setVisibility(GONE);
            } else {
                if (context instanceof ProfileActivity && ((ProfileActivity) context).buttonContentTypeId == 2) {
                    noDataTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_video_library_big, 0, 0);
                    noDataTv.setText(R.string.video_is_not_available);
                } else if (context instanceof ProfileActivity && ((ProfileActivity) context).buttonContentTypeId == 3) {
                    noDataTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_image_big, 0, 0);
                    noDataTv.setText(R.string.photo_is_not_available);
                } else if (context instanceof ProfileActivity && ((ProfileActivity) context).buttonContentTypeId == 4) {
                    noDataTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_image_big, 0, 0);
                    noDataTv.setText(R.string.reels_not_available);
                } else {
                    noDataTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_post_add_big, 0, 0);
                    noDataTv.setText(R.string.post_is_not_available);
                }
                noDataTv.setVisibility(VISIBLE);
            }
            boolean isProfessionalDashboard = content.getUserDetail().isShowProfessionalDashboard();
            boolean isProfessional = content.getUserDetail().isProfessional();
            if (isProfessional && !(content.getUserDetail().isSelfProfile())) {
                friendUnfriend.setVisibility(VISIBLE);
            } else {
                friendUnfriend.setVisibility(GONE);
            }
            if (content.getUserDetail().isSelfProfile()) {
                btnProfessionalDashboard.setVisibility(VISIBLE);
            } else {
                btnProfessionalDashboard.setVisibility(GONE);
            }
            if (content.getUserDetail().isSelfProfile() && isProfessionalDashboard) {
                btnAdvertise.setVisibility(VISIBLE);
            } else {
                btnAdvertise.setVisibility(GONE);
            }
            btnAdvertise.setOnClickListener(view -> {
                Intent intent = new Intent(context, Advertisement.class);
                intent.putExtra("page_id", pageId);
                context.startActivity(intent);
            });
            if (isProfessionalDashboard) {
                btnProfessionalDashboard.setText("Professional Dashboard");
                btnProfessionalDashboard.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProfessionalDashBoardPersonal.class);
                    intent.putExtra("page_id", (pageId != null && !pageId.isEmpty() ? pageId : content.getUserId()));
                    String pageIdToPass = !TextUtils.isEmpty(pageId)
                            ? pageId
                            : content.getUserDetail().getUserId();
                    intent.putExtra("page_id", pageIdToPass);
                    intent.putExtra("isProfileType", isProfile);
                    context.startActivity(intent);
                });
            } else {
                btnProfessionalDashboard.setText("Turn on professional mode");
                btnProfessionalDashboard.setOnClickListener(v -> {
                    loader.show();
                    UtilMethods.INSTANCE.checkEligibilityForProfessional(context, new UtilMethods.ApiCallBackMulti() {
                        @Override
                        public void onSuccess(Object object) {
                            if (loader != null) {
                                if (loader.isShowing()) {
                                    loader.dismiss();
                                }
                            }
                            EligibilityModel eligibilityModel = (EligibilityModel) object;
                            if (eligibilityModel.getStatusCode() == 1) {
                                Intent intent;
                                if (eligibilityModel.getResponseText().equalsIgnoreCase("success")) {
                                    intent = new Intent(context, TurnOnProfessionalMode.class);
                                } else {
                                    intent = new Intent(context, NotEligibleForProfessional.class);
                                    intent.putExtra("eligibilityData", eligibilityModel);
                                }
                                context.startActivity(intent);
                            } else if (eligibilityModel.getStatusCode() == -1) {
                                Intent intent = new Intent(context, NotEligibleForProfessional.class);
                                intent.putExtra("eligibilityData", eligibilityModel);
                                context.startActivity(intent);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (loader != null) {
                                if (loader.isShowing()) {
                                    loader.dismiss();
                                }
                            }
                        }
                    });
                });
            }
            if (content.getUserDetail() != null) {
                Glide.with(context).load(content.getUserDetail().getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile_picture);
                Glide.with(context).load(content.getUserDetail().getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile);
                Glide.with(context).load(content.getUserDetail().getCoverPictureUrl()).apply(requestOptionsCoverImage).into(cover_photo);

                if (content.getUserDetail().getLastName() == null) {
                    user_name.setText(content.getUserDetail().getFisrtName());
                } else {
                    user_name.setText(content.getUserDetail().getFisrtName() + " " + content.getUserDetail().getLastName());
                }

                if (content.getUserDetail().getFollower() != null && !content.getUserDetail().getFollower().isEmpty()) {
                    String count = content.getUserDetail().getFollower();
                    String label = " followers";
                    SpannableString ss = new SpannableString(count + label);
                    ss.setSpan(new StyleSpan(Typeface.BOLD), 0, count.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new AbsoluteSizeSpan(16, true), 0, count.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new ForegroundColorSpan(Color.BLACK), 0, count.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    followersView.setText(ss);
                }
                if (content.getUserDetail().getFollowing() != null && !content.getUserDetail().getFollowing().isEmpty()) {
                    String count = content.getUserDetail().getFollowing();
                    String label = " following";
                    SpannableString ss = new SpannableString(count + label);
                    ss.setSpan(new StyleSpan(Typeface.BOLD), 0, count.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new AbsoluteSizeSpan(16, true), 0, count.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new ForegroundColorSpan(Color.BLACK), 0, count.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    followingView.setText(ss);
                }
                isFollowed = content.getUserDetail().getIsFollowed();
                requestSentStatus = content.getUserDetail().getRequestSentStatus();
                isRequestPending = content.getUserDetail().isRequestPending();

                if (requestSentStatus == 1) {
                    addFriend.setText("Respond");
                    addFriend.setBackgroundResource(R.drawable.bg_blue_rounded);
                    addFriend.setTextColor(ContextCompat.getColor(context, R.color.color_white));
                    addFriend.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.main_blue_color));
                } else if ((requestSentStatus == 0 && !isRequestPending) || (requestSentStatus == 3 && !isRequestPending)) {
                    addFriend.setText("Add Friend");
                    addFriend.setBackgroundResource(R.drawable.bg_blue_rounded);
                    addFriend.setTextColor(ContextCompat.getColor(context, R.color.color_white));
                    addFriend.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.main_blue_color));
                } else if (isRequestPending && requestSentStatus == 0) {
                    addFriend.setText("Cancel Request");
                    addFriend.setBackgroundResource(R.drawable.rounded_corners);
                    addFriend.setTextColor(ContextCompat.getColor(context, R.color.black_alpha_55));
                    addFriend.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.grey_1));
                } else if (requestSentStatus == 2) {
                    addFriend.setText("Friends");
                    addFriend.setBackgroundResource(R.drawable.bg_blue_rounded);
                    addFriend.setTextColor(ContextCompat.getColor(context, R.color.color_white));
                    addFriend.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.main_blue_color));
                }

                if ("0".equals(isFollowed)) {
                    friendUnfriend.setText("Follow");
                    friendUnfriend.setBackgroundResource(R.drawable.bg_blue_rounded);
                    friendUnfriend.setTextColor(ContextCompat.getColor(context, R.color.color_white));
                    friendUnfriend.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.main_blue_color));
                } else {
                    friendUnfriend.setText("Following");
                    friendUnfriend.setBackgroundResource(R.drawable.rounded_corners);
                    friendUnfriend.setTextColor(ContextCompat.getColor(context, R.color.black_alpha_55));
                    friendUnfriend.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.grey_1));
                }
                addFriend.setOnClickListener(v -> {
                    if (isRequestPending && requestSentStatus == 0) {
                        UtilMethods.INSTANCE.openAcceptRequestBottomSheetDialog(context, content.getUserDetail().getUserId(), content.getUserDetail().getFisrtName(), new UtilMethods.ApiCallBackMulti() {
                            @Override
                            public void onSuccess(Object object) {
                                BasicResponse basicResponse = (BasicResponse) object;
                                context.setResult(RESULT_OK);
                                context.finish();
                                UtilMethods.INSTANCE.Success(context, basicResponse.getResponseText());
                                updateFollowUnfollowState(0, friendUnfriend, position);
                                if (context instanceof ProfileActivity) {
                                    ((ProfileActivity) context).refresh();
                                }
                            }

                            @Override
                            public void onError(String msg) {
                            }
                        }, 0);
                    } else if (requestSentStatus == 1) {
                        UtilMethods.INSTANCE.openAcceptRequestBottomSheetDialog(context, content.getUserDetail().getUserId(), content.getUserDetail().getFisrtName(), new UtilMethods.ApiCallBackMulti() {
                            @Override
                            public void onSuccess(Object object) {
                                context.setResult(RESULT_OK);
                                context.finish();
                                updateFollowUnfollowState(0, friendUnfriend, position);
                                if (context instanceof ProfileActivity) {
                                    ((ProfileActivity) context).refresh();
                                }
                            }

                            @Override
                            public void onError(String msg) {
                            }
                        }, 3);
                    } else if ((requestSentStatus == 0 && (!isRequestPending)) || (requestSentStatus == 3 && (!isRequestPending))) {
                        addFriend(content.getUserDetail().getUserId(), friendUnfriend, position);
                    } else if (requestSentStatus == 2) {
                        UtilMethods.INSTANCE.openAcceptRequestBottomSheetDialog(context, content.getUserDetail().getUserId(), content.getUserDetail().getFisrtName(), new UtilMethods.ApiCallBackMulti() {
                            @Override
                            public void onSuccess(Object object) {
                                context.setResult(RESULT_OK);
                                context.finish();
                                updateFollowUnfollowState(0, friendUnfriend, position);
                                if (context instanceof ProfileActivity) {
                                    ((ProfileActivity) context).refresh();
                                }
                            }

                            @Override
                            public void onError(String msg) {
                            }
                        }, 4);
                    }
                });

                friendUnfriend.setOnClickListener(v -> {
                    if ("1".equals(isFollowed)) {
                        UtilMethods.INSTANCE.openAcceptRequestBottomSheetDialog(context, content.getUserDetail().getUserId(), content.getUserDetail().getUserId(), new UtilMethods.ApiCallBackMulti() {
                            @Override
                            public void onSuccess(Object object) {
                                context.setResult(RESULT_OK);
                                context.finish();
                                LikeResponse response = (LikeResponse) object;
                                updateFollowUnfollowState(response.getStatusCode(), friendUnfriend, position);
                                if (context instanceof ProfileActivity) {
                                    ((ProfileActivity) context).refresh();
                                }
                            }

                            @Override
                            public void onError(String msg) {
                            }
                        }, 1);
                    } else {
                        followUser(content.getUserDetail().getUserId(), friendUnfriend, position);
                    }
                });

                if (content.getUserDetail().getBio() != null && !content.getUserDetail().getBio().isEmpty()) {
                    bioTv.setVisibility(VISIBLE);
                    bioTv.setText(content.getUserDetail().getBio());
                } else {
                    bioTv.setVisibility(GONE);
                }

                if (content.getUserDetail().getCityName() != null && !content.getUserDetail().getCityName().isEmpty()) {
                    location.setVisibility(VISIBLE);
                    location.setText(Html.fromHtml(context.getResources().getString(R.string.lives_in, content.getUserDetail().getCityName(), content.getUserDetail().getStateName()), Html.FROM_HTML_MODE_LEGACY));
                } else {
                    location.setVisibility(GONE);
                }

                joiningDate.setText("Joined on " + Utility.INSTANCE.formatedDateMonthYear(content.getUserDetail().getJoiningDate()));
                if (content.getUserDetail().getTotalDownline() > 0) {
                    followers.setVisibility(VISIBLE);
                    followers.setText("Followed by " + content.getUserDetail().getTotalDownline() + " people");
                } else {
                    followers.setVisibility(GONE);
                }

                if (content.getUserDetail().getPaidDownline() > 0) {
                    subscribers.setVisibility(VISIBLE);
                    subscribers.setText("Subscribed by " + content.getUserDetail().getPaidDownline() + " people");
                } else {
                    subscribers.setVisibility(GONE);
                }

                if (content.getUserDetail().getPackageDetail() != null && isProfessionalDashboard) {
                    packageImage.setVisibility(GONE);
                    packageName.setVisibility(GONE);
                    packageTitle.setVisibility(GONE);
                    Glide.with(context).load(content.getUserDetail().getPackageDetail().getImageUrl()).apply(requestOptionsImage).into(packageImage);
                    packageName.setText(content.getUserDetail().getPackageDetail().getPackageName() + " (" + Utility.INSTANCE.formattedAmountWithRupees(content.getUserDetail().getPackageDetail().getPackageCost()) + ")");
                } else {
                    packageImage.setVisibility(GONE);
                    packageName.setVisibility(GONE);
                    packageTitle.setVisibility(GONE);
                }
            }
            editProfileIcon.setOnClickListener(view -> {
                if (context instanceof ProfileActivity) {
                    ((ProfileActivity) context).selectProfileImage();
                }
            });
            editCoverIcon.setOnClickListener(view -> {
                if (context instanceof ProfileActivity) {
                    ((ProfileActivity) context).selectCoverImage();
                }
            });
            if (isProfessionalDashboard) {
                edit_public_details.setVisibility(VISIBLE);
            } else {
                edit_public_details.setVisibility(GONE);
            }
            if (content.getUserDetail() != null) {
                String contentUserId = content.getUserDetail().getUserId();
                boolean isSelf = content.getUserDetail().isSelfProfile();
                boolean isSameUser = contentUserId != null && contentUserId.equals(userId);
                if (!isSameUser) {
                    line_1.setVisibility(GONE);
                    editProfileIcon.setVisibility(GONE);
                    editCoverIcon.setVisibility(GONE);
                    addPostTitle.setVisibility(GONE);
                    storyAddBtn.setVisibility(GONE);
                    profile.setVisibility(GONE);
                    searchBar.setVisibility(GONE);
                    addFriend.setVisibility(VISIBLE);
                }
                if (isSameUser || isSelf) {
                    addFriend.setVisibility(GONE);
                }
            }
            edit_public_details.setOnClickListener(view -> {
                if (context instanceof ProfileActivity) {
                    ((ProfileActivity) context).updateUserDetails(content.getUserDetail().isSelfProfile());
                }
            });

            if (context instanceof ProfileActivity && ((ProfileActivity) context).buttonContentTypeId == 2) {
                ViewCompat.setBackgroundTintList(posts_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(photos_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(videos_tab, context.getColorStateList(R.color.light_blue));
                ViewCompat.setBackgroundTintList(reels_tab, context.getColorStateList(R.color.bgColor));
                posts_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                photos_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                reels_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                videos_tab.setTextColor(context.getColorStateList(R.color.colorPrimaryLight));
            } else if (context instanceof ProfileActivity && ((ProfileActivity) context).buttonContentTypeId == 3) {
                ViewCompat.setBackgroundTintList(posts_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(photos_tab, context.getColorStateList(R.color.light_blue));
                ViewCompat.setBackgroundTintList(videos_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(reels_tab, context.getColorStateList(R.color.bgColor));
                posts_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                reels_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                photos_tab.setTextColor(context.getColorStateList(R.color.colorPrimaryLight));
                videos_tab.setTextColor(context.getColorStateList(R.color.grey_6));
            } else if (context instanceof ProfileActivity && ((ProfileActivity) context).buttonContentTypeId == 4) {
                ViewCompat.setBackgroundTintList(posts_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(reels_tab, context.getColorStateList(R.color.light_blue));
                ViewCompat.setBackgroundTintList(videos_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(photos_tab, context.getColorStateList(R.color.bgColor));
                posts_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                reels_tab.setTextColor(context.getColorStateList(R.color.colorPrimaryLight));
                photos_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                videos_tab.setTextColor(context.getColorStateList(R.color.grey_6));
            } else {
                ViewCompat.setBackgroundTintList(posts_tab, context.getColorStateList(R.color.light_blue));
                ViewCompat.setBackgroundTintList(photos_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(videos_tab, context.getColorStateList(R.color.bgColor));
                ViewCompat.setBackgroundTintList(reels_tab, context.getColorStateList(R.color.bgColor));
                posts_tab.setTextColor(context.getColorStateList(R.color.colorPrimaryLight));
                photos_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                videos_tab.setTextColor(context.getColorStateList(R.color.grey_6));
                reels_tab.setTextColor(context.getColorStateList(R.color.grey_6));
            }

            posts_tab.setOnClickListener(view -> {
                if (context instanceof ProfileActivity) {
                    ((ProfileActivity) context).buttonContentTypeId = 0;
                    ((ProfileActivity) context).refresh();
                }
            });
            photos_tab.setOnClickListener(view -> {
                if (context instanceof ProfileActivity) {
                    ((ProfileActivity) context).buttonContentTypeId = 3;
                    ((ProfileActivity) context).refresh();
                }
            });
            videos_tab.setOnClickListener(view -> {
                if (context instanceof ProfileActivity) {
                    ((ProfileActivity) context).buttonContentTypeId = 2;
                    ((ProfileActivity) context).refresh();
                }
            });
            reels_tab.setOnClickListener(view -> {
                if (context instanceof ProfileActivity) {
                    ((ProfileActivity) context).buttonContentTypeId = 4;
                    ((ProfileActivity) context).refresh();
                }
            });

            searchBar.setOnClickListener(view -> {
                if (clickCallBack != null) {
                    clickCallBack.onClickCreatePost("0");
                }
            });

            storyAddBtn.setOnClickListener(view -> {
                if (clickCallBack != null) {
                    clickCallBack.onClickCreateStory("0");
                }
            });

            if (content.getUserDetail().getResult() != null) {
                GroupResult groupResult = content.getUserDetail().getResult();
                GlideLoadHelper.loadWithProgress(context, groupResult.getOwnerProfileImage(), profile, null);
                Glide.with(context).load(content.getUserDetail().getResult().getCoverImageUrl()).apply(requestOptionsCoverImage).into(cover_photo);
                Glide.with(context).load(groupResult.getOwnerProfileImage()).apply(requestOptionsUserImage).into(profile);
                profile_picture.setVisibility(GONE);
                editProfileIcon.setVisibility(GONE);
                storyAddBtn.setVisibility(GONE);
                packageImage.setVisibility(GONE);
                packageTitle.setVisibility(GONE);
                packageName.setVisibility(GONE);
                followersView.setVisibility(GONE);
                dotView.setVisibility(GONE);
                followingView.setVisibility(GONE);
                user_details.setVisibility(GONE);
                btnProfessionalDashboard.setVisibility(GONE);
                location.setVisibility(GONE);
                joiningDate.setVisibility(GONE);
                edit_public_details.setVisibility(GONE);
                user_name.setText(groupResult.getTitle());
                if (groupResult.isPrivate()) {
                    bioTv.setText("Private group");
                } else {
                    bioTv.setText("Public group");
                }
                if (groupResult.isAdmin()) {
                    buttons.setVisibility(View.VISIBLE);
                    profile.setVisibility(View.VISIBLE);
                    searchBar.setVisibility(View.VISIBLE);
                    manageGroup.setOnClickListener(view -> {
                    });
                    addPeople.setOnClickListener(view -> {
                        Intent intent = new Intent(context, GroupAddPeople.class);
                        intent.putExtra("groupId", groupResult.getGroupId());
                        intent.putExtra("screenType", "dashboard");
                        context.startActivity(intent);
                    });
                } else {
                    buttons.setVisibility(View.GONE);
                    profile.setVisibility(View.GONE);
                    searchBar.setVisibility(View.GONE);
                }
            }
        }
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        // ── auto-scroll machinery ─────────────────────────────────────────────
        private static final long AUTO_SCROLL_DELAY_MS = 3000L;
        private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
        private Runnable autoScrollRunnable;

        AppCompatImageView profile;
        AppCompatTextView searchBar;
        RecyclerView storyRecyclerView;
        View line2;

        // ── banner views ─────────────────────────────────────────────────────
        View bannerSection;
        ViewPager2 bannerViewPager;
        TabLayout bannerDots;
        CustomLoader bannerLoader;

        public PostViewHolder(View itemView) {
            super(itemView);
            profile = itemView.findViewById(R.id.profile);
            searchBar = itemView.findViewById(R.id.search_bar);
            storyRecyclerView = itemView.findViewById(R.id.storyRecyclerView);
            storyRecyclerView.setLayoutManager(
                    new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
            line2 = itemView.findViewById(R.id.line2);
            bannerSection = itemView.findViewById(R.id.bannerSection);
            bannerViewPager = itemView.findViewById(R.id.bannerViewPager);
            bannerDots = itemView.findViewById(R.id.bannerDots);
            bannerLoader= new CustomLoader(context, android.R.style.Theme_Translucent_NoTitleBar);
        }

        public void bind(ContentResult content) {
            if (content.getUserDetail() != null) {
                Glide.with(context)
                        .load(content.getUserDetail().getProfilePictureUrl())
                        .apply(requestOptionsUserImage)
                        .into(profile);
            }

            if (content.getStoryList() != null && !content.getStoryList().isEmpty()) {
                storyRecyclerView.setVisibility(VISIBLE);
                line2.setVisibility(VISIBLE);
                storyRecyclerView.setAdapter(new StoryAdapter(
                        content.getStoryList(), context, content.getUserDetail(),
                        new StoryAdapter.ClickCallBack() {
                            @Override
                            public void onClickCreateStory(String storyId) {
                                if (clickCallBack != null) {
                                    clickCallBack.onClickCreateStory(storyId);
                                }
                            }

                            @Override
                            public void onOpenStory(ArrayList<StoryResult> list, int position, StoryResult result) {
                                if (clickCallBack != null) {
                                    clickCallBack.onOpenStory(list, position, result);
                                }
                            }
                        }));
            } else {
                storyRecyclerView.setVisibility(GONE);
                line2.setVisibility(GONE);
            }

            profile.setOnClickListener(view -> {
                if (clickCallBack != null) {
                    clickCallBack.onClickProfile(
                            content.getUserDetail() != null ? content.getUserDetail().getUserId() : "",
                            content);
                }
            });

            searchBar.setOnClickListener(view -> {
                if (clickCallBack != null) {
                    clickCallBack.onClickCreatePost("0");
                }
            });

            stopAutoScroll();
            bannerSection.setVisibility(GONE);
            fetchAndShowBanners();
        }
        private void fetchAndShowBanners() {
            EndPointInterface apiService = ApiClient.getClient().create(EndPointInterface.class);
            String authHeader = "Bearer " + tokenManager.getAccessToken();
            apiService.getBanners(authHeader).enqueue(new retrofit2.Callback<BannerResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<BannerResponse> call,
                                       @NonNull retrofit2.Response<BannerResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return;
                    }
                    BannerResponse body = response.body();
                    if (body.getStatusCode() != 1) {
                        return;
                    }
                    List<BannerItem> list = body.getResult();
                    if (list == null || list.isEmpty()) {
                        return;
                    }
                    showBanners(list);
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<BannerResponse> call,
                                      @NonNull Throwable t) {
                }
            });
        }
        private void showBanners(List<BannerItem> list) {
            if (!isViewHolderActive()) return;
            bannerSection.setVisibility(VISIBLE);

            BannerPagerAdapter pagerAdapter = new BannerPagerAdapter(
                    context, list,
                    banner -> {
                        String url = banner.getButtonUrl();
                        if (url != null && !url.isEmpty()) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.setPackage("com.android.chrome");
                                context.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                try {
                                    Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(fallback);
                                } catch (ActivityNotFoundException ex) {
                                    Log.w("BannerClick", "No browser found for: " + url, ex);
                                }
                            }
                        }
                    },
                    bannerLoader);

            bannerViewPager.setAdapter(pagerAdapter);
            bannerViewPager.setOffscreenPageLimit(1);

            if (list.size() > 1) {
                bannerDots.setVisibility(VISIBLE);
                new TabLayoutMediator(
                        bannerDots, bannerViewPager,
                        (tab, position) -> { }
                ).attach();
                bannerDots.post(() -> {
                    LinearLayout tabStrip = (LinearLayout) bannerDots.getChildAt(0);

                    for (int i = 0; i < tabStrip.getChildCount(); i++) {
                        View tab = tabStrip.getChildAt(i);

                        ViewGroup.MarginLayoutParams params =
                                (ViewGroup.MarginLayoutParams) tab.getLayoutParams();

                        params.setMargins(
                                dpToPx(),
                                0,
                                dpToPx(),
                                0
                        );

                        tab.setLayoutParams(params);
                    }
                });
                startAutoScroll(list.size());
            } else {
                bannerDots.setVisibility(GONE);
                bannerViewPager.setUserInputEnabled(false);
            }
        }
        private int dpToPx() {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    4,
                    itemView.getResources().getDisplayMetrics()
            );
        }
        // ─────────────────────────────────────────────────────────────────────
        // Auto-scroll helpers
        // ─────────────────────────────────────────────────────────────────────
        private void startAutoScroll(int totalPages) {
            autoScrollRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isViewHolderActive()) return;
                    int current = bannerViewPager.getCurrentItem();
                    int next = (current + 1) % totalPages;
                    bannerViewPager.setCurrentItem(next, true);
                    autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY_MS);
                }
            };
            autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY_MS);
        }

        private void stopAutoScroll() {
            if (autoScrollRunnable != null) {
                autoScrollHandler.removeCallbacks(autoScrollRunnable);
                autoScrollRunnable = null;
            }
        }

        /**
         * Returns false if this ViewHolder has already been recycled.
         * Guards against callbacks arriving after RecyclerView reused the holder.
         */
        private boolean isViewHolderActive() {
            return getBindingAdapterPosition() != RecyclerView.NO_POSITION;
        }
    }
    class NothingHolder extends RecyclerView.ViewHolder {
        public NothingHolder(View itemView) {
            super(itemView);
        }

        public void bind(ContentResult content, int position) {
        }
    }

    class TextViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        MaterialButton likeBtn, commentBtn, whatsAppBtn, shareBtn;
        TextView nameTv, nameParentTv, timeParentTv, postParentTxt, timeTv, like_count, comment_count, share_count, tvDials, userNameTitle, goalType;
        ImageView profile, moreBTn, profileParent;
        View ownerView, goalTypeLayout, callNow, bookNow;
        private CustomLoader loader;

        public TextViewHolder(View itemView) {
            super(itemView);
            ownerView = itemView.findViewById(R.id.ownerView);
            nameParentTv = itemView.findViewById(R.id.nameParentTv);
            timeParentTv = itemView.findViewById(R.id.timeParentTv);
            postParentTxt = itemView.findViewById(R.id.postParentTxt);
            profileParent = itemView.findViewById(R.id.profileParent);
            moreBTn = itemView.findViewById(R.id.moreBTn);
            nameTv = itemView.findViewById(R.id.nameTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            profile = itemView.findViewById(R.id.profile);
            textView = itemView.findViewById(R.id.textView);
            like_count = itemView.findViewById(R.id.like_count);
            comment_count = itemView.findViewById(R.id.comment_count);
            share_count = itemView.findViewById(R.id.share_count);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            whatsAppBtn = itemView.findViewById(R.id.whatsAppBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            goalTypeLayout = itemView.findViewById(R.id.goalTypeLayout);
            tvDials = itemView.findViewById(R.id.tvDials);
            userNameTitle = itemView.findViewById(R.id.userNameTitle);
            goalType = itemView.findViewById(R.id.goalType);
            callNow = itemView.findViewById(R.id.callNow);
            bookNow = itemView.findViewById(R.id.bookNow);
            loader = new CustomLoader(context, android.R.style.Theme_Translucent_NoTitleBar);
        }

        public void bind(ContentResult content, int position) {
            if(Objects.equals(tokenManager.getUserId(), content.getUserId())){
                profile.setEnabled(false);
                profile.setClickable(false);
                profile.setFocusable(false);
            }
            if (content.isBoosted() && showBoosted) {
                if (content.boostedURL() != null && !Objects.equals(content.boostedURL(), "")) {
                    goalTypeLayout.setVisibility(VISIBLE);
                    tvDials.setText("VISIT");
                    userNameTitle.setText(content.getFisrtName() + " " + content.getLastName());
                    goalType.setText("Website");
                    bookNow.setVisibility(VISIBLE);
                    callNow.setVisibility(GONE);
                    bookNow.setOnClickListener(view -> {
                        openInAppWebView(context, content.boostedURL(), loader, content.getPostId(), 1);
                    });
                } else if (content.boostedPhoneNo() != null && !Objects.equals(content.boostedPhoneNo(), "")) {
                    goalTypeLayout.setVisibility(VISIBLE);
                    tvDials.setText("DIALS");
                    userNameTitle.setText(content.getFisrtName() + " " + content.getLastName());
                    goalType.setText("Call");
                    bookNow.setVisibility(GONE);
                    callNow.setVisibility(VISIBLE);
                    callNow.setOnClickListener(view -> {
                        redirectToCall(context, content.boostedPhoneNo(), loader, content.getPostId(), 0);
                    });
                } else {
                    goalTypeLayout.setVisibility(GONE);
                }
            } else {
                goalTypeLayout.setVisibility(GONE);
            }

            if (content.getParsedSharedData() != null) {
                ownerView.setVisibility(VISIBLE);
                nameTv.setText(content.getParsedSharedData().getFisrtName() + " " + content.getParsedSharedData().getLastName());
                if (content.isBoosted() && showBoosted) {
                    timeTv.setText("Sponsored");
                } else {
                    timeTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getParsedSharedData().getEntryAt()));
                }
                if (content.getParsedSharedData().getCaption() != null && !content.getParsedSharedData().getCaption().trim().isEmpty()) {
                    textView.setText(content.getParsedSharedData().getCaption().trim());
                    textView.setVisibility(VISIBLE);
                } else {
                    textView.setVisibility(GONE);
                }
                Glide.with(context).load(content.getParsedSharedData().getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile);
                nameParentTv.setText(content.getFisrtName() + " " + content.getLastName());
                timeParentTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getEntryAt()));
                if (content.getCaption() != null && !content.getCaption().trim().isEmpty()) {
                    postParentTxt.setText(content.getCaption().trim());
                    postParentTxt.setVisibility(VISIBLE);
                } else {
                    postParentTxt.setVisibility(GONE);
                }
                Glide.with(context).load(content.getProfilePictureUrl()).apply(requestOptionsUserImage).into(profileParent);
            } else {
                ownerView.setVisibility(GONE);
                nameTv.setText(content.getFisrtName() + " " + content.getLastName());
                if (content.isBoosted() && showBoosted) {
                    timeTv.setText("Sponsored");
                } else {
                    timeTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getEntryAt()));
                }
                Glide.with(context).load(content.getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile);
                textView.setText((content.getPostContent() + "").trim());
            }

            if (content.getTotalLikes() > 0) {
                like_count.setVisibility(VISIBLE);
                like_count.setText(content.getTotalLikes() + "");
            } else {
                like_count.setVisibility(GONE);
            }
            if (content.getTotalComments() > 0) {
                comment_count.setVisibility(VISIBLE);
                comment_count.setText(content.getTotalComments() + " Comments");
            } else {
                comment_count.setVisibility(GONE);
            }
            if (content.getTotalShares() > 0) {
                share_count.setVisibility(VISIBLE);
                share_count.setText(content.getTotalShares() + " Share");
            } else {
                share_count.setVisibility(GONE);
            }

            commentBtn.setOnClickListener(v -> commentDialog.showCommentsDialog(content.getPostId(), size -> {
                comment_count.setText(size + " Comments");
                comment_count.setVisibility(VISIBLE);
                contentList.get(position).setTotalComments(size);
            }));
            comment_count.setOnClickListener(v -> commentDialog.showCommentsDialog(content.getPostId(), size -> {
                comment_count.setText(size + " Comments");
                comment_count.setVisibility(VISIBLE);
                contentList.get(position).setTotalComments(size);
            }));

            if (content.isLiked()) {
                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.colorFwd));
                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.colorFwd));
            } else {
                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_5));
                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
            }

            likeBtn.setOnClickListener(v -> {
                UtilMethods.INSTANCE.triggerLikeApi(context, content.getPostId(), "", new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                        boolean isLiked = (boolean) object;
                        updateLikeState(isLiked, position, likeBtn, like_count);
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
                UserDetailResponse userDetailResponse = UtilMethods.INSTANCE.getUserDetailResponse(tokenManager);
                int accountType = userDetailResponse.isSelfProfile() ? 1 : 2;
                UtilMethods.INSTANCE.addInsight(context, userDetailResponse.getUserId(), content.getPostId(), accountType, 2, new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
            });
            moreBTn.setOnClickListener(view -> showPopupMenu(view, content, position, null));
            shareBtn.setOnClickListener(view -> {
                ShareDialogFragment bottomSheetDialogFragment = ShareDialogFragment.newInstance(content, (int typeId) -> {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).refresh(typeId);
                    } else if (context instanceof ProfileActivity) {
                        ((ProfileActivity) context).refresh();
                    }
                });
                if (context instanceof MainActivity) {
                    bottomSheetDialogFragment.show(((MainActivity) context).getSupportFragmentManager(), "ShareBottomSheetDialog");
                } else if (context instanceof ProfileActivity) {
                    bottomSheetDialogFragment.show(((ProfileActivity) context).getSupportFragmentManager(), "ShareBottomSheetDialog");
                }
            });
            whatsAppBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://api.whatsapp.com/send?text=" + ApplicationConstant.INSTANCE.postUrl + content.getPostId()));
                context.startActivity(intent);
            });
            profile.setOnClickListener(v -> {
                if (!(content.getGroupId() == null) && !content.getGroupId().isEmpty()) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("groupId", content.getGroupId());
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("id", content.getUserId());
                    intent.putExtra("pageId", content.getPageId());
                    context.startActivity(intent);
                }
            });
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public class VideoViewHolder extends RecyclerView.ViewHolder {

        public ImageView playBtn, volBtn, profile, profileParent, thumbnail, moreBTn;
        public PlayerView videoView;
        // ─────────────────────────────────────────────────────────────────
        // [TRACKING - NEW CODE]
        // These three fields track video watch duration for this ViewHolder.
        //
        // currentPostId     — The postId currently bound to this ViewHolder.
        //                     Stored here so onViewRecycled() knows which post
        //                     to send the duration for (content is not accessible
        //                     there).
        //
        // videoPlayStartTime — System.currentTimeMillis() when playback started.
        //                      Reset to 0 when playback pauses/stops.
        //
        // totalWatchedMs    — Accumulated watch time in milliseconds across
        //                     multiple play/pause cycles for the same post.
        //                     e.g., user plays 3s, pauses, plays 2s more = 5000ms
        // ─────────────────────────────────────────────────────────────────
        public String currentPostId;
        public long videoPlayStartTime = 0;
        public long totalWatchedMs = 0;
        MaterialButton likeBtn, commentBtn, whatsAppBtn, shareBtn;
        View ownerView, goalTypeLayout, callNow, bookNow;
        TextView nameTv, nameParentTv, timeParentTv, postParentTxt, timeTv, postTxt, like_count, comment_count, share_count, tvDials, userNameTitle, goalType;
        RelativeLayout container;
        ProgressBar progress;
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(
                2000, 4000, 1000, 2000
        ).build();
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
        private CustomLoader loader;
        private AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (videoView != null && videoView.getPlayer() != null) {
                    thumbnail.setVisibility(VISIBLE);
                    playBtn.setVisibility(VISIBLE);
                    videoView.getPlayer().pause();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (videoView.getPlayer() != null) {
                    videoView.getPlayer().play();
                }
            }
        };

        public VideoViewHolder(View itemView) {
            super(itemView);
            ownerView = itemView.findViewById(R.id.ownerView);
            profileParent = itemView.findViewById(R.id.profileParent);
            nameParentTv = itemView.findViewById(R.id.nameParentTv);
            timeParentTv = itemView.findViewById(R.id.timeParentTv);
            postParentTxt = itemView.findViewById(R.id.postParentTxt);
            progress = itemView.findViewById(R.id.progress);
            videoView = itemView.findViewById(R.id.videoView);
            videoView.setPlayer(new ExoPlayer.Builder(context, renderersFactory).setLoadControl(loadControl).build());
            container = itemView.findViewById(R.id.container);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            nameTv = itemView.findViewById(R.id.nameTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            postTxt = itemView.findViewById(R.id.postTxt);
            profile = itemView.findViewById(R.id.profile);
            like_count = itemView.findViewById(R.id.like_count);
            comment_count = itemView.findViewById(R.id.comment_count);
            share_count = itemView.findViewById(R.id.share_count);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            moreBTn = itemView.findViewById(R.id.moreBTn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            whatsAppBtn = itemView.findViewById(R.id.whatsAppBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            playBtn = itemView.findViewById(R.id.playBtn);
            volBtn = itemView.findViewById(R.id.volBtn);
            goalTypeLayout = itemView.findViewById(R.id.goalTypeLayout);
            tvDials = itemView.findViewById(R.id.tvDials);
            userNameTitle = itemView.findViewById(R.id.userNameTitle);
            goalType = itemView.findViewById(R.id.goalType);
            callNow = itemView.findViewById(R.id.callNow);
            bookNow = itemView.findViewById(R.id.bookNow);
            loader = new CustomLoader(context, android.R.style.Theme_Translucent_NoTitleBar);
        }

        public void bind(ContentResult content, int position) {
            if(Objects.equals(tokenManager.getUserId(), content.getUserId())){
                profile.setEnabled(false);
                profile.setClickable(false);
                profile.setFocusable(false);
            }
            // ─────────────────────────────────────────────────────────────
            // [TRACKING - NEW CODE]
            // Reset tracking state every time a new post is bound to this
            // ViewHolder. This is critical because ViewHolders are recycled —
            // without this reset, old watch time from a previous post would
            // bleed into the new post's tracking.
            // ─────────────────────────────────────────────────────────────
            this.currentPostId = content.getPostId();
            this.totalWatchedMs = 0;
            this.videoPlayStartTime = 0;

            if (content.isBoosted() && showBoosted) {
                if (content.boostedURL() != null && !Objects.equals(content.boostedURL(), "")) {
                    goalTypeLayout.setVisibility(VISIBLE);
                    tvDials.setText("VISIT");
                    userNameTitle.setText(content.getFisrtName() + " " + content.getLastName());
                    goalType.setText("Website");
                    bookNow.setVisibility(VISIBLE);
                    callNow.setVisibility(GONE);
                    bookNow.setOnClickListener(view -> {
                        openInAppWebView(context, content.boostedURL(), loader, content.getPostId(), 1);
                    });
                } else if (content.boostedPhoneNo() != null && !Objects.equals(content.boostedPhoneNo(), "")) {
                    goalTypeLayout.setVisibility(VISIBLE);
                    tvDials.setText("DIALS");
                    userNameTitle.setText(content.getFisrtName() + " " + content.getLastName());
                    goalType.setText("Call");
                    bookNow.setVisibility(GONE);
                    callNow.setVisibility(VISIBLE);
                    callNow.setOnClickListener(view -> {
                        redirectToCall(context, content.boostedPhoneNo(), loader, content.getPostId(), 0);
                    });
                } else {
                    goalTypeLayout.setVisibility(GONE);
                }
            } else {
                goalTypeLayout.setVisibility(GONE);
            }

            if (content.getHeight() > 0 && content.getWidth() > 0) {
                double aspectRatio = content.getWidth() / content.getHeight();
                double showImageHeight = screenWidth / aspectRatio;
                ViewGroup.LayoutParams params = container.getLayoutParams();
                if (showImageHeight > videoMaxHeight) {
                    params.height = videoMaxHeight;
                } else {
                    params.height = (int) showImageHeight;
                }
                container.setLayoutParams(params);
                GlideLoadHelper.loadWithProgress(context, content.getPostContent(), thumbnail, progress);
            } else {
                Glide.with(context).asBitmap().load(content.getPostContent())
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .apply(requestOptionsVideo)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                double imageHeight = resource.getHeight();
                                double imageWidth = resource.getWidth();
                                double aspectRatio = imageWidth / imageHeight;
                                double showImageHeight = screenWidth / aspectRatio;
                                ViewGroup.LayoutParams params = container.getLayoutParams();
                                if (showImageHeight > videoMaxHeight) {
                                    params.height = videoMaxHeight;
                                } else {
                                    params.height = (int) showImageHeight;
                                }
                                container.setLayoutParams(params);
                                return false;
                            }
                        }).into(thumbnail);
            }

            String savePath = VideoUtils.getString(context, content.getPostContent());
            if (savePath != null && new File(savePath).exists()) {
                Uri uri = Uri.parse(savePath);
                MediaItem mediaItem = MediaItem.fromUri(uri);
                videoView.getPlayer().setMediaItem(mediaItem);
            } else {
                Uri uri = Uri.parse(content.getPostContent());
                MediaItem mediaItem = MediaItem.fromUri(uri);
                videoView.getPlayer().setMediaItem(mediaItem);
                Intent serviceIntent = new Intent(context, DownloadManagerService.class);
                context.startService(serviceIntent);
                new VideoUtils().startDownloadInBackground(context, content.getPostContent(), context.getExternalCacheDir() + "/MyVideo/");
            }

            videoView.getPlayer().prepare();
            videoView.getPlayer().setPlayWhenReady(false);
            videoView.getPlayer().addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (isMute && videoView.getPlayer().getVolume() != 0) {
                        videoView.getPlayer().setVolume(0f);
                        volBtn.setImageResource(R.drawable.ic_mute);
                    } else if (!isMute && videoView.getPlayer().getVolume() == 0) {
                        videoView.getPlayer().setVolume(1f);
                        volBtn.setImageResource(R.drawable.ic_unmute);
                    }
                    if (videoView.getPlayer().getVolume() != 0) {
                        if (isPlaying) {
                            am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                        } else {
                            am.abandonAudioFocus(focusChangeListener);
                        }
                    }

                    // ─────────────────────────────────────────────────────
                    // [TRACKING - NEW CODE]
                    // Measure exact watch time using play/pause events.
                    //
                    // isPlaying = true  → user started watching, record start time
                    // isPlaying = false → user paused/stopped, calculate this
                    //                     segment's duration and add to total
                    //
                    // This handles multiple play-pause cycles correctly:
                    // e.g., play 5s → pause → play 3s → pause → total = 8s
                    // ─────────────────────────────────────────────────────
                    if (isPlaying) {
                        videoPlayStartTime = System.currentTimeMillis();
                    } else {
                        if (videoPlayStartTime > 0) {
                            totalWatchedMs += System.currentTimeMillis() - videoPlayStartTime;
                            videoPlayStartTime = 0;
                        }
                    }
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == ExoPlayer.STATE_ENDED) {

                        // ─────────────────────────────────────────────────
                        // [TRACKING - NEW CODE]
                        // Video finished naturally — flush any remaining
                        // play segment, then send the total duration to API.
                        // Reset totalWatchedMs after sending so the next
                        // loop-replay of the same video starts fresh.
                        // ─────────────────────────────────────────────────
                        if (videoPlayStartTime > 0) {
                            totalWatchedMs += System.currentTimeMillis() - videoPlayStartTime;
                            videoPlayStartTime = 0;
                        }
                        trackPostView(content.getPostId(), Math.toIntExact(totalWatchedMs / 1000));
                        totalWatchedMs = 0; // Reset for next loop replay

                        mRecyclerView.playingTimeMap.put(videoView, 0L);
                        videoView.getPlayer().seekTo(0);
                        mRecyclerView.playingPauseMap.put(videoView, false);
                        videoView.getPlayer().play();
                        mRecyclerView.playingVideoView = videoView;
                        mRecyclerView.thumbnail = thumbnail;
                        mRecyclerView.playBtn = playBtn;

                    } else if (playbackState == ExoPlayer.STATE_BUFFERING) {
                        progress.setVisibility(VISIBLE);
                    } else if (playbackState == ExoPlayer.STATE_READY) {
                        if (mRecyclerView.isScreenPaused() || mRecyclerView.playingPauseMap.containsKey(videoView) && mRecyclerView.playingPauseMap.get(videoView) == true) {
                            if (videoView.getPlayer().getCurrentPosition() > 0) {
                                mRecyclerView.playingTimeMap.put(videoView, videoView.getPlayer().getCurrentPosition());
                            }
                            videoView.getPlayer().pause();
                            thumbnail.setVisibility(VISIBLE);
                            playBtn.setVisibility(VISIBLE);
                        }
                        progress.setVisibility(GONE);
                    } else {
                        progress.setVisibility(GONE);
                    }
                }
            });

            if (isMute) {
                videoView.getPlayer().setVolume(0f);
                volBtn.setImageResource(R.drawable.ic_mute);
            } else {
                videoView.getPlayer().setVolume(1f);
                volBtn.setImageResource(R.drawable.ic_unmute);
            }

            container.setOnClickListener(view -> {
                VideoViewActivity.callBack = new CountChangeCallBack() {
                    @Override
                    public void onRefresh(int typeId) {
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).refresh(typeId);
                        } else if (context instanceof ProfileActivity) {
                            ((ProfileActivity) context).refresh();
                        }
                    }

                    @Override
                    public void onChangeCallBack(String editPostId, Boolean isLiked, int commentCount, int likeCount, int shareCount, int changePosition, int deletePosition) {
                        if (editPostId != null && !editPostId.isEmpty()) {
                            if (clickCallBack != null) {
                                clickCallBack.onClickCreatePost(content.getPostId());
                            }
                        }
                        if (deletePosition != -1) {
                            if (videoView != null) {
                                mRecyclerView.deleteVideo(videoView);
                            }
                            if (clickCallBack != null) {
                                clickCallBack.onDelete(deletePosition);
                            }
                        }
                        if (isLiked != null) {
                            contentList.get(changePosition).setLiked(isLiked);
                            if (isLiked) {
                                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.colorFwd));
                                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.colorFwd));
                            } else {
                                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_5));
                                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
                            }
                        }
                        if (likeCount != -1) {
                            contentList.get(changePosition).setTotalLikes(likeCount);
                            if (likeCount > 0) {
                                like_count.setVisibility(VISIBLE);
                                like_count.setText(likeCount + "");
                            } else {
                                like_count.setVisibility(GONE);
                            }
                        }
                        if (commentCount != -1) {
                            contentList.get(changePosition).setTotalComments(commentCount);
                            if (commentCount > 0) {
                                comment_count.setVisibility(VISIBLE);
                                comment_count.setText(commentCount + " Comments");
                            } else {
                                comment_count.setVisibility(GONE);
                            }
                        }
                        if (shareCount != -1) {
                            contentList.get(changePosition).setTotalShares(shareCount);
                            if (shareCount > 0) {
                                share_count.setVisibility(VISIBLE);
                                share_count.setText(shareCount + " Share");
                            } else {
                                share_count.setVisibility(GONE);
                            }
                        }
                    }
                };
                UserDetailResponse userDetailResponse = UtilMethods.INSTANCE.getUserDetailResponse(tokenManager);
                int accountType = userDetailResponse.isSelfProfile() ? 1 : 2;
                UtilMethods.INSTANCE.addInsight(context, userDetailResponse.getUserId(), content.getPostId(), accountType, 3, new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
                if (onDetailOpened != null) onDetailOpened.run();
                context.startActivity(new Intent(context, VideoViewActivity.class)
                        .putExtra("Position", position)
                        .putExtra("VideoData", content));
            });

            volBtn.setOnClickListener(v -> {
                if (isMute) {
                    if (videoView.getPlayer() != null) {
                        videoView.getPlayer().setVolume(1f);
                        volBtn.setImageResource(R.drawable.ic_unmute);
                        if (videoView.getPlayer().isPlaying()) {
                            am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                        }
                    }
                } else {
                    if (videoView.getPlayer() != null) {
                        videoView.getPlayer().setVolume(0f);
                        volBtn.setImageResource(R.drawable.ic_mute);
                        am.abandonAudioFocus(focusChangeListener);
                    }
                }
                isMute = !isMute;
            });

            if (content.getParsedSharedData() != null) {
                ownerView.setVisibility(VISIBLE);
                nameTv.setText(content.getParsedSharedData().getFisrtName() + " " + content.getParsedSharedData().getLastName());
                if (content.isBoosted() && showBoosted) {
                    timeTv.setText("Sponsored");
                } else {
                    timeTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getParsedSharedData().getEntryAt()));
                }
                if (content.getParsedSharedData().getCaption() != null && !content.getParsedSharedData().getCaption().trim().isEmpty()) {
                    postTxt.setText(content.getParsedSharedData().getCaption().trim());
                    postTxt.setVisibility(VISIBLE);
                } else {
                    postTxt.setVisibility(GONE);
                }
                Glide.with(context).load(content.getParsedSharedData().getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile);
                nameParentTv.setText(content.getFisrtName() + " " + content.getLastName());
                timeParentTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getEntryAt()));
                if (content.getCaption() != null && !content.getCaption().trim().isEmpty()) {
                    postParentTxt.setText(content.getCaption().trim());
                    postParentTxt.setVisibility(VISIBLE);
                } else {
                    postParentTxt.setVisibility(GONE);
                }
                Glide.with(context).load(content.getProfilePictureUrl()).apply(requestOptionsUserImage).into(profileParent);
            } else {
                ownerView.setVisibility(GONE);
                nameTv.setText(content.getFisrtName() + " " + content.getLastName());
                if (content.isBoosted() && showBoosted) {
                    timeTv.setText("Sponsored");
                } else {
                    timeTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getEntryAt()));
                }
                if (content.getCaption() != null && !content.getCaption().trim().isEmpty()) {
                    postTxt.setText(content.getCaption().trim());
                    postTxt.setVisibility(VISIBLE);
                } else {
                    postTxt.setVisibility(GONE);
                }
                Glide.with(context).load(content.getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile);
            }

            if (content.getTotalLikes() > 0) {
                like_count.setVisibility(VISIBLE);
                like_count.setText(content.getTotalLikes() + "");
            } else {
                like_count.setVisibility(GONE);
            }
            if (content.getTotalComments() > 0) {
                comment_count.setVisibility(VISIBLE);
                comment_count.setText(content.getTotalComments() + " Comments");
            } else {
                comment_count.setVisibility(GONE);
            }
            if (content.getTotalShares() > 0) {
                share_count.setVisibility(VISIBLE);
                share_count.setText(content.getTotalShares() + " Share");
            } else {
                share_count.setVisibility(GONE);
            }

            commentBtn.setOnClickListener(v -> commentDialog.showCommentsDialog(content.getPostId(), size -> {
                comment_count.setText(size + " Comments");
                comment_count.setVisibility(VISIBLE);
                contentList.get(position).setTotalComments(size);
            }));
            comment_count.setOnClickListener(v -> commentDialog.showCommentsDialog(content.getPostId(), size -> {
                comment_count.setText(size + " Comments");
                comment_count.setVisibility(VISIBLE);
                contentList.get(position).setTotalComments(size);
            }));

            if (content.isLiked()) {
                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.colorFwd));
                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.colorFwd));
            } else {
                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_5));
                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
            }

            likeBtn.setOnClickListener(v -> {
                UtilMethods.INSTANCE.triggerLikeApi(context, content.getPostId(), "", new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                        boolean isLiked = (boolean) object;
                        updateLikeState(isLiked, position, likeBtn, like_count);
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
                UserDetailResponse userDetailResponse = UtilMethods.INSTANCE.getUserDetailResponse(tokenManager);
                int accountType = userDetailResponse.isSelfProfile() ? 1 : 2;
                UtilMethods.INSTANCE.addInsight(context, userDetailResponse.getUserId(), content.getPostId(), accountType, 2, new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
            });
            moreBTn.setOnClickListener(view -> showPopupMenu(view, content, position, videoView));
            shareBtn.setOnClickListener(view -> {
                ShareDialogFragment bottomSheetDialogFragment = ShareDialogFragment.newInstance(content, (int typeId) -> {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).refresh(typeId);
                    } else if (context instanceof ProfileActivity) {
                        ((ProfileActivity) context).refresh();
                    }
                });
                if (context instanceof MainActivity) {
                    bottomSheetDialogFragment.show(((MainActivity) context).getSupportFragmentManager(), "ShareBottomSheetDialog");
                } else if (context instanceof ProfileActivity) {
                    bottomSheetDialogFragment.show(((ProfileActivity) context).getSupportFragmentManager(), "ShareBottomSheetDialog");
                }
            });
            whatsAppBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://api.whatsapp.com/send?text=" + ApplicationConstant.INSTANCE.postUrl + content.getPostId()));
                context.startActivity(intent);
            });
            profile.setOnClickListener(v -> {
                if (!(content.getGroupId() == null) && !content.getGroupId().isEmpty()) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("groupId", content.getGroupId());
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("id", content.getUserId());
                    intent.putExtra("pageId", content.getPageId());
                    context.startActivity(intent);
                }
            });
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, moreBTn;
        ProgressBar imageLoader;
        MaterialButton likeBtn, commentBtn, whatsAppBtn, shareBtn;
        TextView nameTv, nameParentTv, timeParentTv, postParentTxt, timeTv, postTxt, like_count, comment_count, share_count, tvDials, userNameTitle, goalType;
        ImageView profile, profileParent;
        View ownerView, goalTypeLayout, callNow, bookNow;
        private CustomLoader loader;

        public ImageViewHolder(View itemView) {
            super(itemView);
            ownerView = itemView.findViewById(R.id.ownerView);
            nameParentTv = itemView.findViewById(R.id.nameParentTv);
            timeParentTv = itemView.findViewById(R.id.timeParentTv);
            postParentTxt = itemView.findViewById(R.id.postParentTxt);
            profileParent = itemView.findViewById(R.id.profileParent);
            moreBTn = itemView.findViewById(R.id.moreBTn);
            nameTv = itemView.findViewById(R.id.nameTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            postTxt = itemView.findViewById(R.id.postTxt);
            profile = itemView.findViewById(R.id.profile);
            imageView = itemView.findViewById(R.id.container);
            imageLoader = itemView.findViewById(R.id.imageLoader);
            like_count = itemView.findViewById(R.id.like_count);
            comment_count = itemView.findViewById(R.id.comment_count);
            share_count = itemView.findViewById(R.id.share_count);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            whatsAppBtn = itemView.findViewById(R.id.whatsAppBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            goalTypeLayout = itemView.findViewById(R.id.goalTypeLayout);
            tvDials = itemView.findViewById(R.id.tvDials);
            userNameTitle = itemView.findViewById(R.id.userNameTitle);
            goalType = itemView.findViewById(R.id.goalType);
            callNow = itemView.findViewById(R.id.callNow);
            bookNow = itemView.findViewById(R.id.bookNow);
            loader = new CustomLoader(context, android.R.style.Theme_Translucent_NoTitleBar);
        }

        public void bind(ContentResult content, int position) {
            if(Objects.equals(tokenManager.getUserId(), content.getUserId())){
                profile.setEnabled(false);
                profile.setClickable(false);
                profile.setFocusable(false);
            }
            if (content.isBoosted() && showBoosted) {
                if (content.boostedURL() != null && !Objects.equals(content.boostedURL(), "")) {
                    goalTypeLayout.setVisibility(VISIBLE);
                    tvDials.setText("VISIT");
                    userNameTitle.setText(content.getFisrtName() + " " + content.getLastName());
                    goalType.setText("Website");
                    bookNow.setVisibility(VISIBLE);
                    callNow.setVisibility(GONE);
                    bookNow.setOnClickListener(view -> {
                        openInAppWebView(context, content.boostedURL(), loader, content.getPostId(), 1);
                    });
                } else if (content.boostedPhoneNo() != null && !Objects.equals(content.boostedPhoneNo(), "")) {
                    goalTypeLayout.setVisibility(VISIBLE);
                    tvDials.setText("DIALS");
                    userNameTitle.setText(content.getFisrtName() + " " + content.getLastName());
                    goalType.setText("Call");
                    bookNow.setVisibility(GONE);
                    callNow.setVisibility(VISIBLE);
                    callNow.setOnClickListener(view -> {
                        redirectToCall(context, content.boostedPhoneNo(), loader, content.getPostId(), 0);
                    });
                } else {
                    goalTypeLayout.setVisibility(GONE);
                }
            } else {
                goalTypeLayout.setVisibility(GONE);
            }

            if (content.getHeight() > 0 && content.getWidth() > 0) {
                double aspectRatio = content.getWidth() / content.getHeight();
                double showImageHeight = screenWidth / aspectRatio;
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                if (showImageHeight > videoMaxHeight) {
                    params.height = videoMaxHeight;
                } else {
                    params.height = (int) showImageHeight;
                }
                imageView.setLayoutParams(params);
                GlideLoadHelper.loadWithProgress(context, content.getPostContent(), imageView, imageLoader);
            } else {
                Glide.with(context).asBitmap().load(content.getPostContent())
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .apply(requestOptionsVideo)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                double imageHeight = resource.getHeight();
                                double imageWidth = resource.getWidth();
                                double aspectRatio = imageWidth / imageHeight;
                                double showImageHeight = screenWidth / aspectRatio;
                                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                                if (showImageHeight > videoMaxHeight) {
                                    params.height = videoMaxHeight;
                                } else {
                                    params.height = (int) showImageHeight;
                                }
                                imageView.setLayoutParams(params);
                                return false;
                            }
                        }).into(imageView);
            }

            if (content.getParsedSharedData() != null) {
                ownerView.setVisibility(VISIBLE);
                nameTv.setText(content.getParsedSharedData().getFisrtName() + " " + content.getParsedSharedData().getLastName());
                if (content.isBoosted() && showBoosted) {
                    timeTv.setText("Sponsored");
                } else {
                    timeTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getParsedSharedData().getEntryAt()));
                }
                if (content.getParsedSharedData().getCaption() != null && !content.getParsedSharedData().getCaption().trim().isEmpty()) {
                    postTxt.setText(content.getParsedSharedData().getCaption().trim());
                    postTxt.setVisibility(VISIBLE);
                } else {
                    postTxt.setVisibility(GONE);
                }
                Glide.with(context).load(content.getParsedSharedData().getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile);
                nameParentTv.setText(content.getFisrtName() + " " + content.getLastName());
                timeParentTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getEntryAt()));
                if (content.getCaption() != null && !content.getCaption().trim().isEmpty()) {
                    postParentTxt.setText(content.getCaption().trim());
                    postParentTxt.setVisibility(VISIBLE);
                } else {
                    postParentTxt.setVisibility(GONE);
                }
                Glide.with(context).load(content.getProfilePictureUrl()).apply(requestOptionsUserImage).into(profileParent);
            } else {
                ownerView.setVisibility(GONE);
                nameTv.setText(content.getFisrtName() + " " + content.getLastName());
                if (content.isBoosted() && showBoosted) {
                    timeTv.setText("Sponsored");
                } else {
                    timeTv.setText(UtilMethods.INSTANCE.covertTimeToText(content.getEntryAt()));
                }
                if (content.getCaption() != null && !content.getCaption().trim().isEmpty()) {
                    postTxt.setText(content.getCaption().trim());
                    postTxt.setVisibility(VISIBLE);
                } else {
                    postTxt.setVisibility(GONE);
                }
                Glide.with(context).load(content.getProfilePictureUrl()).apply(requestOptionsUserImage).into(profile);
            }

            if (content.getTotalLikes() > 0) {
                like_count.setVisibility(VISIBLE);
                like_count.setText(content.getTotalLikes() + "");
            } else {
                like_count.setVisibility(GONE);
            }
            if (content.getTotalComments() > 0) {
                comment_count.setVisibility(VISIBLE);
                comment_count.setText(content.getTotalComments() + " Comments");
            } else {
                comment_count.setVisibility(GONE);
            }
            if (content.getTotalShares() > 0) {
                share_count.setVisibility(VISIBLE);
                share_count.setText(content.getTotalShares() + " Share");
            } else {
                share_count.setVisibility(GONE);
            }

            if (content.isLiked()) {
                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.colorFwd));
                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.colorFwd));
            } else {
                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_5));
                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
            }

            commentBtn.setOnClickListener(v -> commentDialog.showCommentsDialog(content.getPostId(), size -> {
                comment_count.setText(size + " Comments");
                comment_count.setVisibility(VISIBLE);
                contentList.get(position).setTotalComments(size);
            }));
            comment_count.setOnClickListener(v -> commentDialog.showCommentsDialog(content.getPostId(), size -> {
                comment_count.setText(size + " Comments");
                comment_count.setVisibility(VISIBLE);
                contentList.get(position).setTotalComments(size);
            }));

            imageView.setOnClickListener(view -> {
                ImageZoomViewActivity.callBack = new CountChangeCallBack() {
                    @Override
                    public void onRefresh(int typeId) {
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).refresh(typeId);
                        } else if (context instanceof ProfileActivity) {
                            ((ProfileActivity) context).refresh();
                        }
                    }

                    @Override
                    public void onChangeCallBack(String editPostId, Boolean isLiked, int commentCount, int likeCount, int shareCount, int changePosition, int deletePosition) {
                        if (editPostId != null && !editPostId.isEmpty()) {
                            if (clickCallBack != null) {
                                clickCallBack.onClickCreatePost(content.getPostId());
                            }
                        }
                        if (deletePosition != -1) {
                            if (clickCallBack != null) {
                                clickCallBack.onDelete(deletePosition);
                            }
                        }
                        if (isLiked != null) {
                            contentList.get(changePosition).setLiked(isLiked);
                            if (isLiked) {
                                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.colorFwd));
                                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.colorFwd));
                            } else {
                                likeBtn.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_5));
                                likeBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
                            }
                        }
                        if (likeCount != -1) {
                            contentList.get(changePosition).setTotalLikes(likeCount);
                            if (likeCount > 0) {
                                like_count.setVisibility(VISIBLE);
                                like_count.setText(likeCount + "");
                            } else {
                                like_count.setVisibility(GONE);
                            }
                        }
                        if (commentCount != -1) {
                            contentList.get(changePosition).setTotalComments(commentCount);
                            if (commentCount > 0) {
                                comment_count.setVisibility(VISIBLE);
                                comment_count.setText(commentCount + " Comments");
                            } else {
                                comment_count.setVisibility(GONE);
                            }
                        }
                        if (shareCount != -1) {
                            contentList.get(changePosition).setTotalShares(shareCount);
                            if (shareCount > 0) {
                                share_count.setVisibility(VISIBLE);
                                share_count.setText(shareCount + " Share");
                            } else {
                                share_count.setVisibility(GONE);
                            }
                        }
                    }
                };
                UserDetailResponse userDetailResponse = UtilMethods.INSTANCE.getUserDetailResponse(tokenManager);
                int accountType = userDetailResponse.isSelfProfile() ? 1 : 2;
                UtilMethods.INSTANCE.addInsight(context, userDetailResponse.getUserId(), content.getPostId(), accountType, 3, new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
                if (onDetailOpened != null) onDetailOpened.run();
                context.startActivity(new Intent(context, ImageZoomViewActivity.class)
                        .putExtra("Position", position)
                        .putExtra("ImageData", content));
            });

            likeBtn.setOnClickListener(v -> {
                UtilMethods.INSTANCE.triggerLikeApi(context, content.getPostId(), "", new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                        boolean isLiked = (boolean) object;
                        updateLikeState(isLiked, position, likeBtn, like_count);
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
                UserDetailResponse userDetailResponse = UtilMethods.INSTANCE.getUserDetailResponse(tokenManager);
                int accountType = userDetailResponse.isSelfProfile() ? 1 : 2;
                UtilMethods.INSTANCE.addInsight(context, userDetailResponse.getUserId(), content.getPostId(), accountType, 2, new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object object) {
                    }

                    @Override
                    public void onError(String msg) {
                    }
                });
            });

            moreBTn.setOnClickListener(view -> showPopupMenu(view, content, position, null));
            shareBtn.setOnClickListener(view -> {
                ShareDialogFragment bottomSheetDialogFragment = ShareDialogFragment.newInstance(content, (int typeId) -> {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).refresh(typeId);
                    } else if (context instanceof ProfileActivity) {
                        ((ProfileActivity) context).refresh();
                    }
                });
                if (context instanceof MainActivity) {
                    bottomSheetDialogFragment.show(((MainActivity) context).getSupportFragmentManager(), "ShareBottomSheetDialog");
                } else if (context instanceof ProfileActivity) {
                    bottomSheetDialogFragment.show(((ProfileActivity) context).getSupportFragmentManager(), "ShareBottomSheetDialog");
                }
            });
            whatsAppBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://api.whatsapp.com/send?text=" + ApplicationConstant.INSTANCE.postUrl + content.getPostId()));
                context.startActivity(intent);
            });
            profile.setOnClickListener(v -> {
                if (!(content.getGroupId() == null) && !content.getGroupId().isEmpty()) {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("groupId", content.getGroupId());
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("id", content.getUserId());
                    intent.putExtra("pageId", content.getPageId());
                    context.startActivity(intent);
                }
            });
        }
    }
}