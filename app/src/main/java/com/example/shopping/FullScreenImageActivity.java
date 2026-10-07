package com.example.shopping;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Full screen immersive mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_full_screen_image);

        String imageUrl = getIntent().getStringExtra("image_url");
        int imageRes = getIntent().getIntExtra("image_res", 0);

        PhotoView photoView = findViewById(R.id.photoView);
        FloatingActionButton btnClose = findViewById(R.id.btnClosePhoto);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(photoView);
        } else if (imageRes != 0) {
            photoView.setImageResource(imageRes);
        }

        btnClose.setOnClickListener(v -> finish());
    }
}