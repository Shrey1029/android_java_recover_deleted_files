package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.fileminer.databinding.ActivityMain2Binding;

import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private ActivityMain2Binding binding;
    private StorageViewModel viewModel;

    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 101;
    private static final String[] FILE_TYPES = {"Photo", "Video", "Audio", "Document", "Deleted", "Hidden", "OtherFiles"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);

        // Check if storage permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(new Intent(this, PermissionUtils.class));
            finish();
            return;
        }

        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(StorageViewModel.class);

        restoreProgress(savedInstanceState);

        // Check and request permission if needed
        if (PermissionUtils.checkStoragePermission(this)) {
            displayStorageInfo();
        } else {
            PermissionUtils.requestStoragePermission(this);
        }

        setupButtonListeners();
    }

    private void restoreProgress(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            viewModel.lastProgress = savedInstanceState.getInt("saved_progress", 0);
        }
        binding.storageProgress.setProgress(viewModel.lastProgress);
        binding.progressText.setText(viewModel.lastProgress + "%");
    }

    private void setupButtonListeners() {
        binding.btnPhoto.setOnClickListener(v -> navigateToCategory(FILE_TYPES[0]));
        binding.btnVideo.setOnClickListener(v -> navigateToCategory(FILE_TYPES[1]));
        binding.btnAudio.setOnClickListener(v -> navigateToCategory(FILE_TYPES[2]));
        binding.btnDocument.setOnClickListener(v -> navigateToCategory(FILE_TYPES[3]));
        binding.btnRecycle.setOnClickListener(v -> navigateToCategory(FILE_TYPES[4]));
        binding.btnHidden.setOnClickListener(v -> navigateToCategory(FILE_TYPES[5]));
        binding.btnOtherFiles.setOnClickListener(v -> navigateToCategory(FILE_TYPES[6]));
    }

    private void navigateToCategory(String category) {
        Intent intent = new Intent(MainActivity2.this, RestoredFilesActivity.class);
        intent.putExtra("fileType", category);
        startActivity(intent);
    }

    private void displayStorageInfo() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long totalBytes = stat.getTotalBytes();
        long freeBytes = stat.getAvailableBytes();
        long usedBytes = totalBytes - freeBytes;

        int usedPercent = (int) ((usedBytes * 100) / totalBytes);

        binding.usedStorage.setText(String.format(Locale.getDefault(), "%.2f GB Used", bytesToGb(usedBytes)));
        binding.freeStorage.setText(String.format(Locale.getDefault(),"%.2f GB Free", bytesToGb(freeBytes)));
        animateProgress(usedPercent);
    }

    private void animateProgress(int targetProgress) {
        Handler handler = new Handler();
        int currentProgress = binding.storageProgress.getProgress();
        Runnable progressUpdater = new Runnable() {
            int progress = currentProgress;

            @Override
            public void run() {
                if (progress != targetProgress) {
                    progress += progress < targetProgress ? 1 : -1;
                    viewModel.lastProgress = progress;
                    binding.storageProgress.setProgress(progress);
                    binding.progressText.setText(progress + "%");
                    handler.postDelayed(this, 10);
                }
            }
        };
        handler.post(progressUpdater);
    }

    private double bytesToGb(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("saved_progress", viewModel.lastProgress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                displayStorageInfo();
            } else {
                showToast("Storage permission denied");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            displayStorageInfo();
        } else {
            showToast("Storage permission denied");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
