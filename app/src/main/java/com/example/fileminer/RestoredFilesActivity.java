package com.example.fileminer;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RestoredFilesActivity extends AppCompatActivity implements ToolbarUpdateListener, FileDeleteListener{

    private GridView listView;
    private MediaAdapter adapter;
    private ArrayList<MediaItem> restoredFiles;
    private ArrayList<MediaItem> selectedFiles;
    ProgressBar progressBar;
    private List<String> fileList = new ArrayList<>();
    TextView noResultsText;
    private List<MediaItem> fullMediaItemList = new ArrayList<>();
    private boolean isCaseSensitive = false;
    private boolean showPath = false;
    private List<String> excludedFolders = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>();
    private boolean isShowingDuplicates = false;
    private List<MediaItem> duplicateList = new ArrayList<>();
    private List<MediaItem> currentFilteredBaseList = new ArrayList<>();
    private String currentQuery = "";
    private String currentSort = "time";
    private boolean isAscending = false;
    private String selectedSearchType = "Contains";

    //--- exclude for extension and folder
    private String fileType = "Photo";

    Toolbar selectionToolbar;

    //-----------deleted
    List<File> deletedFiles;
    private static final int MAX_FILES = 500;

    //--------Hidden Files
    private final List<String> hiddenFilesList = new ArrayList<>();


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

        adapter = new MediaAdapter(this, restoredFiles, this, this);
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
                case "Deleted":
                    startFileScan();
                    break;
                case "Hidden":
                    showHiddenFiles();
                    break;
                case "OtherFiles":
                    fetchOtherFiles();
                    break;
                default:
                    new LoadAllFilesTask().execute();
                    break;
            }
        }

    }

    //------------ image, audio, vedio, document Files
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

    //---------------Deleted Files
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
                adapter = new MediaAdapter(this, restoredFiles, RestoredFilesActivity.this , this);
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

    //-------------------Hidden Files
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

                MediaItem item = new MediaItem(name, path);
                item.size = size;
                item.dateModified = modified;

                restoredFiles.add(item);
            }
            fullMediaItemList.clear();
            fullMediaItemList.addAll(restoredFiles);

            sortFiles();  // Sort current list

            adapter = new MediaAdapter(this, restoredFiles, this, this);
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

    //----------OtherFiles
    private void fetchOtherFiles() {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        new Thread(() -> {
            File directory = new File("/storage/emulated/0/");
            restoredFiles = new ArrayList<>();
            fullMediaItemList = new ArrayList<>();

            searchFiles(directory);
            sortFiles();

            fullMediaItemList.addAll(restoredFiles);
            runOnUiThread(() -> {
                adapter = new MediaAdapter(this, restoredFiles, this, this);
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
                            restoredFiles.add(new MediaItem(file.getName(), file.getAbsolutePath()));
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


    //----------open file with the help of external apps
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

    //=====================================Features================================
    @Override
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

    //--------------SortFiles
    private void sortFiles() {AllFeaturesUtils.sortFiles(restoredFiles, currentSort, isAscending);
    }

    //-------------- filterfiles
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

    //--------------loadfilelist
    private void loadFileList() {
        AllFeaturesUtils.loadFileList(restoredFiles, fileList);
    }

    //--------------------Delet file and Files
    @Override
    public void deleteFile(MediaItem item) {
        AllFeaturesUtils.deleteFile(
                this,
                item,
                restoredFiles,
                fullMediaItemList,
                adapter,
                file -> moveToTrash(file)
        );

    }
    private void deleteSelectedFiles() {
        AllFeaturesUtils.deleteSelectedFiles(
                this,
                restoredFiles,
                fullMediaItemList,
                adapter,
                file -> moveToTrash(file)
        );

    }
    private boolean moveToTrash(File file) {
        File trashDir = new File(Environment.getExternalStorageDirectory(), "_.trashed");
        if (!trashDir.exists()) trashDir.mkdirs();

        File destFile = new File(trashDir, file.getName());
        return file.renameTo(destFile);
    }

    //------------------ select all files
    private void selectAllFiles(boolean select) {
        AllFeaturesUtils.selectAllFiles(fullMediaItemList, select);
        updateSelectionToolbar();  // Keep this to refresh UI
    }

    //-------------- Move Files
    private void moveSelectedFiles() {
        AllFeaturesUtils.moveSelectedFiles(
                this,
                fullMediaItemList,
                this::updateSelectionToolbar,
                this::loadFileList
        );
    }

    //---------------- Search Icon
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);

        AllFeaturesUtils.setupSearch(menu, this, query -> {
            currentQuery = query;
            filterFiles(query, excludedFolders, excludedExtensions);
        });

        return true;
    }

    // ------------ hide and show duplicate
    private void hideDuplicates() {
        AllFeaturesUtils.hideDuplicates(
                this,
                fullMediaItemList,
                currentFilteredBaseList,
                restoredFiles,
                this::sortFiles,
                adapter
        );    }

    private void showOnlyDuplicates() {
        AllFeaturesUtils.showOnlyDuplicates(
                this,
                fullMediaItemList,
                currentFilteredBaseList,
                restoredFiles,
                duplicateList,
                this::sortFiles,
                adapter
        );    }

}