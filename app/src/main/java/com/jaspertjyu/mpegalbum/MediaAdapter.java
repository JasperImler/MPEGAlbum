package com.jaspertjyu.mpegalbum;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private final Context context;
    private final List<MediaItem> mediaItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MediaItem mediaItem);
    }

    public MediaAdapter(Context context, List<MediaItem> mediaItems, OnItemClickListener listener) {
        this.context = context;
        this.mediaItems = mediaItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem mediaItem = mediaItems.get(position);
        holder.mediaNameTextView.setText(mediaItem.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(mediaItem);
            }
        });

        if (mediaItem.isImage()) {
            holder.mediaTypeTextView.setText("Image");
            // Load image thumbnail
            try {
                Bitmap thumbnail = ThumbnailUtils.createImageThumbnail(mediaItem.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
                if (thumbnail != null) {
                    holder.mediaImageView.setImageBitmap(thumbnail);
                } else {
                    holder.mediaImageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } catch (Exception e) {
                holder.mediaImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else if (mediaItem.isVideo()) {
            holder.mediaTypeTextView.setText("MPEG Video");
            // Load video cover image or thumbnail
            String coverPath = mediaItem.getCoverPath();
            if (coverPath != null && !coverPath.isEmpty()) {
                try {
                    Bitmap thumbnail = ThumbnailUtils.createImageThumbnail(coverPath, MediaStore.Images.Thumbnails.MINI_KIND);
                    if (thumbnail != null) {
                        holder.mediaImageView.setImageBitmap(thumbnail);
                        return;
                    }
                } catch (Exception e) {
                    // If cover image loading fails, fall back to video thumbnail
                }
            }
            
            // Fall back to video thumbnail if cover image is not available or failed to load
            try {
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(mediaItem.getPath(), MediaStore.Video.Thumbnails.MINI_KIND);
                if (thumbnail != null) {
                    holder.mediaImageView.setImageBitmap(thumbnail);
                } else {
                    holder.mediaImageView.setImageResource(android.R.drawable.ic_media_play);
                }
            } catch (Exception e) {
                holder.mediaImageView.setImageResource(android.R.drawable.ic_media_play);
            }
        }

    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaImageView;
        TextView mediaNameTextView;
        TextView mediaTypeTextView;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
            mediaNameTextView = itemView.findViewById(R.id.mediaNameTextView);
            mediaTypeTextView = itemView.findViewById(R.id.mediaTypeTextView);
        }
    }
}