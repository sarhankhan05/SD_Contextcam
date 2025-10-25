package com.example.sd_contextcam;// <--- IMPORTANT: Change this to your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnGrantAccess = findViewById(R.id.btn_grant_access);
        btnGrantAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When "Grant Access & Begin" is clicked, start the PermissionsActivity
                Intent intent = new Intent(WelcomeActivity.this, PermissionsActivity.class);
                startActivity(intent);
                // Optionally finish WelcomeActivity if you don't want to return to it
                // finish();
            }
        });
    }
}