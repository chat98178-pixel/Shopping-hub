package com.example.shopping;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BatchProductAdapter extends RecyclerView.Adapter<BatchProductAdapter.ViewHolder> {

    private List<BatchProduct> productList;
    private String[] categories;

    public BatchProductAdapter(List<BatchProduct> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        categories = parent.getContext().getResources().getStringArray(R.array.categories);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BatchProduct product = productList.get(position);
        
        holder.ivPreview.setImageURI(product.getImageUri());
        
        // Remove existing watchers to prevent recursion/incorrect data binding
        holder.etName.removeTextChangedListener(holder.nameWatcher);
        holder.etPrice.removeTextChangedListener(holder.priceWatcher);
        holder.etDesc.removeTextChangedListener(holder.descWatcher);
        holder.etStock.removeTextChangedListener(holder.stockWatcher);
        
        holder.etName.setText(product.getName());
        holder.etPrice.setText(product.getPrice());
        holder.etDesc.setText(product.getDescription());
        holder.etStock.setText(product.getStock());
        
        // Set Spinner selection
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(product.getCategory())) {
                holder.spinnerCategory.setSelection(i);
                break;
            }
        }

        // Attach new watchers
        holder.nameWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                product.setName(s.toString());
            }
        };
        holder.priceWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                product.setPrice(s.toString());
            }
        };
        holder.descWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                product.setDescription(s.toString());
            }
        };
        holder.stockWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                product.setStock(s.toString());
            }
        };

        holder.etName.addTextChangedListener(holder.nameWatcher);
        holder.etPrice.addTextChangedListener(holder.priceWatcher);
        holder.etDesc.addTextChangedListener(holder.descWatcher);
        holder.etStock.addTextChangedListener(holder.stockWatcher);

        holder.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                product.setCategory(categories[pos]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPreview;
        EditText etName, etPrice, etDesc, etStock;
        Spinner spinnerCategory;
        TextWatcher nameWatcher, priceWatcher, descWatcher, stockWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPreview = itemView.findViewById(R.id.ivBatchPreview);
            etName = itemView.findViewById(R.id.etBatchName);
            etPrice = itemView.findViewById(R.id.etBatchPrice);
            etDesc = itemView.findViewById(R.id.etBatchDescription);
            etStock = itemView.findViewById(R.id.etBatchStock);
            spinnerCategory = itemView.findViewById(R.id.spinnerBatchCategory);

            String[] cats = itemView.getContext().getResources().getStringArray(R.array.categories);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_item, cats);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}