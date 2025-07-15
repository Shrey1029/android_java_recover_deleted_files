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

public class AllDataRecovery extends AppCompatActivity {

    GridView listView;
    ProgressBar progressBar;
    List<File> deletedFiles;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int MAX_FILES = 500;

    //--------------
    private MediaAdapterDeletd adapter;
    private ArrayList<MediaItemDeleted> restoredFiles = new ArrayList<>();
    private List<MediaItemDeleted> fullMediaItemList = new ArrayList<>();

    private String currentSort = "time";
    private boolean isAscending = false;

    private ArrayList<MediaItemDeleted> selectedFiles;

    private String selectedSearchType = "Contains";
    private List<String> fileList = new ArrayList<>();
    TextView noResultsText;
    private boolean isCaseSensitive = false;
    private boolean showPath = false;
    private List<String> excludedFolders = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>();
    private boolean isShowingDuplicates = false;
    private List<MediaItemDeleted> duplicateList = new ArrayList<>();
    private List<MediaItemDeleted> currentFilteredBaseList = new ArrayList<>();


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
                    MediaItemDeleted item = new MediaItemDeleted(file.getName(), file.getAbsolutePath());
                    restoredFiles.add(item);
                    fullMediaItemList.add(item);
                }

                sortFiles(); // sort after scan
                adapter = new MediaAdapterDeletd(this, restoredFiles);
                listView.setAdapter(adapter);
            });

        }).start();
    }

    private void searchTrashedFiles(File dir, int depth, Set<String> seenPaths) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || depth > 10 || deletedFiles.size() >= MAX_FILES) return;

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
        return name.startsWith(".trashed-")||name.startsWith(".trashed") || name.equals(".recycle") || name.equals(".trash")||name.equals("_.trashed");

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

    //--------------------------------------
    public void updateSelectionToolbar() {
        boolean anySelected = false;
        for (MediaItemDeleted item : restoredFiles) {
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
        List<MediaItemDeleted> filteredList = new ArrayList<>();

        List<MediaItemDeleted> baseList = new ArrayList<>(currentFilteredBaseList);

        if (!isCaseSensitive) {
            searchQuery = searchQuery.toLowerCase();
        }

        for (MediaItemDeleted item : baseList) {
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
    private boolean shouldExclude(MediaItemDeleted item, List<String> excludedFolders) {
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
        for (MediaItemDeleted item : restoredFiles) {
            fileList.add(item.name);
        }
    }

    public void deleteFile(MediaItemDeleted item) {
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

    // Delete all selected files (Select All + Delete)
    private void deleteSelectedFiles() {
        // Show confirmation dialog first
        new AlertDialog.Builder(this)
                .setTitle("Delete Selected Files")
                .setMessage("Are you sure you want to permanently delete the selected files?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {

                    ArrayList<MediaItemDeleted> itemsToDelete = new ArrayList<>();

                    for (MediaItemDeleted item : restoredFiles) {
                        if (item.isSelected) {
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



    //Select All or Deselect All
    private void selectAllFiles(boolean select) {
        for (MediaItemDeleted item : fullMediaItemList) {
            item.setSelected(select);
        }
        updateSelectionToolbar();
    }
    private void moveSelectedFiles() {
        // Step 1: Get selected files
        List<MediaItemDeleted> selectedItems = new ArrayList<>();
        for (MediaItemDeleted item : fullMediaItemList) {
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

    private void openFolderPicker(File currentDir, List<MediaItemDeleted> selectedItems) {
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

    private void showCreateSubfolderDialog(File parentFolder, List<MediaItemDeleted> selectedItems) {
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

    private void moveFilesToFolder(List<MediaItemDeleted> selectedItems, File destinationFolder) {
        boolean anyFileMoved = false;

        for (MediaItemDeleted item : selectedItems) {
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
            int maxBytes = 1024 * 1024; // 1MB (can adjust this value as needed)

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


    private class HideDuplicatesTask extends AsyncTask<Void, Void, List<MediaItemDeleted>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(AllDataRecovery.this, "Hiding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItemDeleted> doInBackground(Void... voids) {
            Map<Long, List<MediaItemDeleted>> sizeMap = new HashMap<>();
            for (MediaItemDeleted item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Set<String> seenHashes = new HashSet<>();
            List<MediaItemDeleted> uniqueFiles = new ArrayList<>();

            for (List<MediaItemDeleted> group : sizeMap.values()) {
                if (group.size() == 1) {
                    uniqueFiles.add(group.get(0));
                } else {
                    for (MediaItemDeleted item : group) {
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
        protected void onPostExecute(List<MediaItemDeleted> result) {
            isShowingDuplicates = false;

            currentFilteredBaseList.clear();
            currentFilteredBaseList.addAll(result);

            restoredFiles.clear();
            restoredFiles.addAll(result);
            sortFiles();
            adapter.notifyDataSetChanged();

            if (result.isEmpty()) {
                Toast.makeText(AllDataRecovery.this, "No unique files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AllDataRecovery.this, "Duplicates Hidden Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ShowOnlyDuplicatesTask extends AsyncTask<Void, Void, List<MediaItemDeleted>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(AllDataRecovery.this, "Finding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItemDeleted> doInBackground(Void... voids) {
            Map<Long, List<MediaItemDeleted>> sizeMap = new HashMap<>();
            for (MediaItemDeleted item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Map<String, Integer> hashCountMap = new HashMap<>();
            Map<String, MediaItemDeleted> hashToItem = new HashMap<>();
            List<MediaItemDeleted> duplicates = new ArrayList<>();

            for (List<MediaItemDeleted> group : sizeMap.values()) {
                if (group.size() > 1) {
                    for (MediaItemDeleted item : group) {
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
        protected void onPostExecute(List<MediaItemDeleted> result) {
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
                Toast.makeText(AllDataRecovery.this, "No duplicate files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AllDataRecovery.this, "Showing Only Duplicates Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void hideDuplicates() {
        new HideDuplicatesTask().execute();
    }

    private void showOnlyDuplicates() {
        new ShowOnlyDuplicatesTask().execute();
    }
}