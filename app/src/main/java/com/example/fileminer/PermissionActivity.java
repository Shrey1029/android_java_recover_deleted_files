package com.example.fileminer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class PermissionActivity extends Activity {

    private static final int REQUEST_CODE = 2296;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Agar permission already mil chuki hai, toh sidha MainActivity2 open karo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            openMainActivity();
            return;
        }

        // Layout set karo agar permission nahi mili hai
        setContentView(R.layout.activity_permission);

        // TextView ko reference karo
        TextView permissionText = findViewById(R.id.permissionText);

        // Text set karo aur "All Files Access" ko red color do
        String text = "\"All Files Access\" permission is required to search for files.";
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new ForegroundColorSpan(Color.RED), 1, 17, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        permissionText.setText(spannable);

        Button allowButton = findViewById(R.id.allowButton);
        allowButton.setOnClickListener(v -> requestStoragePermission());
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent, REQUEST_CODE);
                    } else {
                        openMainActivity();
                    }
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Error: Unable to open settings!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else {
                openMainActivity();
            }
        } else {
            openMainActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                    openMainActivity();
                } else {
                    Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(PermissionActivity.this, MainActivity2.class);
        startActivity(intent);
        finish();
    }
}
