package com.example.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    private List<Banner> bannerList;
    private OnBannerClickListener listener;

    public interface OnBannerClickListener {
        void onBannerDelete(Banner banner);
    }

    public BannerAdapter(List<Banner> bannerList, OnBannerClickListener listener) {
        this.bannerList = bannerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Banner banner = bannerList.get(position);
        
        if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(banner.getImageUrl())
                    .placeholder(R.drawable.banner_1)
                    .into(holder.ivBanner);
        } else {
            holder.ivBanner.setImageResource(R.drawable.banner_1);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerDelete(banner);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;
        FloatingActionButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.ivBannerPreview);
            btnDelete = itemView.findViewById(R.id.btnDeleteBanner);
        }
    }
}