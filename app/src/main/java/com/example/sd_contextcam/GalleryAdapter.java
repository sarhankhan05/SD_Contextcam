package com.example.sd_contextcam;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.data.Photo;
import com.google.android.material.chip.Chip;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder> {
    private List<Photo> photos = new ArrayList<>();
    private OnPhotoClickListener onPhotoClickListener;

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.onPhotoClickListener = listener;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.bind(photo);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private ImageView photoThumbnail;
        private LinearLayout tagsContainer;
        private TextView timestampText;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoThumbnail = itemView.findViewById(R.id.photoThumbnail);
            tagsContainer = itemView.findViewById(R.id.tagsContainer);
            timestampText = itemView.findViewById(R.id.timestampText);
        }

        public void bind(Photo photo) {
            // Set timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            timestampText.setText(sdf.format(photo.timestamp));

            // Load photo thumbnail
            loadImage(photo.filePath);

            // TODO: Add tags to the tags container
            // For now, we'll just clear the container
            tagsContainer.removeAllViews();

            // Set click listener
            itemView.setOnClickListener(v -> {
                // Open photo in full screen
                Context context = v.getContext();
                Intent intent = new Intent(context, PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_PATH, photo.getFilePath());
                intent.putExtra(PhotoDetailActivity.EXTRA_IS_VAULT_PHOTO, false); // Regular photos
                context.startActivity(intent);
                
                // If we have a listener, also notify it
                if (onPhotoClickListener != null) {
                    onPhotoClickListener.onPhotoClick(photo);
                }
            });
        }

        private void loadImage(String imagePath) {
            // Load image in background thread
            new Thread(() -> {
                try {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        // Decode the image file into a Bitmap
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(imagePath, options);

                        // Calculate inSampleSize
                        options.inSampleSize = calculateInSampleSize(options, 200, 200);

                        // Decode bitmap with inSampleSize set
                        options.inJustDecodeBounds = false;
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

                        // Update UI on main thread
                        final Bitmap finalBitmap = bitmap;
                        photoThumbnail.post(() -> {
                            if (finalBitmap != null) {
                                photoThumbnail.setImageBitmap(finalBitmap);
                            } else {
                                photoThumbnail.setImageResource(R.drawable.ic_gallery);
                            }
                        });
                    } else {
                        photoThumbnail.post(() -> photoThumbnail.setImageResource(R.drawable.ic_gallery));
                    }
                } catch (Exception e) {
                    photoThumbnail.post(() -> photoThumbnail.setImageResource(R.drawable.ic_gallery));
                }
            }).start();
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }
}