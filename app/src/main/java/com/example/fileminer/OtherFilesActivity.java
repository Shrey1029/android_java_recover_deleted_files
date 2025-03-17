package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OtherFilesActivity extends Activity {

    private GridView gridOtherFiles;
    private ProgressBar progressBar;
    private List<File> otherFiles;
    private GridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_files);

        gridOtherFiles = findViewById(R.id.gridOtherFiles);
        progressBar = findViewById(R.id.progressBar);

        fetchOtherFiles();

        gridOtherFiles.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) ->
                openFile(otherFiles.get(position))
        );
    }

    private void fetchOtherFiles() {
        // Show ProgressBar, Hide GridView
        progressBar.setVisibility(View.VISIBLE);
        gridOtherFiles.setVisibility(View.GONE);

        new Thread(() -> {
            File directory = new File("/storage/emulated/0/");
            otherFiles = new ArrayList<>();

            searchFiles(directory); // Fetch files recursively

            runOnUiThread(() -> {
                adapter = new GridAdapter(OtherFilesActivity.this, otherFiles);
                gridOtherFiles.setAdapter(adapter);

                // Hide ProgressBar, Show GridView
                progressBar.setVisibility(View.GONE);
                gridOtherFiles.setVisibility(View.VISIBLE);
            });

        }).start();
    }

    private void searchFiles(File dir) {
        try {
            if (dir != null && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && !isExcludedFileType(file)) {
                            otherFiles.add(file);
                        } else if (file.isDirectory() && file.canRead()) {
                            searchFiles(file); // Recursively scan subdirectories
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Exclude images, videos, PDFs, and audio files
    private boolean isExcludedFileType(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") ||
                name.endsWith(".pdf") ||
                name.endsWith(".mp3") || name.endsWith(".wav") ||
                name.endsWith(".odt") || name.endsWith(".pptx")||
                name.endsWith(".doc") || name.endsWith(".docx");
    }

    private void openFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, "com.example.fileminer.provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, getMimeType(file.getAbsolutePath()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private String getMimeType(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        return extension != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : "*/*";
    }
}
