package com.example.fileminer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.appcompat.widget.Toolbar;


import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OtherFilesActivity extends AppCompatActivity {

    private GridView listView;
    private ProgressBar progressBar;
    private TextView noResultsText;

    private ArrayList<MediaItemOther> otherFiles;  // restoredFiles
    private ArrayList<MediaItemOther> selectedFiles;
    private List<MediaItemOther> fullMediaItemList;

    private MediaAdapterOther adapter;

    private Toolbar selectionToolbar;

    private String currentSort = "time";
    private boolean isAscending = false;
    private String currentQuery = "";
    private String selectedSearchType = "Contains";
    private boolean isCaseSensitive = false;
    private boolean showPath = false;
    private String fileType = "All";

    private List<String> excludedFolders = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>();
    private boolean isShowingDuplicates = false;
    private List<MediaItemOther> duplicateList = new ArrayList<>();
    private List<MediaItemOther> currentFilteredBaseList = new ArrayList<>();


    private List<String> fileList = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_other_files);

        listView = findViewById(R.id.gridOtherFiles);
        progressBar = findViewById(R.id.progressBar);
        noResultsText = findViewById(R.id.noResultsText);

        otherFiles = new ArrayList<>();
        selectedFiles = new ArrayList<>();
        fullMediaItemList = new ArrayList<>();

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

        fetchOtherFiles();

        listView.setOnItemClickListener((parent, view, position, id) -> openFile(new File(otherFiles.get(position).getFilePath())));
    }

    private void fetchOtherFiles() {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        new Thread(() -> {
            File directory = new File("/storage/emulated/0/");
            otherFiles = new ArrayList<>();
            fullMediaItemList = new ArrayList<>();

            searchFiles(directory);
            sortFiles();

            fullMediaItemList.addAll(otherFiles); // Save full list for filtering

            runOnUiThread(() -> {
                adapter = new MediaAdapterOther(this, otherFiles);
                listView.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private void searchFiles(File dir) {
        try {
            if (dir != null && dir.isDirectory() && !isExcludedFolder(dir) && !dir.getName().startsWith(".")) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && !file.getName().startsWith(".") &&
                                !isExcludedFileType(file) && !isTooSmall(file)) {
                            otherFiles.add(new MediaItemOther(file.getName(), file.getAbsolutePath()));
                        } else if (file.isDirectory() && file.canRead()) {
                            searchFiles(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isExcludedFolder(File file) {
        String path = file.getAbsolutePath();
        return path.contains("/WhatsApp/Media/.Statuses") ||
                path.contains("/Android/media/com.whatsapp/WhatsApp/Media/.Statuses") ||
                path.contains("/Android/data/") ||
                path.contains("/Android/obb/") ||
                path.contains("/.thumbnails") ||
                path.contains("/.cache") ||
                path.contains("/Telegram/") ||
                path.contains("/Instagram/") ||
                path.contains("/MIUI/") ||
                path.contains("/com.miui.backup/") ||
                path.contains("/Logs/") || path.contains("_.trashed") || path.contains(".trashed") ||
                path.contains(".recycle") || path.contains(".trash");

    }

    private boolean isTooSmall(File file) {
        return file.length() < 10 * 1024;
    }

    private boolean isExcludedFileType(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") ||
                name.endsWith(".pdf") || name.endsWith(".mp3") || name.endsWith(".wav") ||
                name.endsWith(".odt") || name.endsWith(".pptx") || name.endsWith(".doc") ||
                name.endsWith(".docx") ;
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(file.getAbsolutePath()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open file with"));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getMimeType(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(path)).toString());
        if (extension != null && !extension.isEmpty()) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mime != null) return mime;
        }
        return "*/*";
    }

    //---------------
    public void updateSelectionToolbar() {
        boolean anySelected = false;
        for (MediaItemOther item : otherFiles) {
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
        } else  if (id == R.id.action_filter) {
            SearchBottomSheet bottomSheet = new SearchBottomSheet(
                    this,
                    selectedSearchType,
                    isCaseSensitive,
                    excludedFolders,
                    excludedExtensions,
                    fileType,
                    (searchType, caseSensitive, folders, extensions) -> {
                        selectedSearchType = searchType;
                        isCaseSensitive = caseSensitive;
                        excludedFolders = folders;
                        excludedExtensions = extensions;

                        filterFiles(currentQuery, excludedFolders, excludedExtensions);
                    });
            bottomSheet.show(getSupportFragmentManager(), "SearchBottomSheet");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void sortFiles() {
        if ("name".equals(currentSort)) {
            Collections.sort(otherFiles, (a, b) -> isAscending ?
                    a.name.compareToIgnoreCase(b.name) :
                    b.name.compareToIgnoreCase(a.name));
        } else if ("size".equals(currentSort)) {
            Collections.sort(otherFiles, (a, b) -> {
                if (a.size == 0 && b.size == 0) return 0;
                else if (a.size == 0) return isAscending ? 1 : -1;
                else if (b.size == 0) return isAscending ? -1 : 1;
                return isAscending ? Long.compare(a.size, b.size) : Long.compare(b.size, a.size);
            });
        } else if ("time".equals(currentSort)) {
            Collections.sort(otherFiles, (a, b) ->
                    isAscending ? Long.compare(a.dateModified, b.dateModified) : Long.compare(b.dateModified, a.dateModified));
        }
    }

    private void filterFiles(String query, List<String> excludedFolders, List<String> excludedExtensions) {
        if (query == null) query = "";
        String searchQuery = query.trim();
        List<MediaItemOther> filteredList = new ArrayList<>();

        List<MediaItemOther> baseList = new ArrayList<>(currentFilteredBaseList);

        if (!isCaseSensitive) {
            searchQuery = searchQuery.toLowerCase();
        }

        for (MediaItemOther item : baseList) {
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

        otherFiles.clear();
        otherFiles.addAll(filteredList);
        sortFiles();

        runOnUiThread(() -> {
            if (otherFiles.isEmpty()) {
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

    private boolean shouldExclude(MediaItemOther item, List<String> excludedFolders) {
        if (excludedFolders == null || excludedFolders.isEmpty()) return false;

        File file = new File(item.path);
        File parentFolder = file.getParentFile();
        if (parentFolder != null) {
            String folderName = parentFolder.getName();
            for (String exclude : excludedFolders) {
                if (folderName.equalsIgnoreCase(exclude)) {
                    return true; // yes, exclude
                }
            }
        }
        return false;
    }

    private void loadFileList() {
        fileList.clear();
        for (MediaItemOther item : otherFiles) {
            fileList.add(item.name);
        }
    }
    public void deleteFile(MediaItemOther item) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    File file = new File(item.path);
                    if (file.exists() && moveToTrash(file)) {
                        otherFiles.remove(item);
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
        //  Show confirmation dialog FIRST
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Selected Files")
                .setMessage("Are you sure you want to delete the selected hidden files?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {

                    ArrayList<MediaItemOther> itemsToDelete = new ArrayList<>();

                    for (MediaItemOther item : otherFiles) {
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
                        otherFiles.removeAll(itemsToDelete);
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

    // Select All or Deselect All
    private void selectAllFiles(boolean select) {
        for (MediaItemOther item : otherFiles) {
            item.setSelected(select);
        }
        updateSelectionToolbar();
    }
    private void moveSelectedFiles() {
        // Step 1: Get selected files
        List<MediaItemOther> selectedItems = new ArrayList<>();
        for (MediaItemOther item : fullMediaItemList) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No files selected to move", Toast.LENGTH_SHORT).show();
            return;
        }
        File rootDir = Environment.getExternalStorageDirectory();
        openFolderPicker(rootDir, selectedItems);
    }

    private void openFolderPicker(File currentDir, List<MediaItemOther> selectedItems) {
        File[] subFoldersArr = currentDir.listFiles(File::isDirectory);
        if (subFoldersArr == null) subFoldersArr = new File[0];

        final File[] subFolders = subFoldersArr;

        List<String> options = new ArrayList<>();
        for (File folder : subFolders) {
            options.add(folder.getName());
        }

        options.add("📂 Create New Folder");
        options.add("✅ Select This Folder");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
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

    private void showCreateSubfolderDialog(File parentFolder, List<MediaItemOther> selectedItems) {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                        openFolderPicker(newFolder, selectedItems);
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

    private void moveFilesToFolder(List<MediaItemOther> selectedItems, File destinationFolder) {
        boolean anyFileMoved = false;

        for (MediaItemOther item : selectedItems) {
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
            androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search Files...");

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        currentQuery = query; // remember query
                        filterFiles(query, excludedFolders, excludedExtensions);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        currentQuery = newText; // remember query
                        filterFiles(newText, excludedFolders, excludedExtensions);
                        return true;
                    }
                });

                // Ensure search icon is always visible
                searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            }
        }

        // Filter icon always visible
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


    private class HideDuplicatesTask extends AsyncTask<Void, Void, List<MediaItemOther>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(OtherFilesActivity.this, "Hiding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItemOther> doInBackground(Void... voids) {
            Map<Long, List<MediaItemOther>> sizeMap = new HashMap<>();
            for (MediaItemOther item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Set<String> seenHashes = new HashSet<>();
            List<MediaItemOther> uniqueFiles = new ArrayList<>();

            for (List<MediaItemOther> group : sizeMap.values()) {
                if (group.size() == 1) {
                    uniqueFiles.add(group.get(0));
                } else {
                    for (MediaItemOther item : group) {
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
        protected void onPostExecute(List<MediaItemOther> result) {
            isShowingDuplicates = false;

            currentFilteredBaseList.clear();
            currentFilteredBaseList.addAll(result);

            otherFiles.clear();
            otherFiles.addAll(result);
            sortFiles();
            adapter.notifyDataSetChanged();

            if (result.isEmpty()) {
                Toast.makeText(OtherFilesActivity.this, "No unique files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OtherFilesActivity.this, "Duplicates Hidden Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class ShowOnlyDuplicatesTask extends AsyncTask<Void, Void, List<MediaItemOther>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(OtherFilesActivity.this, "Finding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItemOther> doInBackground(Void... voids) {
            Map<Long, List<MediaItemOther>> sizeMap = new HashMap<>();
            for (MediaItemOther item : fullMediaItemList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        long size = file.length();
                        sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Map<String, Integer> hashCountMap = new HashMap<>();
            Map<String, MediaItemOther> hashToItem = new HashMap<>();
            List<MediaItemOther> duplicates = new ArrayList<>();

            for (List<MediaItemOther> group : sizeMap.values()) {
                if (group.size() > 1) {
                    for (MediaItemOther item : group) {
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
        protected void onPostExecute(List<MediaItemOther> result) {
            isShowingDuplicates = true;
            duplicateList.clear();
            duplicateList.addAll(result);

            currentFilteredBaseList.clear();
            currentFilteredBaseList.addAll(result);

            otherFiles.clear();
            otherFiles.addAll(result);
            sortFiles();
            adapter.notifyDataSetChanged();

            if (result.isEmpty()) {
                Toast.makeText(OtherFilesActivity.this, "No duplicate files found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OtherFilesActivity.this, "Showing Only Duplicates Based on Content", Toast.LENGTH_SHORT).show();
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