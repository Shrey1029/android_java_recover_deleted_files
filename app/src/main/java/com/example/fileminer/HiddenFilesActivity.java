package com.example.fileminer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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

public class HiddenFilesActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int MANAGE_STORAGE_PERMISSION_CODE = 102;
    private GridView listView;
    private MediaAdapterHidden adapter;
    private final List<String> hiddenFilesList = new ArrayList<>();

    //---------------------------
    private ArrayList<MediaItemHidden> restoredFiles = new ArrayList<>();
    private List<MediaItemHidden> fullMediaItemList = new ArrayList<>();

    private String currentSort = "time";
    private boolean isAscending = false;

    private ArrayList<MediaItemHidden> selectedFiles;

    private String selectedSearchType = "Contains";
    private List<String> fileList = new ArrayList<>();
    TextView noResultsText;
    private boolean isCaseSensitive = false;
    private boolean showPath = false;
    private List<String> excludedFolders = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>();
    private boolean isShowingDuplicates = false;
    private List<MediaItemHidden> duplicateList = new ArrayList<>();
    private List<MediaItemHidden> currentFilteredBaseList = new ArrayList<>();


    private String currentQuery = "";

    Toolbar selectionToolbar;
    String fileType = "Hidden";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hidden_files);

        listView = findViewById(R.id.hiddenFilesGridView);

        //  --------
        restoredFiles = new ArrayList<>();
        noResultsText = findViewById(R.id.noResultsText);
        restoredFiles = new ArrayList<>();
        selectedFiles = new ArrayList<>();
        fullMediaItemList = new ArrayList<>();


        //-----------------toolbar below app bar
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


        adapter = new MediaAdapterHidden(HiddenFilesActivity.this, restoredFiles);
        listView.setAdapter(adapter);

        showHiddenFiles();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
            } else {
                showHiddenFiles();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                showHiddenFiles();
            }
        }

        listView.setOnItemClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
            openFile(hiddenFilesList.get(position));
        });
    }

    private void requestManageStoragePermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
        }
    }

    private void showHiddenFiles() {
        File directory = Environment.getExternalStorageDirectory();
        if (directory != null && directory.exists()) {
            hiddenFilesList.clear();
            scanHiddenFolders(directory);
        }

        if (hiddenFilesList.isEmpty()) {
            Toast.makeText(this, "No hidden photos or videos found", Toast.LENGTH_SHORT).show();
        } else {
            restoredFiles.clear();
            for (String path : hiddenFilesList) {
                File file = new File(path);
                String name = file.getName();
                long size = file.length();
                long modified = file.lastModified();

                MediaItemHidden item = new MediaItemHidden(name, path);
                item.size = size;
                item.dateModified = modified;

                restoredFiles.add(item);
            }
            fullMediaItemList.clear();
            fullMediaItemList.addAll(restoredFiles);

            sortFiles();  // Sort current list

            adapter = new MediaAdapterHidden(this, restoredFiles);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    private void scanHiddenFolders(File directory) {
        if (directory == null || !directory.exists() || !directory.canRead()) {
            Log.e("HiddenFiles", "Cannot access directory: " + (directory != null ? directory.getAbsolutePath() : "null"));
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().startsWith(".")) {
                    Log.d("HiddenFiles", "Hidden folder found: " + file.getAbsolutePath());
                    listHiddenFiles(file);
                }
            }
        }
    }

    private void listHiddenFiles(File folder) {
        if (folder == null || !folder.exists() || !folder.canRead()) {
            Log.e("HiddenFiles", "Cannot read folder: " + (folder != null ? folder.getAbsolutePath() : "null"));
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            Log.e("HiddenFiles", "Files list is null for: " + folder.getAbsolutePath());
            return;
        }

        for (File file : files) {
            if (file.isFile() && isPhotoOrVideo(file)) {
                hiddenFilesList.add(file.getAbsolutePath());
                Log.d("HiddenFiles", "Hidden photo or video found: " + file.getAbsolutePath());
            }
        }
    }

    private boolean isPhotoOrVideo(File file) {
        if (file == null || !file.exists()) return false;
        String[] photoExtensions = {
                ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff"
        };

        String[] videoExtensions = {
                ".mp4", ".mkv", ".avi", ".mov", ".flv" ,  ".pdf", ".ppt", ".pptx", ".odt", ".doc", ".docx", ".xls", ".xlsx"
        };

        String fileName = file.getName().toLowerCase();

        for (String ext : photoExtensions) {
            if (fileName.endsWith(ext)) return true;
        }
        for (String ext : videoExtensions) {
            if (fileName.endsWith(ext)) return true;
        }
        return false;
    }

    private void openFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = getMimeType(filePath);

            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            Log.e("HiddenFiles", "Error opening file: ", e);
        }
    }

    private String getMimeType(String fileName) {
        fileName = fileName.toLowerCase();

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                fileName.endsWith(".bmp") || fileName.endsWith(".webp") ||
                fileName.endsWith(".tiff") || fileName.endsWith(".svg")) {
            return "image/*";
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv")) {
            return "video/*";
        } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a")) {
            return "audio/*";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "application/msword";
        } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (fileName.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        }

        return "*/*";
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showHiddenFiles();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                showHiddenFiles();
            } else {
                Toast.makeText(this, "Manage storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //--------------------------------------

public void updateSelectionToolbar() {
    boolean anySelected = false;
    for(MediaItemHidden item: restoredFiles) {
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
                    fileType, // Pass the fileType like "Photo", "Video", etc.
                    new SearchBottomSheet.OnSearchOptionSelectedListener() {
                        @Override
                        public void onSearchOptionSelected(String searchType, boolean caseSensitive,
                                                           List<String> folders, List<String> extensions) {
                            selectedSearchType = searchType;
                            isCaseSensitive = caseSensitive;
                            excludedFolders = folders;
                            excludedExtensions = extensions;

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
        List<MediaItemHidden> filteredList = new ArrayList<>();

        List<MediaItemHidden> baseList = new ArrayList<>(currentFilteredBaseList);

        if (!isCaseSensitive) {
            searchQuery = searchQuery.toLowerCase();
        }

        for (MediaItemHidden item : baseList) {
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

    private boolean shouldExclude(MediaItemHidden item, List<String> excludedFolders) {
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
        for (MediaItemHidden item : restoredFiles) {
            fileList.add(item.name);
        }
    }
    public void deleteFile(MediaItemHidden item) {
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
        // 🔶 Show confirmation dialog FIRST
        new AlertDialog.Builder(this)
                .setTitle("Delete Selected Files")
                .setMessage("Are you sure you want to delete the selected hidden files?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {

                    ArrayList<MediaItemHidden> itemsToDelete = new ArrayList<>();

                    for (MediaItemHidden item : restoredFiles) {
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
    private void selectAllFiles(boolean select) {
        for (MediaItemHidden item : fullMediaItemList) {
            item.setSelected(select);
        }
        updateSelectionToolbar();
    }
    private void moveSelectedFiles() {
        List<MediaItemHidden> selectedItems = new ArrayList<>();
        for (MediaItemHidden item : fullMediaItemList) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No files selected to move", Toast.LENGTH_SHORT).show();
            return;
        }

        File rootDir = Environment.getExternalStorageDirectory();  // Show all folders
        openFolderPicker(rootDir, selectedItems);
    }

    private void openFolderPicker(File currentDir, List<MediaItemHidden> selectedItems) {
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

    private void showCreateSubfolderDialog(File parentFolder, List<MediaItemHidden> selectedItems) {
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

    private void moveFilesToFolder(List<MediaItemHidden> selectedItems, File destinationFolder) {
        boolean anyFileMoved = false;

        for (MediaItemHidden item : selectedItems) {
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


    private class HideDuplicatesTask extends AsyncTask<Void, Void, List<MediaItemHidden>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(HiddenFilesActivity.this, "Hiding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItemHidden> doInBackground(Void... voids) {
            Map<Long, List<MediaItemHidden>> sizeMap = new HashMap<>();
            for (MediaItemHidden item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Set<String> seenHashes = new HashSet<>();
            List<MediaItemHidden> uniqueFiles = new ArrayList<>();

            for (List<MediaItemHidden> group : sizeMap.values()) {
                if (group.size() == 1) {
                    uniqueFiles.add(group.get(0));
                } else {
                    for (MediaItemHidden item : group) {
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
        protected void onPostExecute(List<MediaItemHidden> result) {
            isShowingDuplicates = false;

            currentFilteredBaseList.clear();
            currentFilteredBaseList.addAll(result);

            restoredFiles.clear();
            restoredFiles.addAll(result);
            sortFiles();
            adapter.notifyDataSetChanged();

            if (result.isEmpty()) {
                Toast.makeText(HiddenFilesActivity.this, "No unique files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HiddenFilesActivity.this, "Duplicates Hidden Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class ShowOnlyDuplicatesTask extends AsyncTask<Void, Void, List<MediaItemHidden>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(HiddenFilesActivity.this, "Finding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItemHidden> doInBackground(Void... voids) {
            Map<Long, List<MediaItemHidden>> sizeMap = new HashMap<>();
            for (MediaItemHidden item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Map<String, Integer> hashCountMap = new HashMap<>();
            Map<String, MediaItemHidden> hashToItem = new HashMap<>();
            List<MediaItemHidden> duplicates = new ArrayList<>();

            for (List<MediaItemHidden> group : sizeMap.values()) {
                if (group.size() > 1) {
                    for (MediaItemHidden item : group) {
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
        protected void onPostExecute(List<MediaItemHidden> result) {
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
                Toast.makeText(HiddenFilesActivity.this, "No duplicate files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HiddenFilesActivity.this, "Showing Only Duplicates Based on Content", Toast.LENGTH_SHORT).show();
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