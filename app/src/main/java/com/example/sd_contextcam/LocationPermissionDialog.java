package com.example.sd_contextcam;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class LocationPermissionDialog extends DialogFragment {
    public interface OnPermissionResultListener {
        void onPermissionResult(boolean granted);
    }
    
    private OnPermissionResultListener permissionResultListener;
    
    public void setOnPermissionResultListener(OnPermissionResultListener listener) {
        this.permissionResultListener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Location Permission");
        builder.setMessage("To tag photos automatically based on location, you can grant optional location permission.");
        
        builder.setPositiveButton("Allow While Using App", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (permissionResultListener != null) {
                    permissionResultListener.onPermissionResult(true);
                }
            }
        });
        
        builder.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (permissionResultListener != null) {
                    permissionResultListener.onPermissionResult(false);
                }
            }
        });
        
        return builder.create();
    }
}