package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Environment;
import android.os.Handler;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.provider.Settings;
import android.net.Uri;
import android.os.Build;

public class MainActivity2 extends Activity {
    private TextView usedStorage, freeStorage, progressText;
    private ProgressBar storageProgress;
    private Button btnPhoto, btnVideo, btnAudio, btnDocument, btnRecycle, btnhidden;
    private Button btnOtherFiles , manageduplicate;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 101;

    private int lastProgress = 0; // Track the last progress for restoration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(this, PermissionActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main2);

        Log.d("ThemeTest", "App theme applied: " + AppCompatDelegate.getDefaultNightMode());

        initViews();

        if (savedInstanceState != null) {
            lastProgress = savedInstanceState.getInt("saved_progress", 0);
            storageProgress.setProgress(lastProgress);
            progressText.setText(lastProgress + "%");
        }

        if (checkStoragePermission()) {
            displayStorageInfo();
        } else {
            requestStoragePermission();
        }

        setupButtonListeners();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("saved_progress", lastProgress);
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                displayStorageInfo();
            } else {
                showToast("Storage permission denied");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    displayStorageInfo();
                } else {
                    showToast("Storage permission denied");
                }
            }
        }
    }

    private void initViews() {
        usedStorage = findViewById(R.id.usedStorage);
        freeStorage = findViewById(R.id.freeStorage);
        storageProgress = findViewById(R.id.storageProgress);
        progressText = findViewById(R.id.progressText);

        btnPhoto = findViewById(R.id.btnPhoto);
        btnVideo = findViewById(R.id.btnVideo);
        btnAudio = findViewById(R.id.btnAudio);
        btnDocument = findViewById(R.id.btnDocument);
        btnRecycle = findViewById(R.id.btnRecycle);
        btnhidden = findViewById(R.id.btnHidden);
        btnOtherFiles = findViewById(R.id.btnOtherFiles);
    }

    private void displayStorageInfo() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long totalBytes = stat.getTotalBytes();
        long freeBytes = stat.getAvailableBytes();
        long usedBytes = totalBytes - freeBytes;

        int usedPercent = (int) ((usedBytes * 100) / totalBytes);

        usedStorage.setText(String.format("%.2f GB Used", bytesToGb(usedBytes)));
        freeStorage.setText(String.format("%.2f GB Free", bytesToGb(freeBytes)));
        animateProgress(usedPercent);
    }

    private void animateProgress(int targetProgress) {
        Handler handler = new Handler();
        new Thread(() -> {
            int currentProgress = storageProgress.getProgress();

            if (currentProgress < targetProgress) {
                while (currentProgress < targetProgress) {
                    currentProgress++;
                    int finalProgress = currentProgress;
                    lastProgress = finalProgress;
                    handler.post(() -> {
                        storageProgress.setProgress(finalProgress);
                        progressText.setText(finalProgress + "%");
                    });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                while (currentProgress > targetProgress) {
                    currentProgress--;
                    int finalProgress = currentProgress;
                    lastProgress = finalProgress;
                    handler.post(() -> {
                        storageProgress.setProgress(finalProgress);
                        progressText.setText(finalProgress + "%");
                    });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private double bytesToGb(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    private void setupButtonListeners() {
        btnPhoto.setOnClickListener(v -> navigateToCategory("Photo"));
        btnVideo.setOnClickListener(v -> navigateToCategory("Video"));
        btnAudio.setOnClickListener(v -> navigateToCategory("Audio"));
        btnDocument.setOnClickListener(v -> navigateToCategory("Document"));
        btnRecycle.setOnClickListener(v -> navigateToCategory("Deleted"));
        btnhidden.setOnClickListener(v -> navigateToCategory("Hidden"));
        btnOtherFiles.setOnClickListener(v -> navigateToCategory("OtherFiles"));
//      btnhidden.setOnClickListener(v -> navigateToHiddenFiles());
  //      btnOtherFiles.setOnClickListener(v -> navigateotherdata());
    }

    private void navigateToCategory(String category) {
        Intent intent = new Intent(MainActivity2.this, RestoredFilesActivity.class);
        intent.putExtra("fileType", category);
        startActivity(intent);
    }

//    private void navigateToRecycleBin() {
//        Intent intent = new Intent(MainActivity2.this, AllDataRecovery.class);
//        startActivity(intent);
//    }

//    private void navigateToHiddenFiles() {
//        Intent intent = new Intent(MainActivity2.this, HiddenFilesActivity.class);
//        startActivity(intent);
//    }

//    private void navigateotherdata(){
//        Intent intent = new Intent(MainActivity2.this, OtherFilesActivity.class);
//        startActivity(intent);
//    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
