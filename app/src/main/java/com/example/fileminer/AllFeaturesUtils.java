package com.example.fileminer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.function.Function;
public class AllFeaturesUtils {

    // Static method to show/hide selection toolbar
    public static void updateSelectionToolbar(List<MediaItem> fileList, Toolbar selectionToolbar) {
        boolean anySelected = false;
        for (MediaItem item : fileList) {
            if (item.isSelected()) {
                anySelected = true;
                break;
            }
        }
        selectionToolbar.setVisibility(anySelected ? View.VISIBLE : View.GONE);
    }

    //------SortFiles
    public static void sortFiles(List<MediaItem> fileList, String sortType, boolean isAscending) {
        if ("name".equals(sortType)) {
            Collections.sort(fileList, (a, b) -> isAscending ?
                    a.name.compareToIgnoreCase(b.name) :
                    b.name.compareToIgnoreCase(a.name));
        } else if ("size".equals(sortType)) {
            Collections.sort(fileList, (a, b) -> {
                if (a.size == 0 && b.size == 0) return 0;
                else if (a.size == 0) return isAscending ? 1 : -1;
                else if (b.size == 0) return isAscending ? -1 : 1;
                return isAscending ? Long.compare(a.size, b.size) : Long.compare(b.size, a.size);
            });
        } else if ("time".equals(sortType)) {
            Collections.sort(fileList, (a, b) ->
                    isAscending ? Long.compare(a.dateModified, b.dateModified)
                            : Long.compare(b.dateModified, a.dateModified));
        }
    }

