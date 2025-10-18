package com.example.sd_contextcam;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class VaultAdapter extends RecyclerView.Adapter<VaultAdapter.VaultPhotoViewHolder> {
    private List<String> encryptedPhotoPaths = new ArrayList<>();
    private OnVaultPhotoClickListener onVaultPhotoClickListener;

    public interface OnVaultPhotoClickListener {
        void onVaultPhotoClick(String encryptedPhotoPath);
    }

    public void setOnVaultPhotoClickListener(OnVaultPhotoClickListener listener) {
        this.onVaultPhotoClickListener = listener;
    }

    public void setEncryptedPhotoPaths(List<String> encryptedPhotoPaths) {
        this.encryptedPhotoPaths = encryptedPhotoPaths;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VaultPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vault_photo, parent, false);
        return new VaultPhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VaultPhotoViewHolder holder, int position) {
        String photoPath = encryptedPhotoPaths.get(position);
        holder.bind(photoPath);
    }

    @Override
    public int getItemCount() {
        return encryptedPhotoPaths.size();
    }

    class VaultPhotoViewHolder extends RecyclerView.ViewHolder {
        private ImageView photoThumbnail;
        private ImageView lockIcon;

        public VaultPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoThumbnail = itemView.findViewById(R.id.photoThumbnail);
            lockIcon = itemView.findViewById(R.id.lockIcon);
        }

        public void bind(String photoPath) {
            // TODO: Load thumbnail for encrypted photo
            // For now, we'll set a placeholder
            photoThumbnail.setImageResource(R.drawable.ic_gallery);

            // Set click listener
            itemView.setOnClickListener(v -> {
                // Open photo in full screen
                Context context = v.getContext();
                Intent intent = new Intent(context, PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_PATH, photoPath);
                intent.putExtra(PhotoDetailActivity.EXTRA_IS_VAULT_PHOTO, true); // Vault photos
                context.startActivity(intent);
                
                // If we have a listener, also notify it
                if (onVaultPhotoClickListener != null) {
                    onVaultPhotoClickListener.onVaultPhotoClick(photoPath);
                }
            });
        }
    }
}