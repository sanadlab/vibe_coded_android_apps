package com.sanad.gemini_2_dot_5_pro_preview.galleryapp;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<Uri> photoUris;
    private Context context;

    // Optional: Add listener for item clicks
    public interface OnItemClickListener {
        void onItemClick(Uri photoUri);
    }
    private OnItemClickListener listener;

    public PhotoAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.photoUris = new ArrayList<>();
        this.listener = listener; // Assign listener
    }

    public void setPhotos(List<Uri> photoUris) {
        this.photoUris.clear();
        if (photoUris != null) {
            this.photoUris.addAll(photoUris);
        }
        notifyDataSetChanged(); // Consider using DiffUtil for better performance
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_photo_grid, parent, false); // Use your item layout
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri photoUri = photoUris.get(position);
        holder.bind(photoUri, listener); // Pass listener to ViewHolder
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    // --- ViewHolder ---
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPhoto = itemView.findViewById(R.id.imageViewPhoto);
        }

        public void bind(final Uri photoUri, final OnItemClickListener listener) {
            // Use Glide to load the image efficiently
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.placeholder_image) // Use your placeholder
                    .error(R.drawable.error_image)           // Use your error placeholder
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache original & resized
                    .centerCrop(); // Scale type

            Glide.with(itemView.getContext())
                    .load(photoUri)
                    .apply(options)
                    .into(imageViewPhoto);

            // Set click listener on the item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(photoUri);
                }
            });
        }
    }
}