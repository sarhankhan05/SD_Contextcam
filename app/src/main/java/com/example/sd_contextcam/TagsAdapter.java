package com.example.sd_contextcam;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sd_contextcam.data.Tag;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.TagViewHolder> {
    private List<Tag> tags = new ArrayList<>();
    private List<Integer> photoCounts = new ArrayList<>();
    private OnTagClickListener onTagClickListener;

    public interface OnTagClickListener {
        void onTagClick(Tag tag);
    }

    public void setOnTagClickListener(OnTagClickListener listener) {
        this.onTagClickListener = listener;
    }

    public void setTags(List<Tag> tags, List<Integer> photoCounts) {
        this.tags = tags;
        this.photoCounts = photoCounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        Tag tag = tags.get(position);
        int photoCount = position < photoCounts.size() ? photoCounts.get(position) : 0;
        holder.bind(tag, photoCount);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    class TagViewHolder extends RecyclerView.ViewHolder {
        private TextView tagNameText;
        private TextView photoCountText;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagNameText = itemView.findViewById(R.id.tagNameText);
            photoCountText = itemView.findViewById(R.id.photoCountText);
        }

        public void bind(Tag tag, int photoCount) {
            tagNameText.setText(tag.getName());
            photoCountText.setText(String.valueOf(photoCount));
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (onTagClickListener != null) {
                    onTagClickListener.onTagClick(tag);
                }
            });
        }
    }
}