package com.example.fileminer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AllDataRecovery extends AppCompatActivity implements ToolbarUpdateListener, FileDeleteListener{

    GridView listView;
    ProgressBar progressBar;
    List<File> deletedFiles;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int MAX_FILES = 500;

    //--------------
    private MediaAdapter adapter;
    private ArrayList<MediaItem> restoredFiles = new ArrayList<>();
    private List<MediaItem> fullMediaItemList = new ArrayList<>();

    private String currentSort = "time";
    private boolean isAscending = false;

    private ArrayList<MediaItem> selectedFiles;

    private String selectedSearchType = "Contains";
    private List<String> fileList = new ArrayList<>();
    TextView noResultsText;
    private boolean isCaseSensitive = false;
    private boolean showPath = false;
    private List<String> excludedFolders = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>();
    private boolean isShowingDuplicates = false;
    private List<MediaItem> duplicateList = new ArrayList<>();
    private List<MediaItem> currentFilteredBaseList = new ArrayList<>();


    private String currentQuery = "";

    Toolbar selectionToolbar;

    String fileType = "Deleted";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_all_data_recovery);


        listView = findViewById(R.id.deletedFilesGridView);
        progressBar = findViewById(R.id.progressBar);

        //-----------
        restoredFiles = new ArrayList<>();
        noResultsText = findViewById(R.id.noResultsText);
        restoredFiles = new ArrayList<>();
        selectedFiles = new ArrayList<>();
        fullMediaItemList = new ArrayList<>();

        // ------ toolbar below app bar
        selectionToolbar = findViewById(R.id.selectionToolbar);
        selectionToolbar.inflateMenu(R.menu.selection_menu);
        selectionToolbar.setTitleTextColor(getResources().getColor(android.R.color.black));


        selectionToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.deleteSelected) {
                selectionToolbar.setTitle("Delete Files");
                deleteSelectedFiles();
                return true;

            } else if (id == R.id.moveSelected) {
                selectionToolbar.setTitle("Move Files");
                moveSelectedFiles();
                return true;

            } else if (id == R.id.selectAll) {
                boolean selectAll = !item.isChecked();
                item.setChecked(selectAll);
                item.setTitle(selectAll ? "Deselect All File" : "Select All File");

                selectionToolbar.setTitle(selectAll ? "All Files Selected" : "Select Files");

                selectAllFiles(selectAll);
                adapter.notifyDataSetChanged();
                return true;
            }

            return false;
        });


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

        listView.setOnItemClickListener((parent, view, position, id) -> {
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
        listView.setVisibility(View.GONE);

        new Thread(() -> {
            deletedFiles = new ArrayList<>();
            Set<String> seenPaths = new HashSet<>();

            File rootDir = Environment.getExternalStorageDirectory();
            searchTrashedFiles(rootDir, 0, seenPaths);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);

                if (deletedFiles.isEmpty()) {
                    Toast.makeText(this, "No deleted files found!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, deletedFiles.size() + " deleted files found!", Toast.LENGTH_SHORT).show();
                }

                //---change in this
                restoredFiles.clear();
                fullMediaItemList.clear();

                for (File file : deletedFiles) {
                    MediaItem item = new MediaItem(file.getName(), file.getAbsolutePath());
                    restoredFiles.add(item);
                    fullMediaItemList.add(item);
                }

                sortFiles(); // sort after scan
                adapter = new MediaAdapter(this, restoredFiles, AllDataRecovery.this , this);
                listView.setAdapter(adapter);
            });

        }).start();
    }

    private void searchTrashedFiles(File dir, int depth, Set<String> seenPaths) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || depth > 10 || deletedFiles.size() >= MAX_FILES)
            return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String absPath = file.getAbsolutePath();
            if (seenPaths.contains(absPath)) continue;
            seenPaths.add(absPath);

            if (file.isDirectory()) {
                searchTrashedFiles(file, depth + 1, seenPaths);
            } else if (isDeletedFile(file)) {
                deletedFiles.add(file);
            }
        }
    }

    private boolean isTrashFolder(File file) {
        String name = file.getName().toLowerCase();
        return name.startsWith(".trashed-") || name.startsWith(".trashed") || name.equals(".recycle") || name.equals(".trash") || name.equals("_.trashed");

    }

    private boolean isDeletedFile(File file) {
        File parentDir = file.getParentFile();
        return parentDir != null && isTrashFolder(parentDir) || file.getName().startsWith(".trashed-");
    }

    private void openFile(File file) {
        try {
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type = "/";

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

    //===============================Featurs Call===========================
    public void updateSelectionToolbar() {
        AllFeaturesUtils.updateSelectionToolbar(restoredFiles, selectionToolbar);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sortByName) {
            currentSort = "name";
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (id == R.id.sortBySize) {
            currentSort = "size";
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (id == R.id.sortByTime) {
            currentSort = "time";
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (id == R.id.sortOrderToggle) {
            isAscending = !isAscending;
            item.setTitle(isAscending ? "Ascending" : "Descending");
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (id == R.id.hideDuplicates) {
            hideDuplicates();
            return true;
        } else if (id == R.id.showOnlyDuplicates) {
            showOnlyDuplicates();
            return true;
        } else if (id == R.id.showPathToggle) {
            showPath = !item.isChecked();
            item.setChecked(showPath);
            adapter.setShowPath(showPath);
            return true;
        } else if (id == R.id.action_filter) {
            loadFileList(); // Reload original file list

            SearchBottomSheet bottomSheet = new SearchBottomSheet(
                    this,
                    selectedSearchType,
                    isCaseSensitive,
                    excludedFolders,
                    excludedExtensions,
                    fileType, // ✅ Pass the fileType like "Photo", "Video", etc.
                    new SearchBottomSheet.OnSearchOptionSelectedListener() {
                        @Override
                        public void onSearchOptionSelected(String searchType, boolean caseSensitive,
                                                           List<String> folders, List<String> extensions) {
                            selectedSearchType = searchType;
                            isCaseSensitive = caseSensitive;
                            excludedFolders = folders;
                            excludedExtensions = extensions;

                            // ✅ Apply both folder + extension exclusions
                            filterFiles(currentQuery, excludedFolders, excludedExtensions);
                        }
                    }
            );

            bottomSheet.show(getSupportFragmentManager(), "SearchBottomSheet");
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private void sortFiles() {
        AllFeaturesUtils.sortFiles(restoredFiles, currentSort, isAscending);

    }

    private void filterFiles(String query, List<String> excludedFolders, List<String> excludedExtensions) {
        List<MediaItem> baseList;
        if (currentFilteredBaseList != null && !currentFilteredBaseList.isEmpty()) {
            baseList = new ArrayList<>(currentFilteredBaseList);
        } else if (isShowingDuplicates) {
            baseList = new ArrayList<>(duplicateList);
        } else {
            baseList = new ArrayList<>(fullMediaItemList);
        }

        AllFeaturesUtils.filterFiles(
                query,
                excludedFolders,
                excludedExtensions,
                baseList,
                restoredFiles,
                isCaseSensitive,
                selectedSearchType,
                noResultsText,
                listView,
                adapter,
                this::sortFiles
        );

    }

    private void loadFileList() {
        AllFeaturesUtils.loadFileList(restoredFiles, fileList);
    }


    //-----------permanently Files Delete
    public void deleteFile(MediaItem item) {
      new AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to *permanently* delete this file?")
            .setPositiveButton("Yes, Delete", (dialog, which) -> {
                File file = new File(item.path);
                if (file.exists() && file.delete()) {
                    restoredFiles.removeIf(mediaItem -> mediaItem.path.equals(item.path));
                    fullMediaItemList.removeIf(mediaItem -> mediaItem.path.equals(item.path));

                    // Update UI
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "File permanently deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("No", null)
            .show();
      }
     private void deleteSelectedFiles() {
        // Show confirmation dialog first
        new AlertDialog.Builder(this)
                .setTitle("Delete Selected Files")
                .setMessage("Are you sure you want to permanently delete the selected files?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {

                    ArrayList<MediaItem> itemsToDelete = new ArrayList<>();

                    for (MediaItem item : restoredFiles) {
                        if (item.isSelected()) {
                            File file = new File(item.path);
                            if (file.exists() && file.delete()) {
                                itemsToDelete.add(item);
                                fullMediaItemList.removeIf(mediaItem -> mediaItem.path.equals(item.path));
                            } else {
                                Toast.makeText(this, "Failed to delete: " + item.name, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    if (!itemsToDelete.isEmpty()) {
                        restoredFiles.removeAll(itemsToDelete);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Selected files permanently deleted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No files were deleted.", Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    // ============= Select All or Deselect All
    private void selectAllFiles(boolean select) {
        AllFeaturesUtils.selectAllFiles(fullMediaItemList, select);
        updateSelectionToolbar();
    }


    private void moveSelectedFiles() {
        AllFeaturesUtils.moveSelectedFiles(
                this,
                fullMediaItemList,
                this::updateSelectionToolbar,
                this::loadFileList
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);

        AllFeaturesUtils.setupSearch(menu, this, query -> {
            currentQuery = query;
            filterFiles(query, excludedFolders, excludedExtensions);
        });

        return true;
    }

    private void hideDuplicates() {
        AllFeaturesUtils.hideDuplicates(
                this,
                fullMediaItemList,
                currentFilteredBaseList,
                restoredFiles,
                this::sortFiles,
                adapter
        );
    }

    private void showOnlyDuplicates() {
        AllFeaturesUtils.showOnlyDuplicates(
                this,
                fullMediaItemList,
                currentFilteredBaseList,
                restoredFiles,
                duplicateList,
                this::sortFiles,
                adapter
        );
    }
}
