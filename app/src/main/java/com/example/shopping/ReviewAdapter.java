package com.example.shopping;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<Review> reviewList;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.tvUser.setText(review.getUserName());
        holder.tvComment.setText(review.getComment());
        
        if (review.getImageUrl() != null && !review.getImageUrl().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(review.getImageUrl()).into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        if (review.isVideo()) {
            holder.flVideo.setVisibility(View.VISIBLE);
            holder.flVideo.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(review.getVideoUrl()), "video/*");
                holder.itemView.getContext().startActivity(intent);
            });
        } else {
            holder.flVideo.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvComment;
        ImageView ivImage;
        FrameLayout flVideo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvReviewUser);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            ivImage = itemView.findViewById(R.id.ivReviewImage);
            flVideo = itemView.findViewById(R.id.flVideoContainer);
        }
    }
}