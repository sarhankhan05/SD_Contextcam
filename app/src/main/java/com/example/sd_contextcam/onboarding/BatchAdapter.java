package com.example.sd_contextcam.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.R;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class BatchAdapter extends RecyclerView.Adapter<BatchAdapter.BatchViewHolder> {
    private List<PhotoBatch> batches = new ArrayList<>();
    private List<String> allTags = new ArrayList<>();
    private OnTagApplyListener onTagApplyListener;

    public interface OnTagApplyListener {
        void onTagApply(PhotoBatch batch, String tag);
    }

    public void setOnTagApplyListener(OnTagApplyListener listener) {
        this.onTagApplyListener = listener;
    }

    public void setBatches(List<PhotoBatch> batches) {
        this.batches = batches;
        notifyDataSetChanged();
    }

    public void setAllTags(List<String> allTags) {
        this.allTags = allTags;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_batch, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        PhotoBatch batch = batches.get(position);
        holder.bind(batch);
    }

    @Override
    public int getItemCount() {
        return batches.size();
    }

    class BatchViewHolder extends RecyclerView.ViewHolder {
        private TextView batchTitle;
        private TextView batchDescription;
        private TextView photoCount;
        private AutoCompleteTextView tagAutoComplete;
        private TextInputLayout tagInputLayout;
        private Button applyTagButton;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            batchTitle = itemView.findViewById(R.id.batchTitle);
            batchDescription = itemView.findViewById(R.id.batchDescription);
            photoCount = itemView.findViewById(R.id.photoCount);
            tagAutoComplete = itemView.findViewById(R.id.tagAutoComplete);
            tagInputLayout = itemView.findViewById(R.id.tagInputLayout);
            applyTagButton = itemView.findViewById(R.id.applyTagButton);
        }

        public void bind(PhotoBatch batch) {
            batchTitle.setText(batch.getTitle());
            batchDescription.setText(batch.getDescription());
            photoCount.setText(batch.getPhotoCount() + " photos");

            // Set up tag autocomplete
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    allTags
            );
            tagAutoComplete.setAdapter(adapter);
            
            // Set suggested tag if available
            if (batch.getSuggestedTag() != null && !batch.getSuggestedTag().isEmpty()) {
                tagAutoComplete.setText(batch.getSuggestedTag(), false);
            }

            // Set up apply button
            applyTagButton.setOnClickListener(v -> {
                String tag = tagAutoComplete.getText().toString().trim();
                if (!tag.isEmpty() && onTagApplyListener != null) {
                    onTagApplyListener.onTagApply(batch, tag);
                }
            });
        }
    }
}