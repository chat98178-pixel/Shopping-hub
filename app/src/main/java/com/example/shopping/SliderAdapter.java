package com.example.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.ViewHolder> {

    private List<String> bannerList;

    public SliderAdapter(List<String> bannerList) {
        this.bannerList = bannerList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slider, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (bannerList == null || bannerList.isEmpty()) return;
        String imageUrl = bannerList.get(position);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.banner_1)
                        .error(R.drawable.banner_1)
                        .into(holder.ivBanner);
            } catch (Exception e) {
                holder.ivBanner.setImageResource(R.drawable.banner_1);
            }
        } else {
            holder.ivBanner.setImageResource(R.drawable.banner_1);
        }
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // In item_slider.xml we had a View, let's change it to ImageView
            ivBanner = itemView.findViewById(R.id.ivBanner);
        }
    }
}