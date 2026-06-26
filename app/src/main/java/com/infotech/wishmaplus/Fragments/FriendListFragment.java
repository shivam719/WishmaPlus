package com.infotech.wishmaplus.Fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.infotech.wishmaplus.Activity.FriendRequest;
import com.infotech.wishmaplus.Activity.ProfileActivity;
import com.infotech.wishmaplus.Activity.SentRequests;
import com.infotech.wishmaplus.Activity.YourFriends;
import com.infotech.wishmaplus.Adapter.FriendSuggestionAdapter;
import com.infotech.wishmaplus.Adapter.FriendSuggestionItem;
import com.infotech.wishmaplus.Adapter.FriendSuggestionResponse;
import com.infotech.wishmaplus.Api.Response.BasicResponse;
import com.infotech.wishmaplus.Api.Response.UserDetailResponse;
import com.infotech.wishmaplus.Api.Response.UserListFriends;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.PreferencesManager;
import com.infotech.wishmaplus.Utils.UtilMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class FriendListFragment extends Fragment {
    private final int pageSize = 20;
    public PreferencesManager tokenManager;
    View noDataLayout,noDataLayout2;
    RecyclerView recyclerView;
    List<UserListFriends> list = new ArrayList<>();
    //    FriendListAdapter adapter;
    FriendSuggestionAdapter adapter;
    FriendSuggestionResponse friendSuggestionResponse = new FriendSuggestionResponse();
    UserDetailResponse userDetailResponse;
    BottomSheetDialog bottomSheetDialog;
    View notificationDot;
    ImageButton search_button;
    SearchView searchView;
    TextView user_title;
    HorizontalScrollView horizontalScrollView;
    SwipeRefreshLayout pullToRefresh;
    View line1;
    ImageView closeButton;
    ActivityResultLauncher<Intent> profileActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
            });
    private CustomLoader loader;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private String currentQuery = "";

    private  RecyclerView recyclerView2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_list, container, false);
        pullToRefresh = view.findViewById(R.id.swipeRefreshLayout);
        pullToRefresh.setOnRefreshListener(() -> {
            hitApi();
            getFriendSuggestionList(true);
            pullToRefresh.setRefreshing(false);
        });
        tokenManager = new PreferencesManager(requireContext(), 1);
        userDetailResponse = UtilMethods.INSTANCE.getUserDetailResponse(tokenManager);
        user_title = view.findViewById(R.id.user_title);
        search_button = view.findViewById(R.id.search_button);
        searchView = view.findViewById(R.id.searchView);
        closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);

        closeButton.setVisibility(View.VISIBLE);
        closeButton.setImageResource(R.drawable.cancel);
        horizontalScrollView = view.findViewById(R.id.horizontalTabs);
        line1 = view.findViewById(R.id.line1);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView2 = view.findViewById(R.id.recyclerView2);
        noDataLayout = view.findViewById(R.id.noDataLayout);
        noDataLayout2 = view.findViewById(R.id.noDataLayout2);
        notificationDot = view.findViewById(R.id.notification_dot);
        loader = new CustomLoader(requireContext(), android.R.style.Theme_Translucent_NoTitleBar);

        view.findViewById(R.id.content).setOnClickListener(view1 -> startActivity(new Intent(requireActivity(), YourFriends.class)));
        view.findViewById(R.id.sentRequest).setOnClickListener(view1 -> startActivity(new Intent(requireActivity(), SentRequests.class)));
        view.findViewById(R.id.insights).setOnClickListener(view1 -> startActivity(new Intent(requireActivity(), FriendRequest.class)));
        getFriendSuggestionList(false);
        hitApi();

        search_button.setOnClickListener(clickView -> {
            user_title.setVisibility(GONE);
            search_button.setVisibility(GONE);
            horizontalScrollView.setVisibility(GONE);
            line1.setVisibility(GONE);
            searchView.setVisibility(VISIBLE);
            recyclerView2.setVisibility(VISIBLE);
            pullToRefresh.setVisibility(GONE);
            searchView.post(() -> {
                if (closeButton != null) {
                    closeButton.setVisibility(View.VISIBLE);
                    closeButton.setImageResource(R.drawable.cancel);
                }
            });

            searchView.setIconified(false);
            searchView.requestFocus();
        });
        closeButton.setOnClickListener(clickView -> {
            searchView.setQuery("", false);
            searchView.clearFocus();
            currentQuery = "";
            currentPage = 1;
            isLastPage = false;
            user_title.setVisibility(VISIBLE);
            search_button.setVisibility(VISIBLE);
            horizontalScrollView.setVisibility(VISIBLE);
            line1.setVisibility(VISIBLE);
            searchView.setVisibility(GONE);
            recyclerView2.setVisibility(GONE);
            pullToRefresh.setVisibility(VISIBLE);
            recyclerView2.setAdapter(null);
            getFriendSuggestionList(true);
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query.trim();
                currentPage = 1;
                isLastPage = false;
                searchFriends(currentQuery, currentPage);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ImageView closeBtn = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
                if (closeBtn != null) {
                    closeBtn.setVisibility(View.VISIBLE);
                    closeBtn.setImageResource(R.drawable.cancel);
                }

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                String query = newText.trim();
                if (query.isEmpty()) {
                    currentQuery = "";
                    currentPage = 1;
                    isLastPage = false;
                    recyclerView2.setAdapter(null);
                    noDataLayout2.setVisibility(GONE);
                } else {
                    searchRunnable = () -> {
                        currentQuery = query;
                        currentPage = 1;
                        isLastPage = false;
                        searchFriends(currentQuery, currentPage);
                    };
                    searchHandler.postDelayed(searchRunnable, 400);
                }
                return true;
            }
        });
        return view;
    }

    private void searchFriends(String query, int pageNumber) {

        if (isLoading) return;

        isLoading = true;
        UtilMethods.INSTANCE.searchFriends(query, pageNumber, pageSize, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                isLoading = false;
                FriendSuggestionResponse searchResponse = (FriendSuggestionResponse) object;
                if (searchResponse.getStatusCode() == 1) {
                    friendSuggestionResponse = searchResponse;
                    adapter = new FriendSuggestionAdapter(
                            requireContext(),
                            friendSuggestionResponse.getResult(),
                            new FriendSuggestionAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(FriendSuggestionItem user, int pos) {

                                }

                                @Override
                                public void onMoreClicked(View anchor, FriendSuggestionItem user, int pos) {
                                    addFriend(user.getUserId());
                                }

                                @Override
                                public void onProfileClick(FriendSuggestionItem user, int position) {
                                    profileActivityResultLauncher.launch(new Intent(requireContext(), ProfileActivity.class)
                                            .putExtra("userData", userDetailResponse)
                                            .putExtra("id", user.getUserId()));
                                }

                            }
                    );

                    recyclerView2.setLayoutManager(new LinearLayoutManager(requireContext()));
                    recyclerView2.setAdapter(adapter);
                    recyclerView2.addItemDecoration(
                            new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                    );
                    updateEmptyView2();
                } else {
                    Toast.makeText(requireContext(), "No results found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String msg) {
                UtilMethods.INSTANCE.Error(requireActivity(), msg);
            }
        });
    }
    private void getFriendSuggestionList(boolean isRefresh) {
        loader.show();

        UtilMethods.INSTANCE.getFriendSuggestionList(new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                if (loader != null && loader.isShowing()) loader.dismiss();

                friendSuggestionResponse = (FriendSuggestionResponse) object;

                if (friendSuggestionResponse.getStatusCode() == 1) {
                    if (adapter == null) {
                        // Create adapter only once
                        adapter = new FriendSuggestionAdapter(
                                requireContext(),
                                friendSuggestionResponse.getResult(),
                                new FriendSuggestionAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(FriendSuggestionItem user, int pos) {}

                                    @Override
                                    public void onMoreClicked(View anchor, FriendSuggestionItem user, int pos) {
                                        addFriend(user.getUserId());
                                    }

                                    @Override
                                    public void onProfileClick(FriendSuggestionItem user, int position) {
                                        profileActivityResultLauncher.launch(
                                                new Intent(requireContext(), ProfileActivity.class)
                                                        .putExtra("userData", userDetailResponse)
                                                        .putExtra("id", user.getUserId())
                                        );
                                    }
                                }
                        );
                        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                        recyclerView.setAdapter(adapter);
                        recyclerView.addItemDecoration(
                                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                        );
                    } else {
                        // Update existing adapter data
                        adapter.updateList(friendSuggestionResponse.getResult());
                    }
                    updateEmptyView();
                }
            }

            @Override
            public void onError(String msg) {
                if (loader != null && loader.isShowing()) loader.dismiss();
            }
        });
    }

    private void getUserDetail() {
        UtilMethods.INSTANCE.userDetail(requireActivity(), "0", "", loader, tokenManager, object -> {
        });
    }

    private void addFriend(String userId) {
        loader.show();
        UtilMethods.INSTANCE.createRequest(requireActivity(), userId, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                if (loader != null && loader.isShowing()) loader.dismiss();
                BasicResponse basicResponse = (BasicResponse) object;
                if (basicResponse.getStatusCode() == 1) {
                    getFriendSuggestionList(true);

                } else {
                    UtilMethods.INSTANCE.Error(requireActivity(), basicResponse.getResponseText());
                }

            }

            @Override
            public void onError(String msg) {
                if (loader != null && loader.isShowing()) loader.dismiss();

            }
        });


    }

    private void updateEmptyView() {
        if (friendSuggestionResponse.getResult().isEmpty()) {
            recyclerView.setVisibility(GONE);
            noDataLayout.setVisibility(VISIBLE);
        } else {
            recyclerView.setVisibility(VISIBLE);
            noDataLayout.setVisibility(GONE);
        }
    }

    private void updateEmptyView2() {
        if (friendSuggestionResponse.getResult().isEmpty()) {
            recyclerView2.setVisibility(GONE);
            noDataLayout2.setVisibility(VISIBLE);
        } else {
            recyclerView2.setVisibility(VISIBLE);
            noDataLayout2.setVisibility(GONE);
        }
    }

    @Override
    public void onResume() {
        getUserDetail();
        hitApi();
        super.onResume();
    }

    public void openBottomSheet(Activity context) {

        if (bottomSheetDialog != null && bottomSheetDialog.isShowing())
            return;

        bottomSheetDialog = new BottomSheetDialog(context, R.style.DialogStyle);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_profile_options, null);

        View unfollow = sheetView.findViewById(R.id.unfollow);
        View block = sheetView.findViewById(R.id.block);
        View unfriend = sheetView.findViewById(R.id.unfriend);

        unfollow.setOnClickListener(v -> bottomSheetDialog.dismiss());
        block.setOnClickListener(v -> bottomSheetDialog.dismiss());
        unfriend.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        BottomSheetBehavior.from(
                        Objects.requireNonNull(bottomSheetDialog.findViewById(
                                com.google.android.material.R.id.design_bottom_sheet)))
                .setState(BottomSheetBehavior.STATE_EXPANDED);

        bottomSheetDialog.show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        hitApi();
    }

    private void callAddFriendApi(UserListFriends user, int position) {
        if (getActivity() == null) return;
        UtilMethods.INSTANCE.createRequest(getActivity(), user.getUserId(), new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                Toast.makeText(getContext(), "Friend Added!", Toast.LENGTH_SHORT).show();
                if (adapter != null) {
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callRemoveFriendApi(UserListFriends user, int position) {
        if (getActivity() == null) return;
        UtilMethods.INSTANCE.removeRequest(getActivity(), user.getUserId(), new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object object) {
                if (position >= 0 && position < list.size()) {
                    list.remove(position);
                    if (adapter != null) {
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, list.size());
//                        updateEmptyView();
                    }
                }
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hitApi() {
        if (!isAdded()) return;
        UtilMethods.INSTANCE.getFriendRequest(requireActivity(), new UtilMethods.ApiCallBackMulti() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(Object object) {
                list.clear();
                if (object instanceof List) {
                    List<UserListFriends> apiList = (List<UserListFriends>) object;
                    list.addAll(apiList);
                    if (list.size() > 0) {
                        notificationDot.setVisibility(VISIBLE);
                    } else {
                        notificationDot.setVisibility(GONE);
                    }
                }
            }

            @Override
            public void onError(String msg) {

            }
        });
    }
}
