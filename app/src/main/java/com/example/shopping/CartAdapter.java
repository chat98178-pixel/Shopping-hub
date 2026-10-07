package com.example.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<Product> cartItems;
    private OnCartActionListener listener;

    public interface OnCartActionListener {
        void onItemRemove(Product product);
        void onQuantityChange(Product product, int newQuantity);
    }

    public CartAdapter(List<Product> cartItems, OnCartActionListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = cartItems.get(position);
        holder.tvName.setText(product.getName());
        holder.tvPrice.setText("Rs " + String.format("%.2f", product.getPrice() * product.getQuantity()));
        holder.tvQuantity.setText(String.valueOf(product.getQuantity()));

        // Show Variant (Size/Color)
        StringBuilder variant = new StringBuilder();
        if (product.getSelectedSize() != null) variant.append("Size: ").append(product.getSelectedSize());
        if (product.getSelectedColor() != null) {
            if (variant.length() > 0) variant.append(" | ");
            variant.append("Color: ").append(product.getSelectedColor());
        }
        
        if (variant.length() > 0) {
            holder.tvVariant.setText(variant.toString());
            holder.tvVariant.setVisibility(View.VISIBLE);
        } else {
            holder.tvVariant.setVisibility(View.GONE);
        }

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.banner_1)
                    .into(holder.ivItem);
        } else {
            holder.ivItem.setImageResource(product.getImageRes() != 0 ? product.getImageRes() : R.drawable.banner_1);
        }

        holder.ivRemove.setOnClickListener(v -> listener.onItemRemove(product));

        holder.btnPlus.setOnClickListener(v -> {
            int newQty = product.getQuantity() + 1;
            product.setQuantity(newQty);
            notifyItemChanged(position);
            listener.onQuantityChange(product, newQty);
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (product.getQuantity() > 1) {
                int newQty = product.getQuantity() - 1;
                product.setQuantity(newQty);
                notifyItemChanged(position);
                listener.onQuantityChange(product, newQty);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItem, ivRemove;
        TextView tvName, tvPrice, tvQuantity, tvVariant;
        ImageButton btnPlus, btnMinus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItem = itemView.findViewById(R.id.ivCartItem);
            ivRemove = itemView.findViewById(R.id.ivRemoveItem);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvVariant = itemView.findViewById(R.id.tvCartVariant);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}