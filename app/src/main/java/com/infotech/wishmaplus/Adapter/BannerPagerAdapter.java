package com.infotech.wishmaplus.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.BannerItem;
import com.infotech.wishmaplus.Utils.CustomLoader;

import java.util.List;

public class BannerPagerAdapter extends RecyclerView.Adapter<BannerPagerAdapter.BannerViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(BannerItem banner);
    }

    private final Context context;
    private final List<BannerItem> bannerList;
    private final OnBannerClickListener clickListener;
    private final CustomLoader loader;

    public BannerPagerAdapter(Context context, List<BannerItem> bannerList,
                              OnBannerClickListener clickListener, CustomLoader loader) {
        this.context = context;
        this.bannerList = bannerList;
        this.clickListener = clickListener;
        this.loader = loader;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerItem banner = bannerList.get(position);

        Glide.with(context)
                .load(banner.getImageUrl())
                .placeholder(R.drawable.banner_placeholder)
                .into(holder.bannerImage);

        holder.bannerTitle.setText(banner.getTitle());
        holder.bannerSubTitle.setText(banner.getSubTitle());
        holder.bannerButton.setText(banner.getButtonText());

        holder.bannerButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onBannerClick(banner);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bannerList == null ? 0 : bannerList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView bannerImage;
        AppCompatTextView bannerTitle;
        AppCompatTextView bannerSubTitle;
        AppCompatTextView bannerButton;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.bannerImage);
            bannerTitle = itemView.findViewById(R.id.bannerTitle);
            bannerSubTitle = itemView.findViewById(R.id.bannerSubTitle);
            bannerButton = itemView.findViewById(R.id.bannerButton);
        }
    }
}