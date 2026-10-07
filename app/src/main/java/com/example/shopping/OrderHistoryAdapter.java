package com.example.shopping;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private List<Order> orderList;
    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onCancelClick(Order order);
        void onReorderClick(Order order);
    }

    public OrderHistoryAdapter(List<Order> orderList, OnOrderActionListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);
        
        holder.tvOrderId.setText("Order #" + (order.getOrderId() != null ? order.getOrderId().substring(0, 8).toUpperCase() : "N/A"));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        holder.tvOrderDate.setText(sdf.format(new Date(order.getTimestamp())));
        
        // Populate items sub-list
        holder.llItems.removeAllViews();
        for (Product product : order.getItems()) {
            TextView tv = new TextView(holder.itemView.getContext());
            tv.setText("• " + product.getName() + " (x" + product.getQuantity() + ")");
            tv.setTextColor(Color.BLUE);
            tv.setPadding(0, 8, 0, 8);
            tv.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), ProductDetailActivity.class);
                intent.putExtra("product", product);
                holder.itemView.getContext().startActivity(intent);
            });
            holder.llItems.addView(tv);
        }

        holder.tvOrderItems.setVisibility(View.GONE); // Hide the old summary text
        holder.tvOrderTotal.setText("Rs " + String.format(Locale.getDefault(), "%.2f", order.getTotalAmount()));
        
        String status = order.getStatus() != null ? order.getStatus() : "Pending";
        holder.tvStatus.setText(status);

        // Show cancel button only for pending
        if (status.equalsIgnoreCase("Pending")) {
            holder.btnCancel.setVisibility(View.VISIBLE);
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancelClick(order);
        });

        holder.btnReorder.setOnClickListener(v -> {
            if (listener != null) listener.onReorderClick(order);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), OrderDetailActivity.class);
            intent.putExtra("order", order);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderItems, tvOrderTotal, tvStatus;
        LinearLayout llItems;
        Button btnCancel, btnReorder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            llItems = itemView.findViewById(R.id.llOrderItemsContainer);
            btnCancel = itemView.findViewById(R.id.btnCancelOrder);
            btnReorder = itemView.findViewById(R.id.btnReorder);
        }
    }
}