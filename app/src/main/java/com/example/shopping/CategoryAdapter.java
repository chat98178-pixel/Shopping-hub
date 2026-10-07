package com.example.shopping;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categoryList;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvCategoryName.setText(category.getName());
        holder.ivCategory.setImageResource(category.getIconRes());

        // Dynamic attractive colors
        int baseColor = ContextCompat.getColor(holder.itemView.getContext(), category.getColorRes());
        int primaryColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_500);

        if (selectedPosition == position) {
            holder.cvCategoryIcon.setCardBackgroundColor(primaryColor);
            holder.ivCategory.setColorFilter(Color.WHITE);
            holder.tvCategoryName.setTextColor(primaryColor);
            holder.tvCategoryName.setTextStyle(android.graphics.Typeface.BOLD);
        } else {
            holder.cvCategoryIcon.setCardBackgroundColor(baseColor);
            holder.ivCategory.setColorFilter(primaryColor);
            holder.tvCategoryName.setTextColor(Color.BLACK);
            holder.tvCategoryName.setTextStyle(android.graphics.Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategory;
        CustomTextView tvCategoryName;
        CardView cvCategoryIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategory = itemView.findViewById(R.id.ivCategory);
            tvCategoryName = new CustomTextView(itemView.findViewById(R.id.tvCategoryName));
            cvCategoryIcon = itemView.findViewById(R.id.cvCategoryIcon);
        }
    }

    // Helper class to manage TextView styles easily
    private static class CustomTextView {
        private TextView textView;
        public CustomTextView(TextView tv) { this.textView = tv; }
        public void setText(String text) { textView.setText(text); }
        public void setTextColor(int color) { textView.setTextColor(color); }
        public void setTextStyle(int style) { textView.setTypeface(null, style); }
    }
}