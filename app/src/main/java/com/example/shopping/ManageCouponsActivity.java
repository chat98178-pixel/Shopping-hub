package com.example.shopping;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageCouponsActivity extends AppCompatActivity {

    private RecyclerView rvCoupons;
    private FirebaseFirestore db;
    private List<Coupon> couponList;
    private CouponAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_coupons);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadCoupons();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarCoupons);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvCoupons = findViewById(R.id.rvCoupons);
        couponList = new ArrayList<>();
        adapter = new CouponAdapter(couponList, this::deleteCoupon);
        rvCoupons.setLayoutManager(new LinearLayoutManager(this));
        rvCoupons.setAdapter(adapter);

        findViewById(R.id.fabAddCoupon).setOnClickListener(v -> showAddCouponDialog());
    }

    private void loadCoupons() {
        db.collection("coupons").addSnapshotListener((value, error) -> {
            if (value != null) {
                couponList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Coupon c = doc.toObject(Coupon.class);
                    couponList.add(c);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showAddCouponDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_add_coupon, null);
        EditText etCode = view.findViewById(R.id.etCouponCodeAdd);
        EditText etAmt = view.findViewById(R.id.etCouponAmtAdd);
        EditText etMin = view.findViewById(R.id.etCouponMinAdd);

        new AlertDialog.Builder(this)
                .setTitle("Add New Coupon")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String code = etCode.getText().toString().trim().toUpperCase();
                    String amt = etAmt.getText().toString().trim();
                    String min = etMin.getText().toString().trim();

                    if (!code.isEmpty() && !amt.isEmpty()) {
                        Coupon coupon = new Coupon(code, Double.parseDouble(amt), Double.parseDouble(min.isEmpty() ? "0" : min));
                        db.collection("coupons").document(code).set(coupon);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCoupon(Coupon coupon) {
        db.collection("coupons").document(coupon.getCode()).delete();
    }

    // Inner Adapter Class for simplicity
    private static class CouponAdapter extends RecyclerView.Adapter<CouponAdapter.ViewHolder> {
        private List<Coupon> list;
        private java.util.function.Consumer<Coupon> deleteListener;

        public CouponAdapter(List<Coupon> list, java.util.function.Consumer<Coupon> deleteListener) {
            this.list = list;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coupon, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Coupon c = list.get(position);
            holder.tvCode.setText(c.getCode());
            holder.tvDetails.setText("Rs " + c.getDiscountAmount() + " off | Min Rs " + c.getMinOrderValue());
            holder.btnDelete.setOnClickListener(v -> deleteListener.accept(c));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCode, tvDetails;
            ImageView btnDelete;
            public ViewHolder(@NonNull View v) {
                super(v);
                tvCode = v.findViewById(R.id.tvCouponCode);
                tvDetails = v.findViewById(R.id.tvCouponDetails);
                btnDelete = v.findViewById(R.id.btnDeleteCoupon);
            }
        }
    }
}