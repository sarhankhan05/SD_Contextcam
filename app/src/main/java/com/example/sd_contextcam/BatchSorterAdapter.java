package com.example.sd_contextcam; // Make sure this package name is correct

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.data.Photo;
import com.example.sd_contextcam.data.Tag;
import com.example.sd_contextcam.viewmodel.PhotoViewModel;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BatchSorterAdapter extends RecyclerView.Adapter<BatchSorterAdapter.BatchViewHolder> {

    private List<BatchItem> batchList;
    private Context context;
    private PhotoViewModel photoViewModel;
    private List<String> tagSuggestions = new ArrayList<>();

    public BatchSorterAdapter(List<BatchItem> batchList, Context context, PhotoViewModel photoViewModel) {
        this.batchList = batchList;
        this.context = context;
        this.photoViewModel = photoViewModel;

        // Get the list of tags from the database for the dropdown suggestions
        photoViewModel.getTags().observe((LifecycleOwner) context, tags -> {
            if (tags != null) {
                tagSuggestions = tags.stream().map(Tag::getName).collect(Collectors.toList());
            }
        });
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_batch, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        BatchItem batch = batchList.get(position);

        // 1. Set the text
        holder.batchTitle.setText(batch.getTitle());
        holder.batchDescription.setText(batch.getDescription());
        holder.photoCount.setText(batch.getPhotoCount() + " photos");

        // 2. Set up the AutoComplete (the dropdown)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                tagSuggestions
        );
        holder.tagAutoComplete.setAdapter(adapter);

        // 3. Set the "Apply Tag" button click listener
        holder.applyTagButton.setOnClickListener(v -> {
            String tagName = holder.tagAutoComplete.getText().toString().trim();

            if (tagName.isEmpty()) {
                holder.tagInputLayout.setError("Please enter a tag name");
                return;
            }

            holder.tagInputLayout.setError(null);

            // Apply tag to all photos in the batch
            applyTagToBatchPhotos(batch, tagName, position);
        });
    }

    @Override
    public int getItemCount() {
        return batchList.size();
    }

    public void updateBatches(List<BatchItem> newBatches) {
        this.batchList = newBatches;
        notifyDataSetChanged();
    }

    private void applyTagToBatchPhotos(BatchItem batch, String tagName, int position) {
        // Show loading state
        Toast.makeText(context, "Applying tag '" + tagName + "' to " + batch.getPhotoCount() + " photos...", Toast.LENGTH_SHORT).show();

        // Apply tag to all photos in the batch on a background thread
        new Thread(() -> {
            try {
                // 1. Check if tag exists, if not create it
                Tag existingTag = photoViewModel.getRepository().getTagByName(tagName);
                Tag tag;
                
                if (existingTag == null) {
                    // Create new tag
                    tag = new Tag();
                    tag.setName(tagName);
                    long tagId = photoViewModel.getRepository().insertTag(tag);
                    // Get the created tag with its ID
                    tag = photoViewModel.getRepository().getTagById((int) tagId);
                } else {
                    tag = existingTag;
                }

                if (tag == null) {
                    // Error creating tag
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Error creating tag", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 2. Process each photo in the batch
                int successCount = 0;
                for (String photoPath : batch.getPhotoPaths()) {
                    try {
                        // Check if photo already exists in database
                        Photo existingPhoto = photoViewModel.getRepository().getPhotoByFilePath(photoPath);
                        Photo photo;
                        
                        if (existingPhoto == null) {
                            // Create new photo entry
                            photo = new Photo();
                            photo.setFilePath(photoPath);
                            photo.setTimestamp(System.currentTimeMillis());
                            long photoId = photoViewModel.getRepository().insertPhoto(photo);
                            photo = photoViewModel.getRepository().getPhotoById((int) photoId);
                        } else {
                            photo = existingPhoto;
                        }

                        if (photo != null) {
                            // 3. Link photo to tag
                            photoViewModel.getRepository().addTagToPhoto(photo.getId(), tag.getId());
                            successCount++;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("BatchSorterAdapter", "Error processing photo: " + photoPath, e);
                    }
                }

                // Update UI on main thread
                final int finalSuccessCount = successCount;
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Successfully tagged " + finalSuccessCount + " photos as '" + tagName + "'", Toast.LENGTH_SHORT).show();
                    
                    // Remove the batch from the list since it's been processed
                    batchList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, batchList.size());
                });

            } catch (Exception e) {
                android.util.Log.e("BatchSorterAdapter", "Error applying tag to batch photos", e);
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Error applying tag: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // The ViewHolder class
    public static class BatchViewHolder extends RecyclerView.ViewHolder {
        TextView batchTitle;
        TextView photoCount;
        TextView batchDescription;
        TextInputLayout tagInputLayout;
        AutoCompleteTextView tagAutoComplete;
        Button applyTagButton;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            batchTitle = itemView.findViewById(R.id.batchTitle);
            photoCount = itemView.findViewById(R.id.photoCount);
            batchDescription = itemView.findViewById(R.id.batchDescription);
            tagInputLayout = itemView.findViewById(R.id.tagInputLayout);
            tagAutoComplete = itemView.findViewById(R.id.tagAutoComplete);
            applyTagButton = itemView.findViewById(R.id.applyTagButton);
        }
    }
}