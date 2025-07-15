package com.example.fileminer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import android.content.DialogInterface; // Import this
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.MessageDigest;
import java.io.FileInputStream;


public class RestoredFilesActivity extends AppCompatActivity {

    private GridView listView;
    private MediaAdapter adapter;
    private ArrayList<MediaItem> restoredFiles;
    private String currentSort = "time";
    private boolean isAscending = false;

    private ArrayList<MediaItem> selectedFiles;
    ProgressBar progressBar;
    private String selectedSearchType = "Contains";
    private List<String> fileList = new ArrayList<>();
    TextView noResultsText;
    private List<MediaItem> fullMediaItemList = new ArrayList<>();
    private boolean isCaseSensitive = false;
    private boolean showPath = false;
    private boolean setSelected = false;

    private List<String> excludedFolders = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>();
    private boolean isShowingDuplicates = false;
    private List<MediaItem> duplicateList = new ArrayList<>();

    private List<MediaItem> currentFilteredBaseList = new ArrayList<>();
    private String currentQuery = "";

    Toolbar selectionToolbar;

    private String fileType = "Photo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_restored_files);

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.gridView);
        noResultsText = findViewById(R.id.noResultsText);

        //-----------Toolbar below appbar
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

        restoredFiles = new ArrayList<>();
        selectedFiles = new ArrayList<>();

        fullMediaItemList = new ArrayList<>();

        adapter = new MediaAdapter(this, restoredFiles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            MediaItem item = restoredFiles.get(position);
            openFile(item.path);
        });

        Intent intent = getIntent();
        fileType = intent.getStringExtra("fileType");

        Log.d("RestoredFilesActivity", "Received fileType: " + fileType);

        if (fileType != null) {
            switch (fileType) {
                case "Photo":
                    new LoadMediaFilesTask().execute(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    break;
                case "Video":
                    new LoadMediaFilesTask().execute(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    break;
                case "Audio":
                    new LoadMediaFilesTask().execute(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    break;
                case "Document":
                    new LoadDocumentFilesTask().execute();
                    break;
                default:
                    new LoadAllFilesTask().execute();
                    break;
            }
        }
    }

    private void openFile(String filePath) {
        if (filePath == null) return;

        File file = new File(filePath);
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, getMimeType(filePath));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e("RestoredFilesActivity", "No suitable app found to open this file", e);
        }
    }

    private String getMimeType(String filePath) {
        if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
            return "video/*";
        } else if (filePath.endsWith(".mp3") || filePath.endsWith(".wav") || filePath.endsWith(".m4a")) {
            return "audio/*";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".png")) {
            return "image/*";
        } else if (filePath.endsWith(".pdf")) {
            return "application/pdf";
        } else if (filePath.endsWith(".doc") || filePath.endsWith(".docx")) {
            return "application/msword";
        } else if (filePath.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (filePath.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        }
        return "/";
    }

    private void sortFiles() {
        if ("name".equals(currentSort)) {
            Collections.sort(restoredFiles, (a, b) -> isAscending ?
                    a.name.compareToIgnoreCase(b.name) :
                    b.name.compareToIgnoreCase(a.name));
        } else if ("size".equals(currentSort)) {
            Collections.sort(restoredFiles, (a, b) -> {
                if (a.size == 0 && b.size == 0) return 0;
                else if (a.size == 0) return isAscending ? 1 : -1;
                else if (b.size == 0) return isAscending ? -1 : 1;
                return isAscending ? Long.compare(a.size, b.size) : Long.compare(b.size, a.size);
            });
        } else if ("time".equals(currentSort)) {
            Collections.sort(restoredFiles, (a, b) ->
                    isAscending ? Long.compare(a.dateModified, b.dateModified) : Long.compare(b.dateModified, a.dateModified));
        }
    }

    private void filterFiles(String query, List<String> excludedFolders, List<String> excludedExtensions) {
        if (query == null) query = "";
        String searchQuery = query.trim();
        List<MediaItem> filteredList = new ArrayList<>();

        List<MediaItem> baseList = new ArrayList<>(currentFilteredBaseList);

        if (!isCaseSensitive) {
            searchQuery = searchQuery.toLowerCase();
        }

        for (MediaItem item : baseList) {
            if (item == null || item.name == null) continue;

            File file = new File(item.path);
            if (!file.exists()) continue;

            String fileName = item.name;
            String filePath = item.path;
            String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";

            if (!isCaseSensitive) {
                fileName = fileName.toLowerCase();
                if (filePath != null) filePath = filePath.toLowerCase();
                extension = extension.toLowerCase();
            }

            if (shouldExclude(item, excludedFolders)) continue;
            if (excludedExtensions.contains(extension)) continue;

            if (searchQuery.isEmpty()) {
                filteredList.add(item);
            } else {
                switch (selectedSearchType) {
                    case "Starts With":
                        if (fileName.startsWith(searchQuery)) filteredList.add(item);
                        break;
                    case "Ends With":
                        if (fileName.endsWith(searchQuery)) filteredList.add(item);
                        break;
                    case "Path":
                        if (filePath != null && filePath.contains(searchQuery)) filteredList.add(item);
                        break;
                    case "Contains":
                    default:
                        if (fileName.contains(searchQuery)) filteredList.add(item);
                        break;
                }
            }
        }

        restoredFiles.clear();
        restoredFiles.addAll(filteredList);
        sortFiles();

        runOnUiThread(() -> {
            if (restoredFiles.isEmpty()) {
                noResultsText.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } else {
                noResultsText.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }


    private boolean shouldExclude(MediaItem item, List<String> excludedFolders) {
        if (excludedFolders == null || excludedFolders.isEmpty()) return false;

        File file = new File(item.path);
        File parentFolder = file.getParentFile();
        if (parentFolder != null) {
            String folderName = parentFolder.getName();
            for (String exclude : excludedFolders) {
                if (folderName.equalsIgnoreCase(exclude)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void loadFileList() {
        fileList.clear();
        for (MediaItem item : restoredFiles) {
            fileList.add(item.name);
        }
    }
    private void updateSelectionToolbar() {
        boolean anySelected = false;
        for (MediaItem item : restoredFiles) {
            if (item.isSelected()) {
                anySelected = true;
                break;
            }
        }

        selectionToolbar.setVisibility(anySelected ? View.VISIBLE : View.GONE);
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
        }  else if (id == R.id.hideDuplicates) {
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

        }
        else if (id == R.id.action_filter) {
            loadFileList();

            SearchBottomSheet bottomSheet = new SearchBottomSheet(
                    this,
                    selectedSearchType,
                    isCaseSensitive,
                    excludedFolders,
                    excludedExtensions,
                    fileType,
                    new SearchBottomSheet.OnSearchOptionSelectedListener() {
                        @Override
                        public void onSearchOptionSelected(String searchType, boolean caseSensitive,
                                                           List<String> folders, List<String> extensions) {
                            selectedSearchType = searchType;
                            isCaseSensitive = caseSensitive;
                            excludedFolders = folders;
                            excludedExtensions = extensions;

                            // =========== Apply both folder + extension exclusions
                            filterFiles(currentQuery, excludedFolders, excludedExtensions);
                        }
                    }
            );

            bottomSheet.show(getSupportFragmentManager(), "SearchBottomSheet");
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private void deleteFile(MediaItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    File file = new File(item.path);
                    if (file.exists() && moveToTrash(file)) {
                        restoredFiles.remove(item);
                        fullMediaItemList.removeIf(mediaItem -> mediaItem.path.equals(item.path));
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "File moved to trash", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


    private void deleteSelectedFiles() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Selected Files")
                .setMessage("Are you sure you want to delete the selected files?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    ArrayList<MediaItem> itemsToDelete = new ArrayList<>();

                    for (MediaItem item : restoredFiles) {
                        if (item.isSelected) {
                            File file = new File(item.path);
                            if (file.exists() && moveToTrash(file)) {
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
                        Toast.makeText(this, "Selected files moved to trash!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No files were deleted.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private boolean moveToTrash(File file) {
        File trashDir = new File(Environment.getExternalStorageDirectory(), "_.trashed");
        if (!trashDir.exists()) trashDir.mkdirs();

        File destFile = new File(trashDir, file.getName());
        return file.renameTo(destFile);
    }


    // ============= Select All or Deselect All
    private void selectAllFiles(boolean select) {
        for (MediaItem item : fullMediaItemList) {
            item.setSelected(select);
        }

        updateSelectionToolbar();
    }

    private void moveSelectedFiles() {
        // Step 1: Get selected files
        List<MediaItem> selectedItems = new ArrayList<>();
        for (MediaItem item : fullMediaItemList) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No files selected to move", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 2: Start folder picker from root (internal storage)
        File rootDir = Environment.getExternalStorageDirectory();  // Show all folders
        openFolderPicker(rootDir, selectedItems);
    }

    private void openFolderPicker(File currentDir, List<MediaItem> selectedItems) {
        File[] subFoldersArr = currentDir.listFiles(File::isDirectory);
        if (subFoldersArr == null) subFoldersArr = new File[0];

        final File[] subFolders = subFoldersArr;

        List<String> options = new ArrayList<>();
        for (File folder : subFolders) {
            options.add(folder.getName());
        }

        options.add("📂 Create New Folder");
        options.add("✅ Select This Folder");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Folder in:\n" + currentDir.getAbsolutePath());
        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
            if (which < subFolders.length) {
                // Navigate into subfolder
                openFolderPicker(subFolders[which], selectedItems);
            } else if (which == subFolders.length) {
                // Create new folder
                showCreateSubfolderDialog(currentDir, selectedItems);
            } else {
                // Move to selected folder
                moveFilesToFolder(selectedItems, currentDir);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCreateSubfolderDialog(File parentFolder, List<MediaItem> selectedItems) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Folder in:\n" + parentFolder.getAbsolutePath());

        final EditText input = new EditText(this);
        input.setHint("Folder name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (!folderName.isEmpty()) {
                File newFolder = new File(parentFolder, folderName);
                if (!newFolder.exists()) {
                    if (newFolder.mkdirs()) {
                        Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show();
                        openFolderPicker(newFolder, selectedItems); // Open new folder
                    } else {
                        Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Folder already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void moveFilesToFolder(List<MediaItem> selectedItems, File destinationFolder) {
        boolean anyFileMoved = false;

        for (MediaItem item : selectedItems) {
            File sourceFile = new File(item.getFilePath());
            File destFile = new File(destinationFolder, sourceFile.getName());

            try {
                if (copyFile(sourceFile, destFile)) {
                    if (sourceFile.delete()) {
                        item.setFilePath(destFile.getAbsolutePath());
                        anyFileMoved = true;
                    } else {
                        Toast.makeText(this, "Copied but failed to delete: " + sourceFile.getName(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Failed to copy: " + sourceFile.getName(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error moving: " + sourceFile.getName(), Toast.LENGTH_SHORT).show();
            }
        }

        if (anyFileMoved) {
            Toast.makeText(this, "Files moved successfully", Toast.LENGTH_SHORT).show();
            loadFileList();
        }
    }
    private boolean copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);

        // Search icon
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search Files...");

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        currentQuery = query;
                        filterFiles(query, excludedFolders, excludedExtensions);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        currentQuery = newText;
                        filterFiles(newText, excludedFolders, excludedExtensions);
                        return true;
                    }
                });

                searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            }
        }

        MenuItem filterItem = menu.findItem(R.id.action_filter);
        if (filterItem != null) {
            filterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        return true;
    }
    private String getFileHash(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(filePath);

            byte[] byteArray = new byte[1024];
            int bytesRead;
            int totalRead = 0;
            int maxBytes = 1024 * 1024;

            while ((bytesRead = fis.read(byteArray)) != -1 && totalRead < maxBytes) {
                digest.update(byteArray, 0, bytesRead);
                totalRead += bytesRead;
            }
            fis.close();

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private class HideDuplicatesTask extends AsyncTask<Void, Void, List<MediaItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(RestoredFilesActivity.this, "Hiding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItem> doInBackground(Void... voids) {
            Map<Long, List<MediaItem>> sizeMap = new HashMap<>();
            for (MediaItem item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Set<String> seenHashes = new HashSet<>();
            List<MediaItem> uniqueFiles = new ArrayList<>();

            for (List<MediaItem> group : sizeMap.values()) {
                if (group.size() == 1) {
                    uniqueFiles.add(group.get(0));
                } else {
                    for (MediaItem item : group) {
                        String hash = getFileHash(item.path);
                        if (hash != null && !seenHashes.contains(hash)) {
                            seenHashes.add(hash);
                            uniqueFiles.add(item);
                        }
                    }
                }
            }

            return uniqueFiles;
        }

        @Override
        protected void onPostExecute(List<MediaItem> result) {
            isShowingDuplicates = false;

            currentFilteredBaseList.clear();
            currentFilteredBaseList.addAll(result);

            restoredFiles.clear();
            restoredFiles.addAll(result);
            sortFiles();
            adapter.notifyDataSetChanged();

            if (result.isEmpty()) {
                Toast.makeText(RestoredFilesActivity.this, "No unique files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RestoredFilesActivity.this, "Duplicates Hidden Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class ShowOnlyDuplicatesTask extends AsyncTask<Void, Void, List<MediaItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(RestoredFilesActivity.this, "Finding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItem> doInBackground(Void... voids) {
            Map<Long, List<MediaItem>> sizeMap = new HashMap<>();
            for (MediaItem item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Map<String, Integer> hashCountMap = new HashMap<>();
            Map<String, MediaItem> hashToItem = new HashMap<>();
            List<MediaItem> duplicates = new ArrayList<>();

            for (List<MediaItem> group : sizeMap.values()) {
                if (group.size() > 1) {
                    for (MediaItem item : group) {
                        String hash = getFileHash(item.path);
                        if (hash != null) {
                            int count = hashCountMap.getOrDefault(hash, 0);
                            hashCountMap.put(hash, count + 1);
                            if (count == 1) {
                                duplicates.add(hashToItem.get(hash));
                                duplicates.add(item);
                            } else if (count > 1) {
                                duplicates.add(item);
                            } else {
                                hashToItem.put(hash, item);
                            }
                        }
                    }
                }
            }

            return duplicates;
        }

        @Override
        protected void onPostExecute(List<MediaItem> result) {
            isShowingDuplicates = true;
            duplicateList.clear();
            duplicateList.addAll(result);

            currentFilteredBaseList.clear();
            currentFilteredBaseList.addAll(result);

            restoredFiles.clear();
            restoredFiles.addAll(result);
            sortFiles();
            adapter.notifyDataSetChanged();

            if (result.isEmpty()) {
                Toast.makeText(RestoredFilesActivity.this, "No duplicate files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RestoredFilesActivity.this, "Showing Only Duplicates Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void hideDuplicates() {
        new HideDuplicatesTask().execute();
    }

    private void showOnlyDuplicates() {
        new ShowOnlyDuplicatesTask().execute();
    }

    private class LoadMediaFilesTask extends AsyncTask<Uri, Void, ArrayList<MediaItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
        }

        @Override
        protected ArrayList<MediaItem> doInBackground(Uri... uris) {
            ArrayList<MediaItem> mediaItems = new ArrayList<>();
            Uri contentUri = uris[0];
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};

            try (Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(0);
                        String displayName = cursor.getString(1);
                        if (filePath != null && !filePath.contains("/_.trashed/") && !filePath.contains("/.trashed/") &&
                                !filePath.contains("/.recycle/") && !filePath.contains("/.trash/")) {
                            mediaItems.add(new MediaItem(displayName, filePath));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("RestoredFilesActivity", "Error loading media files", e);
            }

            return mediaItems;
        }

        @Override
        protected void onPostExecute(ArrayList<MediaItem> mediaItems) {
            restoredFiles.clear();
            restoredFiles.addAll(mediaItems);

            fullMediaItemList.clear();
            fullMediaItemList.addAll(restoredFiles);

            sortFiles();
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE); // Hide progress bar
        }
    }
    private class LoadDocumentFilesTask extends AsyncTask<Void, Void, ArrayList<MediaItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
        }

        @Override
        protected ArrayList<MediaItem> doInBackground(Void... voids) {
            ArrayList<MediaItem> mediaItems = new ArrayList<>();
            Uri contentUri = MediaStore.Files.getContentUri("external");
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
            String selection = MediaStore.MediaColumns.MIME_TYPE + " IN (?, ?, ?, ?, ?)";
            String[] selectionArgs = {
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.oasis.opendocument.text"
            };

            try (Cursor cursor = getContentResolver().query(contentUri, projection, selection, selectionArgs, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(0);
                        String displayName = cursor.getString(1);
                        mediaItems.add(new MediaItem(displayName, filePath));
                    }
                }
            } catch (Exception e) {
                Log.e("RestoredFilesActivity", "Error loading document files", e);
            }

            return mediaItems;
        }

        @Override
        protected void onPostExecute(ArrayList<MediaItem> mediaItems) {
            restoredFiles.clear();
            restoredFiles.addAll(mediaItems);

            fullMediaItemList.clear();
            fullMediaItemList.addAll(restoredFiles);

            sortFiles();
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE); // Hide progress bar
        }
    }
    private class LoadAllFilesTask extends AsyncTask<Void, Void, ArrayList<MediaItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
        }

        @Override
        protected ArrayList<MediaItem> doInBackground(Void... voids) {
            ArrayList<MediaItem> mediaItems = new ArrayList<>();
            Uri contentUri = MediaStore.Files.getContentUri("external");
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};

            try (Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(0);
                        String displayName = cursor.getString(1);
                        if (filePath != null && !filePath.contains("/_.trashed/") && !filePath.contains("/.trashed/") &&
                                !filePath.contains("/.recycle/") && !filePath.contains("/.trash/")) {
                            mediaItems.add(new MediaItem(displayName, filePath));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("RestoredFilesActivity", "Error loading all files", e);
            }

            return mediaItems;
        }

        @Override
        protected void onPostExecute(ArrayList<MediaItem> mediaItems) {
            restoredFiles.clear();
            restoredFiles.addAll(mediaItems);

            // Sync fullMediaItemList
            fullMediaItemList.clear();
            fullMediaItemList.addAll(restoredFiles);

            sortFiles();
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE); // Hide progress bar
        }
    }

    private class MediaItem {
        String name;
        String path;
        long size;
        long dateModified;
        private boolean isSelected = false;

        MediaItem(String name, String path) {
            this.name = name;
            this.path = path;

            try {
                File file = new File(path);
                if (file.exists()) {
                    this.size = file.length();
                    this.dateModified = file.lastModified();
                } else {
                    Log.e("MediaItem", "File not found: " + path);
                    this.size = 0;
                    this.dateModified = 0;
                }
            } catch (Exception e) {
                Log.e("MediaItem", "Error retrieving file size or dateModified for file: " + path, e);
                this.size = 0;
                this.dateModified = 0;
            }
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }

        public String getFilePath() {
            return path;
        }

        public void setFilePath(String newPath) {
            this.path = newPath;
        }
    }

    // MediaAdapter class
    private class MediaAdapter extends ArrayAdapter<MediaItem> {
        private boolean showPath = false;
        private final RestoredFilesActivity activityContext;

        MediaAdapter(RestoredFilesActivity context, ArrayList<MediaItem> mediaItems) {
            super(context, R.layout.media_list_item, mediaItems);
            this.activityContext = context;
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;

            if (listItem == null) {
                listItem = getLayoutInflater().inflate(R.layout.media_list_item, parent, false);
            }

            TextView text1 = listItem.findViewById(R.id.mediaName);
            ImageView imageView = listItem.findViewById(R.id.mediaThumbnail);
            ImageView shareButton = listItem.findViewById(R.id.shareButton);
            ImageView deleteButton = listItem.findViewById(R.id.deleteButton);
            CheckBox checkBox = listItem.findViewById(R.id.checkBox);

            MediaItem currentItem = getItem(position);

            if (currentItem != null && currentItem.path != null) {
                text1.setText(currentItem.name);

                if (showPath) {
                    File file = new File(currentItem.path);
                    File parentFolderFile = file.getParentFile();

                    if (parentFolderFile != null) {
                        String parentFolder = parentFolderFile.getName();
                        text1.setText(parentFolder + "/");
                    } else {
                        text1.setText(currentItem.name);
                    }
                } else {
                    text1.setText(currentItem.name);
                }

                // Handle checkbox state
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(currentItem.isSelected);

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    currentItem.isSelected = isChecked;
                    activityContext.updateSelectionToolbar();
                });

                loadThumbnail(imageView, currentItem);

                // Share Button functionality
                shareButton.setOnClickListener(v -> {
                    File file = new File(currentItem.path);
                    Uri uri = FileProvider.getUriForFile(
                            getContext(),
                            getContext().getPackageName() + ".provider",
                            file
                    );

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(getMimeType(currentItem.path));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    try {
                        getContext().startActivity(Intent.createChooser(shareIntent, "Share file via"));
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error sharing file", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                });

                // Open file on item click
                listItem.setOnClickListener(v -> openFile(currentItem));

                // Delete Button functionality
                deleteButton.setOnClickListener(v -> deleteFile(currentItem));
            }

            return listItem;
        }
        public void setShowPath(boolean showPath) {
            this.showPath = showPath;
            notifyDataSetChanged();
        }

        private void loadThumbnail(ImageView imageView, MediaItem currentItem) {
            String filePath = currentItem.path;

            if (filePath.endsWith(".jpg") || filePath.endsWith(".png")) {
                Glide.with(getContext())
                        .load(new File(filePath))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            } else if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
                Glide.with(getContext())
                        .asBitmap()
                        .load(Uri.fromFile(new File(filePath)))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_video)
                        .error(R.drawable.ic_video)
                        .into(imageView);
            } else if (filePath.endsWith(".mp3") || filePath.endsWith(".wav") || filePath.endsWith(".m4a")) {
                imageView.setImageResource(R.drawable.ic_audio);
            } else if (filePath.endsWith(".pdf")) {
                imageView.setImageResource(R.drawable.ic_pdf);
            } else if (filePath.endsWith(".pptx")) {
                imageView.setImageResource(R.drawable.ic_ppt);
            } else if (filePath.endsWith(".odt")) {
                imageView.setImageResource(R.drawable.ic_excel);
            } else {
                imageView.setImageResource(R.drawable.ic_file);
            }
        }

        private void openFile(MediaItem currentItem) {
            try {
                File file = new File(currentItem.path);
                Uri uri = FileProvider.getUriForFile(
                        getContext(),
                        getContext().getPackageName() + ".provider",
                        file
                );
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(uri, getMimeType(currentItem.path));
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                getContext().startActivity(Intent.createChooser(openIntent, "Open with"));
            } catch (Exception e) {
                Toast.makeText(getContext(), "No app found to open this file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        private String getMimeType(String filePath) {
            String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            return mime.getMimeTypeFromExtension(extension);
        }

  }
}