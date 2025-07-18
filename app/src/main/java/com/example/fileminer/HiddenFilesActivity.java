//package com.example.fileminer;
//
//import android.Manifest;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.provider.Settings;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.EditText;
//import android.widget.GridView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.app.AppCompatDelegate;
//import androidx.appcompat.widget.SearchView;
//import androidx.appcompat.widget.Toolbar;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.content.FileProvider;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.security.MessageDigest;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//public class HiddenFilesActivity extends AppCompatActivity implements ToolbarUpdateListener, FileDeleteListener {
//
//    private static final int STORAGE_PERMISSION_CODE = 101;
//    private static final int MANAGE_STORAGE_PERMISSION_CODE = 102;
//    private GridView listView;
//    private MediaAdapter adapter;
//    private final List<String> hiddenFilesList = new ArrayList<>();
//
//    //---------------------------
//    private ArrayList<MediaItem> restoredFiles = new ArrayList<>();
//    private List<MediaItem> fullMediaItemList = new ArrayList<>();
//
//    private String currentSort = "time";
//    private boolean isAscending = false;
//
//    private ArrayList<MediaItem> selectedFiles;
//
//    private String selectedSearchType = "Contains";
//    private List<String> fileList = new ArrayList<>();
//    TextView noResultsText;
//    private boolean isCaseSensitive = false;
//    private boolean showPath = false;
//    private List<String> excludedFolders = new ArrayList<>();
//    private List<String> excludedExtensions = new ArrayList<>();
//    private boolean isShowingDuplicates = false;
//    private List<MediaItem> duplicateList = new ArrayList<>();
//    private List<MediaItem> currentFilteredBaseList = new ArrayList<>();
//
//
//    private String currentQuery = "";
//
//    Toolbar selectionToolbar;
//    String fileType = "Hidden";
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
//
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.activity_hidden_files);
//
//        listView = findViewById(R.id.hiddenFilesGridView);
//
//        //  --------
//        restoredFiles = new ArrayList<>();
//        noResultsText = findViewById(R.id.noResultsText);
//        restoredFiles = new ArrayList<>();
//        selectedFiles = new ArrayList<>();
//        fullMediaItemList = new ArrayList<>();
//
//
//        //-----------------toolbar below app bar
//        selectionToolbar = findViewById(R.id.selectionToolbar);
//        selectionToolbar.inflateMenu(R.menu.selection_menu);
//        selectionToolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
//
//
//        selectionToolbar.setOnMenuItemClickListener(item -> {
//            int id = item.getItemId();
//
//            if (id == R.id.deleteSelected) {
//                selectionToolbar.setTitle("Delete Files");
//                deleteSelectedFiles();
//                return true;
//
//            } else if (id == R.id.moveSelected) {
//                selectionToolbar.setTitle("Move Files");
//                moveSelectedFiles();
//                return true;
//
//            } else if (id == R.id.selectAll) {
//                boolean selectAll = !item.isChecked();
//                item.setChecked(selectAll);
//                item.setTitle(selectAll ? "Deselect All File" : "Select All File");
//
//                selectionToolbar.setTitle(selectAll ? "All Files Selected" : "Select Files");
//
//                selectAllFiles(selectAll);
//                adapter.notifyDataSetChanged();
//                return true;
//            }
//
//            return false;
//        });
//
//
//        adapter = new MediaAdapter(HiddenFilesActivity.this, restoredFiles, this, this);
//        listView.setAdapter(adapter);
//
//        showHiddenFiles();
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (!Environment.isExternalStorageManager()) {
//                requestManageStoragePermission();
//            } else {
//                showHiddenFiles();
//            }
//        } else {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
//            } else {
//                showHiddenFiles();
//            }
//        }
//
//        listView.setOnItemClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
//            openFile(hiddenFilesList.get(position));
//        });
//    }
//
//    private void requestManageStoragePermission() {
//        try {
//            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//            intent.setData(Uri.parse("package:" + getPackageName()));
//            startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
//        } catch (Exception e) {
//            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//            startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
//        }
//    }
//
//    private void showHiddenFiles() {
//        File directory = Environment.getExternalStorageDirectory();
//        if (directory != null && directory.exists()) {
//            hiddenFilesList.clear();
//            scanHiddenFolders(directory);
//        }
//
//        if (hiddenFilesList.isEmpty()) {
//            Toast.makeText(this, "No hidden photos or videos found", Toast.LENGTH_SHORT).show();
//        } else {
//            restoredFiles.clear();
//            for (String path : hiddenFilesList) {
//                File file = new File(path);
//                String name = file.getName();
//                long size = file.length();
//                long modified = file.lastModified();
//
//                MediaItem item = new MediaItem(name, path);
//                item.size = size;
//                item.dateModified = modified;
//
//                restoredFiles.add(item);
//            }
//            fullMediaItemList.clear();
//            fullMediaItemList.addAll(restoredFiles);
//
//            sortFiles();  // Sort current list
//
//            adapter = new MediaAdapter(this, restoredFiles, this, this);
//            listView.setAdapter(adapter);
//            adapter.notifyDataSetChanged();
//        }
//    }
//
//    private void scanHiddenFolders(File directory) {
//        if (directory == null || !directory.exists() || !directory.canRead()) {
//            Log.e("HiddenFiles", "Cannot access directory: " + (directory != null ? directory.getAbsolutePath() : "null"));
//            return;
//        }
//
//        File[] files = directory.listFiles();
//        if (files != null) {
//            for (File file : files) {
//                if (file.isDirectory() && file.getName().startsWith(".")) {
//                    Log.d("HiddenFiles", "Hidden folder found: " + file.getAbsolutePath());
//                    listHiddenFiles(file);
//                }
//            }
//        }
//    }
//
//    private void listHiddenFiles(File folder) {
//        if (folder == null || !folder.exists() || !folder.canRead()) {
//            Log.e("HiddenFiles", "Cannot read folder: " + (folder != null ? folder.getAbsolutePath() : "null"));
//            return;
//        }
//
//        File[] files = folder.listFiles();
//        if (files == null) {
//            Log.e("HiddenFiles", "Files list is null for: " + folder.getAbsolutePath());
//            return;
//        }
//
//        for (File file : files) {
//            if (file.isFile() && isPhotoOrVideo(file)) {
//                hiddenFilesList.add(file.getAbsolutePath());
//                Log.d("HiddenFiles", "Hidden photo or video found: " + file.getAbsolutePath());
//            }
//        }
//    }
//
//    private boolean isPhotoOrVideo(File file) {
//        if (file == null || !file.exists()) return false;
//        String[] photoExtensions = {
//                ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff"
//        };
//
//        String[] videoExtensions = {
//                ".mp4", ".mkv", ".avi", ".mov", ".flv" ,  ".pdf", ".ppt", ".pptx", ".odt", ".doc", ".docx", ".xls", ".xlsx"
//        };
//
//        String fileName = file.getName().toLowerCase();
//
//        for (String ext : photoExtensions) {
//            if (fileName.endsWith(ext)) return true;
//        }
//        for (String ext : videoExtensions) {
//            if (fileName.endsWith(ext)) return true;
//        }
//        return false;
//    }
//
//    private void openFile(String filePath) {
//        File file = new File(filePath);
//        if (!file.exists()) {
//            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        try {
//            Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            String mimeType = getMimeType(filePath);
//
//            intent.setDataAndType(fileUri, mimeType);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            startActivity(intent);
//        } catch (Exception e) {
//            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
//            Log.e("HiddenFiles", "Error opening file: ", e);
//        }
//    }
//
//    private String getMimeType(String fileName) {
//        fileName = fileName.toLowerCase();
//
//        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
//                fileName.endsWith(".png") || fileName.endsWith(".gif") ||
//                fileName.endsWith(".bmp") || fileName.endsWith(".webp") ||
//                fileName.endsWith(".tiff") || fileName.endsWith(".svg")) {
//            return "image/*";
//        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv")) {
//            return "video/*";
//        } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a")) {
//            return "audio/*";
//        } else if (fileName.endsWith(".pdf")) {
//            return "application/pdf";
//        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
//            return "application/msword";
//        } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
//            return "application/vnd.ms-powerpoint";
//        } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
//            return "application/vnd.ms-excel";
//        } else if (fileName.endsWith(".odt")) {
//            return "application/vnd.oasis.opendocument.text";
//        }
//
//        return "*/*";
//    }
//
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == STORAGE_PERMISSION_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                showHiddenFiles();
//            } else {
//                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == MANAGE_STORAGE_PERMISSION_CODE) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
//                showHiddenFiles();
//            } else {
//                Toast.makeText(this, "Manage storage permission denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    //============================Featurs Call ================================
//
//public void updateSelectionToolbar() {
//    AllFeaturesUtils.updateSelectionToolbar(restoredFiles, selectionToolbar);
//}
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//
//        if (id == R.id.sortByName) {
//            currentSort = "name";
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        } else if (id == R.id.sortBySize) {
//            currentSort = "size";
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        } else if (id == R.id.sortByTime) {
//            currentSort = "time";
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        } else if (id == R.id.sortOrderToggle) {
//            isAscending = !isAscending;
//            item.setTitle(isAscending ? "Ascending" : "Descending");
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        }  else if (id == R.id.hideDuplicates) {
//            hideDuplicates();
//            return true;
//        } else if (id == R.id.showOnlyDuplicates) {
//            showOnlyDuplicates();
//            return true;
//        } else if (id == R.id.showPathToggle) {
//            showPath = !item.isChecked();
//            item.setChecked(showPath);
//            adapter.setShowPath(showPath);
//            return true;
//        }
//        else if (id == R.id.action_filter) {
//            loadFileList();
//
//            SearchBottomSheet bottomSheet = new SearchBottomSheet(
//                    this,
//                    selectedSearchType,
//                    isCaseSensitive,
//                    excludedFolders,
//                    excludedExtensions,
//                    fileType, // Pass the fileType like "Photo", "Video", etc.
//                    new SearchBottomSheet.OnSearchOptionSelectedListener() {
//                        @Override
//                        public void onSearchOptionSelected(String searchType, boolean caseSensitive,
//                                                           List<String> folders, List<String> extensions) {
//                            selectedSearchType = searchType;
//                            isCaseSensitive = caseSensitive;
//                            excludedFolders = folders;
//                            excludedExtensions = extensions;
//
//                            filterFiles(currentQuery, excludedFolders, excludedExtensions);
//                        }
//                    }
//            );
//
//            bottomSheet.show(getSupportFragmentManager(), "SearchBottomSheet");
//            return true;
//        }
//
//
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void sortFiles() {
//        AllFeaturesUtils.sortFiles(restoredFiles, currentSort, isAscending);
//
//    }
//
//    private void filterFiles(String query, List<String> excludedFolders, List<String> excludedExtensions) {
//        List<MediaItem> baseList;
//        if (currentFilteredBaseList != null && !currentFilteredBaseList.isEmpty()) {
//            baseList = new ArrayList<>(currentFilteredBaseList);
//        } else if (isShowingDuplicates) {
//            baseList = new ArrayList<>(duplicateList);
//        } else {
//            baseList = new ArrayList<>(fullMediaItemList);
//        }
//
//        AllFeaturesUtils.filterFiles(
//                query,
//                excludedFolders,
//                excludedExtensions,
//                baseList,
//                restoredFiles,
//                isCaseSensitive,
//                selectedSearchType,
//                noResultsText,
//                listView,
//                adapter,
//                this::sortFiles
//        );
//
//    }
//
//
//    private void loadFileList() {
//        AllFeaturesUtils.loadFileList(restoredFiles, fileList);
//    }
//
//    @Override
//    public void deleteFile(MediaItem item) {
//        AllFeaturesUtils.deleteFile(
//                this,
//                item,
//                restoredFiles,
//                fullMediaItemList,
//                adapter,
//                file -> moveToTrash(file)
//        );
//
//    }
//
//    private void deleteSelectedFiles() {
//        AllFeaturesUtils.deleteSelectedFiles(
//                this,
//                restoredFiles,
//                fullMediaItemList,
//                adapter,
//                file -> moveToTrash(file)
//        );
//
//    }
//
//    private boolean moveToTrash(File file) {
//        File trashDir = new File(Environment.getExternalStorageDirectory(), "_.trashed");
//        if (!trashDir.exists()) trashDir.mkdirs();
//
//        File destFile = new File(trashDir, file.getName());
//        return file.renameTo(destFile);
//    }
//
//
//    // ============= Select All or Deselect All
//    private void selectAllFiles(boolean select) {
//        AllFeaturesUtils.selectAllFiles(fullMediaItemList, select);
//        updateSelectionToolbar();
//    }
//
//
//    private void moveSelectedFiles() {
//        AllFeaturesUtils.moveSelectedFiles(
//                this,
//                fullMediaItemList,
//                this::updateSelectionToolbar,
//                this::loadFileList
//        );
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.sort_menu, menu);
//
//        AllFeaturesUtils.setupSearch(menu, this, query -> {
//            currentQuery = query;
//            filterFiles(query, excludedFolders, excludedExtensions);
//        });
//
//        return true;
//    }
//
//    private void hideDuplicates() {
//        AllFeaturesUtils.hideDuplicates(
//                this,
//                fullMediaItemList,
//                currentFilteredBaseList,
//                restoredFiles,
//                this::sortFiles,
//                adapter
//        );
//    }
//
//    private void showOnlyDuplicates() {
//        AllFeaturesUtils.showOnlyDuplicates(
//                this,
//                fullMediaItemList,
//                currentFilteredBaseList,
//                restoredFiles,
//                duplicateList,
//                this::sortFiles,
//                adapter
//        );
//    }
//}