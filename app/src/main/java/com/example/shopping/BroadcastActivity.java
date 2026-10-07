package com.example.shopping;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BroadcastActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etMessage;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);

        db = FirebaseFirestore.getInstance();
        initToolbar();
        initViews();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarBroadcast);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        etTitle = findViewById(R.id.etBroadcastTitle);
        etMessage = findViewById(R.id.etBroadcastMessage);
        Button btnSend = findViewById(R.id.btnSendBroadcast);

        btnSend.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            sendBroadcast(title, message);
        });
    }

    private void sendBroadcast(String title, String message) {
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("title", title);
        broadcast.put("message", message);
        broadcast.put("timestamp", System.currentTimeMillis());

        db.collection("broadcasts").add(broadcast)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Broadcast Sent!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}