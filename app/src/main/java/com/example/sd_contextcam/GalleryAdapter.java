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
// ADD this import
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.data.Photo;
// REMOVE this import, it's not used here
// import com.google.android.material.chip.Chip;

import java.io.File;
import java.io.Serializable; // <--- ADD THIS IMPORT
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.PhotoViewHolder> {
    private List<Photo> photos = new ArrayList<>();
    private OnPhotoClickListener onPhotoClickListener;
    private int currentTagId = -1;
    private String currentViewMode = "TAG";

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.onPhotoClickListener = listener;
    }

    public void setPhotos(List<Photo> photos, int tagId, String viewMode) {
        this.photos = (photos != null) ? photos : new ArrayList<>();
        this.currentTagId = tagId;
        this.currentViewMode = (viewMode != null) ? viewMode : "TAG";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_photo_grid, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.bind(photo); // Pass only photo
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private ImageView photoThumbnail;
        private ImageView vaultIcon;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoThumbnail = itemView.findViewById(R.id.photo_thumbnail);
            vaultIcon = itemView.findViewById(R.id.vault_icon);
        }

        public void bind(Photo photo) {
            // Load photo thumbnail
            loadImage(photo.getFilePath(), photo.isEncrypted());

            // Show vault icon if encrypted
            if (vaultIcon != null) {
                vaultIcon.setVisibility(photo.isEncrypted() ? View.VISIBLE : View.GONE);
            }

            // --- MODIFIED: Click listener for swiping ---
            itemView.setOnClickListener(v -> {
                int clickedPosition = getAdapterPosition();
                if (clickedPosition == RecyclerView.NO_POSITION) {
                    return; // Guard against clicks during animations
                }

                Context context = v.getContext();
                Intent intent = new Intent(context, PhotoDetailActivity.class);

                // 1. Get the list of all photo file paths from the current adapter state.
                List<String> allPhotoPaths = photos.stream()
                        .map(Photo::getFilePath)
                        .collect(Collectors.toList());

                // 2. Pass the full list of paths to enable swiping
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, (Serializable) allPhotoPaths);

                // 3. Pass the specific position that was clicked
                intent.putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, clickedPosition);

                // 4. Tell the detail view if these photos are from the vault (for decryption)
                intent.putExtra(PhotoDetailActivity.EXTRA_IS_VAULT_PHOTO, photo.isEncrypted());

                context.startActivity(intent);

                // Notify listener if it exists (optional)
                if (onPhotoClickListener != null) {
                    onPhotoClickListener.onPhotoClick(photo);
                }
            });
            // --- END MODIFICATION ---
        }

        // --- MODIFIED: loadImage handles encryption using CryptoUtils ---
        private void loadImage(String imagePath, boolean isEncrypted) {
            new Thread(() -> {
                final Bitmap bitmap;
                if (isEncrypted) {
                    // Decrypt photo using your existing CryptoUtils class
                    bitmap = CryptoUtils.decryptPhoto(imagePath, itemView.getContext());
                } else {
                    // This case is for non-encrypted photos, if you have any.
                    bitmap = BitmapFactory.decodeFile(imagePath);
                }

                // Update UI on main thread
                photoThumbnail.post(() -> {
                    if (bitmap != null) {
                        photoThumbnail.setImageBitmap(bitmap);
                    } else {
                        postImageError();
                    }
                });
            }).start();
        }

        private void postImageError() {
            photoThumbnail.post(() -> {
                photoThumbnail.setImageResource(R.drawable.ic_error); // Use the error icon
            });
        }
        // --- END MODIFICATION ---

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }
    }
}
