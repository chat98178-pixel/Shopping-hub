package com.example.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> implements Filterable {

    private List<Product> productList;
    private List<Product> productListFull;
    private OnProductClickListener listener;
    private String selectedCategory = "All";
    private String currentSearchQuery = "";
    private double maxPrice = Double.MAX_VALUE;
    private boolean onlyHighRated = false;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onAddToCartClick(Product product);
        void onFavoriteClick(Product product);
    }

    public ProductAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.productListFull = new ArrayList<>(productList);
        this.listener = listener;
    }

    public void updateList(List<Product> newList) {
        this.productListFull = new ArrayList<>(newList);
        // Safely update the displayed list
        this.productList.clear();
        this.productList.addAll(newList);
        
        // Re-apply any active search or category filter
        getFilter().filter(currentSearchQuery);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        if (product == null) return;

        holder.tvProductName.setText(product.getName() != null ? product.getName() : "Unknown");
        holder.tvProductPrice.setText("Rs " + String.format("%.2f", product.getPrice()));

        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.banner_1)
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.banner_1);
        }

        if (WishlistManager.getInstance().isInWishlist(product)) {
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite);
        } else {
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null) listener.onAddToCartClick(product);
        });
        holder.ivFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(product);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    @Override
    public Filter getFilter() {
        return productFilter;
    }

    public void filterByCategory(String category) {
        this.selectedCategory = category;
        getFilter().filter(currentSearchQuery);
    }

    public void setAdvancedFilter(double maxPrice, boolean onlyHighRated) {
        this.maxPrice = maxPrice > 0 ? maxPrice : Double.MAX_VALUE;
        this.onlyHighRated = onlyHighRated;
        getFilter().filter(currentSearchQuery);
    }

    private Filter productFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            currentSearchQuery = constraint != null ? constraint.toString() : "";
            List<Product> filteredList = new ArrayList<>();

            String searchPattern = currentSearchQuery.toLowerCase().trim();

            for (Product item : productListFull) {
                boolean matchesCategory = selectedCategory.equals("All") || item.getCategory().equalsIgnoreCase(selectedCategory);
                boolean matchesSearch = item.getName().toLowerCase().contains(searchPattern);
                boolean matchesPrice = item.getPrice() <= maxPrice;
                boolean matchesRating = !onlyHighRated || item.getRating() >= 4.0;

                if (matchesCategory && matchesSearch && matchesPrice && matchesRating) {
                    filteredList.add(item);
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            productList.clear();
            if (results.values != null) {
                productList.addAll((List) results.values);
            }
            notifyDataSetChanged();
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct, ivFavorite;
        TextView tvProductName, tvProductPrice;
        Button btnAddToCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}