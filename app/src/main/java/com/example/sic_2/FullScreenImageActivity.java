package com.example.sic_2;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView imageView = findViewById(R.id.full_image_view);
        String imageUrl = getIntent().getStringExtra("image_url");

        Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .into(imageView);

        imageView.setOnClickListener(v -> finish());
    }
}