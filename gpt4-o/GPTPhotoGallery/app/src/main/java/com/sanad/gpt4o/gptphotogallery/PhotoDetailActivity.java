package com.sanad.gpt4o.gptphotogallery;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class PhotoDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        ImageView imageView = findViewById(R.id.detail_image_view);
        String photoPath = getIntent().getStringExtra("photoPath");
        Glide.with(this).load(photoPath).into(imageView);
    }
}