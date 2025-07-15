package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

public class PermissionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        // ====================== Set image based on night mode
        ImageView imageView = findViewById(R.id.imageView);
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            imageView.setImageResource(R.drawable.filetwo); // Dark mode image
        } else {
            imageView.setImageResource(R.drawable.filetwo); // Light mode image
        }

        //=================== Set styled text with red highlight
        TextView permissionText = findViewById(R.id.permissionText);
        String text = "\"All Files Access\" permission is required to search for files.";
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new ForegroundColorSpan(Color.RED), 1, 17, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        permissionText.setText(spannable);

        //================= Allow button logic
        Button allowButton = findViewById(R.id.allowButton);
        allowButton.setOnClickListener(v -> openPermissionSettings());
    }

    private void openPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                //Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                openMainActivity();
            }
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(PermissionActivity.this, MainActivity2.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Please grant permission to proceed.", Toast.LENGTH_SHORT).show();
    }
}
