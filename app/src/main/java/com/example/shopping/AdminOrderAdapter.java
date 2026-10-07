package com.example.shopping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.ViewHolder> {

    private List<Order> orderList;
    private OnOrderStatusUpdateListener listener;

    public interface OnOrderStatusUpdateListener {
        void onStatusUpdate(Order order, String newStatus);
    }

    public AdminOrderAdapter(List<Order> orderList, OnOrderStatusUpdateListener listener) {
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
        
        StringBuilder itemsSummary = new StringBuilder("Items: ");
        for (int i = 0; i < order.getItems().size(); i++) {
            Product p = order.getItems().get(i);
            itemsSummary.append(p.getName()).append(" (x").append(p.getQuantity()).append(")");
            if (i < order.getItems().size() - 1) {
                itemsSummary.append(", ");
            }
        }
        holder.tvOrderItems.setText(itemsSummary.toString());
        holder.tvOrderTotal.setText("Rs " + String.format(Locale.getDefault(), "%.2f", order.getTotalAmount()));
        
        holder.tvStatus.setText(order.getStatus());
        
        // Show status change button
        holder.btnUpdateStatus.setVisibility(View.VISIBLE);
        holder.btnUpdateStatus.setOnClickListener(v -> {
            String nextStatus = "Pending";
            if (order.getStatus().equals("Pending")) nextStatus = "Shipped";
            else if (order.getStatus().equals("Shipped")) nextStatus = "Delivered";
            
            listener.onStatusUpdate(order, nextStatus);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderItems, tvOrderTotal, tvStatus;
        Button btnUpdateStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }
    }
}