    //------------FilterFiles
    public static void filterFiles(
            String query,
            List<String> excludedFolders,
            List<String> excludedExtensions,
            List<MediaItem> baseList,
            List<MediaItem> restoredFiles,
            boolean isCaseSensitive,
            String selectedSearchType,
            TextView noResultsText,
            GridView listView,
            MediaAdapter adapter,
            Runnable sortRunnable
    ) {
        if (query == null) query = "";
        String searchQuery = query.trim();
        List<MediaItem> filteredList = new ArrayList<>();

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

        if (sortRunnable != null) sortRunnable.run(); // Call your existing sort logic

        noResultsText.post(() -> {
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
    //--------- excludedFolders
    public static boolean shouldExclude(MediaItem item, List<String> excludedFolders) {
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

    //--------------loadfilelist
    public static void loadFileList(List<MediaItem> restoredFiles, List<String> fileList) {
        fileList.clear();
        for (MediaItem item : restoredFiles) {
            fileList.add(item.name);
        }
    }

    //--------------------Delet file and Files
    public static void deleteFile(Context context,
                                  MediaItem item,
                                  List<MediaItem> restoredFiles,
                                  List<MediaItem> fullMediaItemList,
                                  MediaAdapter adapter,
                                  Function<File, Boolean> moveToTrashFunc) {

        new AlertDialog.Builder(context)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    File file = new File(item.path);
                    if (file.exists() && moveToTrashFunc.apply(file)) {
                        restoredFiles.remove(item);
                        fullMediaItemList.removeIf(mediaItem -> mediaItem.path.equals(item.path));
                        adapter.notifyDataSetChanged();
                        Toast.makeText(context, "File moved to trash", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public static void deleteSelectedFiles(Context context,
                                           List<MediaItem> restoredFiles,
                                           List<MediaItem> fullMediaItemList,
                                           MediaAdapter adapter,
                                           Function<File, Boolean> moveToTrashFunc) {

        new AlertDialog.Builder(context)
                .setTitle("Delete Selected Files")
                .setMessage("Are you sure you want to delete the selected files?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    ArrayList<MediaItem> itemsToDelete = new ArrayList<>();

                    for (MediaItem item : restoredFiles) {
                        if (item.isSelected()) {
                            File file = new File(item.path);
                            if (file.exists() && moveToTrashFunc.apply(file)) {
                                itemsToDelete.add(item);
                                fullMediaItemList.removeIf(mediaItem -> mediaItem.path.equals(item.path));
                            } else {
                                Toast.makeText(context, "Failed to delete: " + item.name, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    if (!itemsToDelete.isEmpty()) {
                        restoredFiles.removeAll(itemsToDelete);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(context, "Selected files moved to trash!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "No files were deleted.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //------------------ select all files
    public static void selectAllFiles(List<MediaItem> mediaItemList, boolean select) {
        for (MediaItem item : mediaItemList) {
            item.setSelected(select);
        }
    }

    //-------------- Move Files
    public static void moveSelectedFiles(Context context, List<MediaItem> fullMediaItemList, Runnable onMoveComplete, Runnable loadFileList) {
        List<MediaItem> selectedItems = new ArrayList<>();
        for (MediaItem item : fullMediaItemList) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(context, "No files selected to move", Toast.LENGTH_SHORT).show();
            return;
        }

        File rootDir = Environment.getExternalStorageDirectory();
        openFolderPicker(context, rootDir, selectedItems, onMoveComplete, loadFileList);
    }

    private static void openFolderPicker(Context context, File currentDir, List<MediaItem> selectedItems, Runnable onMoveComplete, Runnable loadFileList) {
        File[] subFoldersArr = currentDir.listFiles(File::isDirectory);
        if (subFoldersArr == null) subFoldersArr = new File[0];

        final File[] subFolders = subFoldersArr;

        List<String> options = new ArrayList<>();
        for (File folder : subFolders) {
            options.add(folder.getName());
        }

        options.add("📂 Create New Folder");
        options.add("✅ Select This Folder");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Folder in:\n" + currentDir.getAbsolutePath());
        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
            if (which < subFolders.length) {
                openFolderPicker(context, subFolders[which], selectedItems, onMoveComplete, loadFileList);
            } else if (which == subFolders.length) {
                showCreateSubfolderDialog(context, currentDir, selectedItems, onMoveComplete, loadFileList);
            } else {
                moveFilesToFolder(context, selectedItems, currentDir, onMoveComplete, loadFileList);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static void showCreateSubfolderDialog(Context context, File parentFolder, List<MediaItem> selectedItems, Runnable onMoveComplete, Runnable loadFileList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Create New Folder in:\n" + parentFolder.getAbsolutePath());

        final EditText input = new EditText(context);
        input.setHint("Folder name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (!folderName.isEmpty()) {
                File newFolder = new File(parentFolder, folderName);
                if (!newFolder.exists()) {
                    if (newFolder.mkdirs()) {
                        Toast.makeText(context, "Folder created", Toast.LENGTH_SHORT).show();
                        openFolderPicker(context, newFolder, selectedItems, onMoveComplete, loadFileList);
                    } else {
                        Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Folder already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static void moveFilesToFolder(Context context, List<MediaItem> selectedItems, File destinationFolder, Runnable onMoveComplete, Runnable loadFileList) {
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
                        Toast.makeText(context, "Copied but failed to delete: " + sourceFile.getName(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Failed to copy: " + sourceFile.getName(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error moving: " + sourceFile.getName(), Toast.LENGTH_SHORT).show();
            }
        }

        if (anyFileMoved) {
            Toast.makeText(context, "Files moved successfully", Toast.LENGTH_SHORT).show();
            if (onMoveComplete != null) onMoveComplete.run();
            if (loadFileList != null) loadFileList.run();
        }
    }

    private static boolean copyFile(File source, File dest) throws IOException {
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

    // ---------------- search icon
    public static void setupSearch(Menu menu, Context context, FileFilterCallback callback) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search Files...");
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        callback.onFilter(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        callback.onFilter(newText);
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
    }

    public interface FileFilterCallback {
        void onFilter(String query);
    }

    //--------------- hide and show duplicate
    public static void hideDuplicates(Context context,
                                      List<MediaItem> fullMediaItemList,
                                      List<MediaItem> currentFilteredBaseList,
                                      List<MediaItem> restoredFiles,
                                      Runnable sortFiles,
                                      MediaAdapter adapter) {
        new HideDuplicatesTask(context, fullMediaItemList, currentFilteredBaseList, restoredFiles, sortFiles, adapter).execute();
    }

    public static void showOnlyDuplicates(Context context,
                                          List<MediaItem> fullMediaItemList,
                                          List<MediaItem> currentFilteredBaseList,
                                          List<MediaItem> restoredFiles,
                                          List<MediaItem> duplicateList,
                                          Runnable sortFiles,
                                          MediaAdapter adapter) {
        new ShowOnlyDuplicatesTask(context, fullMediaItemList, currentFilteredBaseList, restoredFiles, duplicateList, sortFiles, adapter).execute();
    }

    private static class HideDuplicatesTask extends AsyncTask<Void, Void, List<MediaItem>> {
        private final Context context;
        private final List<MediaItem> fullList, filteredList, restoredList;
        private final Runnable sortCallback;
        private final BaseAdapter adapter;

        public HideDuplicatesTask(Context context, List<MediaItem> fullList, List<MediaItem> filteredList,
                                  List<MediaItem> restoredList, Runnable sortCallback, BaseAdapter adapter) {
            this.context = context;
            this.fullList = fullList;
            this.filteredList = filteredList;
            this.restoredList = restoredList;
            this.sortCallback = sortCallback;
            this.adapter = adapter;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(context, "Hiding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItem> doInBackground(Void... voids) {
            Map<Long, List<MediaItem>> sizeMap = new HashMap<>();
            for (MediaItem item : fullList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        sizeMap.computeIfAbsent(file.length(), k -> new ArrayList<>()).add(item);
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
                        String hash = DuplicateUtils.getFileHash(item.path);
                        if (hash != null && seenHashes.add(hash)) {
                            uniqueFiles.add(item);
                        }
                    }
                }
            }

            return uniqueFiles;
        }

        @Override
        protected void onPostExecute(List<MediaItem> result) {
            filteredList.clear();
            filteredList.addAll(result);

            restoredList.clear();
            restoredList.addAll(result);

            sortCallback.run();
            adapter.notifyDataSetChanged();

            Toast.makeText(context, result.isEmpty() ? "No unique files found" : "Duplicates Hidden", Toast.LENGTH_SHORT).show();
        }
    }

    private static class ShowOnlyDuplicatesTask extends AsyncTask<Void, Void, List<MediaItem>> {
        private final Context context;
        private final List<MediaItem> fullList, filteredList, restoredList, duplicateList;
        private final Runnable sortCallback;
        private final BaseAdapter adapter;

        public ShowOnlyDuplicatesTask(Context context, List<MediaItem> fullList, List<MediaItem> filteredList,
                                      List<MediaItem> restoredList, List<MediaItem> duplicateList,
                                      Runnable sortCallback, BaseAdapter adapter) {
            this.context = context;
            this.fullList = fullList;
            this.filteredList = filteredList;
            this.restoredList = restoredList;
            this.duplicateList = duplicateList;
            this.sortCallback = sortCallback;
            this.adapter = adapter;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(context, "Finding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<MediaItem> doInBackground(Void... voids) {
            Map<Long, List<MediaItem>> sizeMap = new HashMap<>();
            for (MediaItem item : fullList) {
                if (item != null && item.path != null) {
                    File file = new File(item.path);
                    if (file.exists()) {
                        sizeMap.computeIfAbsent(file.length(), k -> new ArrayList<>()).add(item);
                    }
                }
            }

            Map<String, Integer> hashCountMap = new HashMap<>();
            Map<String, MediaItem> hashToItem = new HashMap<>();
            List<MediaItem> duplicates = new ArrayList<>();

            for (List<MediaItem> group : sizeMap.values()) {
                if (group.size() > 1) {
                    for (MediaItem item : group) {
                        String hash = DuplicateUtils.getFileHash(item.path);
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
            duplicateList.clear();
            duplicateList.addAll(result);

            filteredList.clear();
            filteredList.addAll(result);

            restoredList.clear();
            restoredList.addAll(result);

            sortCallback.run();
            adapter.notifyDataSetChanged();

            Toast.makeText(context, result.isEmpty() ? "No duplicate files found" : "Showing Only Duplicates", Toast.LENGTH_SHORT).show();
        }
    }
}
