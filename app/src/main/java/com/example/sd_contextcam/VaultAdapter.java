package com.example.sd_contextcam;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.Serializable; // Import Serializable
import java.util.ArrayList;
import java.util.List;

public class VaultAdapter extends RecyclerView.Adapter<VaultAdapter.VaultPhotoViewHolder> {
    private List<String> encryptedPhotoPaths = new ArrayList<>();
    // ... (listener interface is fine)

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
        // Pass the full list and the current position to the ViewHolder
        holder.bind(photoPath, encryptedPhotoPaths, position);
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

        // MODIFIED: bind() now accepts the full list and position
        public void bind(String photoPath, List<String> allPhotoPaths, int position) {
            // ... (placeholder thumbnail logic is fine for now)
            photoThumbnail.setImageResource(R.drawable.ic_gallery);

            itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, PhotoDetailActivity.class);

                // --- THIS IS THE FIX ---
                // 1. Send the list of all photos
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_LIST, (Serializable) allPhotoPaths);
                // 2. Send the position of the clicked photo
                intent.putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, position);
                // 3. Keep the vault flag
                intent.putExtra(PhotoDetailActivity.EXTRA_IS_VAULT_PHOTO, true);
                // --- END FIX ---

                context.startActivity(intent);
            });
        }
    }
}
