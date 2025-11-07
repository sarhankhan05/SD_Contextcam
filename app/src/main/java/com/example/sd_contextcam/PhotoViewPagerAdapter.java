package com.example.sd_contextcam; // Make sure package name is correct

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.security.EncryptionUtil; // Assuming you have this

import java.io.File;
import java.util.List;

public class PhotoViewPagerAdapter extends RecyclerView.Adapter<PhotoViewPagerAdapter.PhotoViewHolder> {

    private List<Photo> photoList;
    private Context context;
    private EncryptionUtil encryptionUtil;

    public PhotoViewPagerAdapter(List<Photo> photoList, Context context) {
        this.photoList = photoList;
        this.context = context;
        this.encryptionUtil = new EncryptionUtil(context); // Init encryption utility
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo_fullscreen, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photoList.get(position);
        holder.bindPhoto(photo);
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.fullscreen_image_view);
        }

        void bindPhoto(Photo photo) {
            // Load photo in a background thread to avoid freezing the UI
            new Thread(() -> {
                Bitmap bitmap = loadBitmap(photo);
                // Post the result back to the UI thread
                if (bitmap != null) {
                    imageView.post(() -> imageView.setImageBitmap(bitmap));
                }
            }).start();
        }

        private Bitmap loadBitmap(Photo photo) {
            File photoFile = new File(photo.getFilePath());
            if (!photoFile.exists()) {
                return null;
            }

            if (photo.isEncrypted()) {
                // Decrypt to a temporary file
                File tempFile = new File(context.getCacheDir(), "temp_decrypted.jpg");
                boolean success = encryptionUtil.decryptPhoto(photo.getFilePath(), tempFile.getAbsolutePath());
                if (success) {
                    Bitmap bmp = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
                    tempFile.delete(); // Clean up temp file
                    return bmp;
                }
            } else {
                // Load standard photo
                return BitmapFactory.decodeFile(photo.getFilePath());
            }
            return null;
        }
    }
}