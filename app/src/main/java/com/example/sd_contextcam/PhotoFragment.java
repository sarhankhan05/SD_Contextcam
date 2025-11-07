// Create a new file: PhotoFragment.java
package com.example.sd_contextcam;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PhotoFragment extends Fragment {

    private static final String ARG_PHOTO_PATH = "arg_photo_path";
    private static final String ARG_IS_VAULT = "arg_is_vault";

    public static PhotoFragment newInstance(String photoPath, boolean isVault) {
        PhotoFragment fragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_PATH, photoPath);
        args.putBoolean(ARG_IS_VAULT, isVault);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView imageView = view.findViewById(R.id.fullScreenImageView);

        if (getArguments() != null) {
            String photoPath = getArguments().getString(ARG_PHOTO_PATH);
            boolean isVault = getArguments().getBoolean(ARG_IS_VAULT);

            Bitmap bitmap = null;
            if (isVault) {
                // You must have a way to decrypt the photo for display
                bitmap = CryptoUtils.decryptPhoto(photoPath, getContext());
            } else {
                // Logic for non-vault photos
                bitmap = CryptoUtils.loadNormalPhoto(photoPath);
            }

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_error); // Show error icon
            }
        }
    }
}
