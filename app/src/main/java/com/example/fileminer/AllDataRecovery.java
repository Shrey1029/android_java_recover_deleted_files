package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AllDataRecovery extends Activity {

    GridView deletedFilesGridView;
    ProgressBar progressBar;
    FileAdapter fileAdapter;
    List<File> deletedFiles;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int MAX_FILES = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_data_recovery);

        deletedFilesGridView = findViewById(R.id.deletedFilesGridView);
        progressBar = findViewById(R.id.progressBar); // Initialize ProgressBar

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestFilePermission();
            } else {
                startFileScan();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                startFileScan();
            }
        }

        deletedFilesGridView.setOnItemClickListener((parent, view, position, id) -> {
            File file = deletedFiles.get(position);
            openFile(file);
        });
    }

    private void requestFilePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open permission settings!", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startFileScan();
            } else {
                Toast.makeText(this, "Storage permission is required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startFileScan() {
        progressBar.setVisibility(View.VISIBLE);
        deletedFilesGridView.setVisibility(View.GONE);
        deletedFiles = new ArrayList<>();

        new Thread(() -> {
            File rootDir = Environment.getExternalStorageDirectory();
            searchTrashedFiles(rootDir, 0);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                deletedFilesGridView.setVisibility(View.VISIBLE);

                if (deletedFiles.isEmpty()) {
                    Toast.makeText(this, "No deleted files found!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, deletedFiles.size() + " deleted files found!", Toast.LENGTH_SHORT).show();
                }

                fileAdapter = new FileAdapter(this, deletedFiles);
                deletedFilesGridView.setAdapter(fileAdapter);
            });
        }).start();
    }

    private void searchTrashedFiles(File dir, int depth) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || depth > 10 || deletedFiles.size() >= MAX_FILES) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (isTrashFolder(file)) {
                    searchTrashedFiles(file, depth + 1);
                } else {
                    searchTrashedFiles(file, depth + 1);
                }
            } else {
                if (isDeletedFile(file)) {
                    deletedFiles.add(file);
                }
            }
        }
    }

    private boolean isTrashFolder(File file) {
        String name = file.getName().toLowerCase();
        return name.startsWith(".trashed-") || name.equals(".recycle") || name.equals(".trash");
    }

    private boolean isDeletedFile(File file) {
        File parentDir = file.getParentFile();
        return parentDir != null && isTrashFolder(parentDir) || file.getName().startsWith(".trashed-");
    }

    private void openFile(File file) {
        try {
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type = "*/*";

            if (file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg") || file.getName().endsWith(".png")) {
                type = "image/*";
            } else if (file.getName().endsWith(".mp4") || file.getName().endsWith(".avi") || file.getName().endsWith(".mkv")) {
                type = "video/*";
            } else if (file.getName().endsWith(".pdf")) {
                type = "application/pdf";
            } else if (file.getName().endsWith(".mp3") || file.getName().endsWith(".wav")) {
                type = "audio/*";
            }

            intent.setDataAndType(uri, type);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No supported app found to open this file!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
