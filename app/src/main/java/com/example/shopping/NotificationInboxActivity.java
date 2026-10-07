package com.example.shopping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationInboxActivity extends AppCompatActivity {

    private RecyclerView rvNotifs;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private List<NotificationItem> notificationList;
    private NotifAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
        loadNotifications();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        rvNotifs = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmptyInbox);
        notificationList = new ArrayList<>();
        adapter = new NotifAdapter(notificationList);
        rvNotifs.setLayoutManager(new LinearLayoutManager(this));
        rvNotifs.setAdapter(adapter);
    }

    private void loadNotifications() {
        db.collection("broadcasts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            NotificationItem item = new NotificationItem(
                                    doc.getString("title"),
                                    doc.getString("message"),
                                    doc.getLong("timestamp")
                            );
                            notificationList.add(item);
                        }
                        if (notificationList.isEmpty()) tvEmpty.setVisibility(View.VISIBLE);
                        else tvEmpty.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // Small Model and Adapter classes
    private static class NotificationItem {
        String title, message;
        Long timestamp;
        public NotificationItem(String t, String m, Long ts) { this.title = t; this.message = m; this.timestamp = ts; }
    }

    private static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.ViewHolder> {
        private List<NotificationItem> list;
        public NotifAdapter(List<NotificationItem> list) { this.list = list; }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMsg.setText(item.message);
            if (item.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                holder.tvTime.setText(sdf.format(new Date(item.timestamp)));
            }
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMsg, tvTime;
            public ViewHolder(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNotifTitle);
                tvMsg = v.findViewById(R.id.tvNotifMessage);
                tvTime = v.findViewById(R.id.tvNotifTime);
            }
        }
    }
}