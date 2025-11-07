// Create a new file: PhotoPagerAdapter.java
package com.example.sd_contextcam;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;

public class PhotoPagerAdapter extends FragmentStateAdapter {

    private final List<String> photoPaths;
    private final boolean isVault;

    public PhotoPagerAdapter(FragmentActivity fa, List<String> photoPaths, boolean isVault) {
        super(fa);
        this.photoPaths = photoPaths;
        this.isVault = isVault;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Create a new fragment instance for the photo at the given position
        return PhotoFragment.newInstance(photoPaths.get(position), isVault);
    }

    @Override
    public int getItemCount() {
        return photoPaths.size();
    }
}
