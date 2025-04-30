package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OtherFilesActivity extends AppCompatActivity {

    private GridView gridOtherFiles;
    private ProgressBar progressBar;
    private List<File> otherFiles;
    private List<File> selectedFiles = new ArrayList<>();
    private GridAdapter adapter;
    private String currentSort = "name";
    private boolean isAscending = true;
    private List<File> fullFileList = new ArrayList<>(); // Full list for search
    private List<File> fileList;
    private String selectedSearchType = "Contains"; // Declare variable globally
    private List<MediaBrowser.MediaItem> restoredFiles = new ArrayList<>();
    private List<String> fileList1 = new ArrayList<>();
    TextView noResultsText;
    private boolean showPath = false;
    private boolean isCaseSensitive = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_files);

        gridOtherFiles = findViewById(R.id.gridOtherFiles);
        progressBar = findViewById(R.id.progressBar);
        noResultsText = findViewById(R.id.noResultsText);

        fetchOtherFiles();

        gridOtherFiles.setOnItemClickListener((parent, view, position, id) -> openFile(otherFiles.get(position)));
    }

    private void fetchOtherFiles() {
        progressBar.setVisibility(View.VISIBLE);
        gridOtherFiles.setVisibility(View.GONE);

        new Thread(() -> {
            File directory = new File("/storage/emulated/0/");
            otherFiles = new ArrayList<>();
            fullFileList = new ArrayList<>(); // ✅ Ensure full list is initialized
            searchFiles(directory);

            fullFileList.addAll(otherFiles); // ✅ Save full file list for search
            sortFiles();

            runOnUiThread(() -> {
                adapter = new GridAdapter(OtherFilesActivity.this, otherFiles);
                gridOtherFiles.setAdapter(adapter);
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
                            searchFiles(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isExcludedFileType(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") ||
                name.endsWith(".pdf") || name.endsWith(".mp3") || name.endsWith(".wav") ||
                name.endsWith(".odt") || name.endsWith(".pptx") || name.endsWith(".doc") ||
                name.endsWith(".docx");
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
        return "/";
    }

    private void sortFiles() {
        Comparator<File> comparator;
        switch (currentSort) {
            case "size":
                comparator = Comparator.comparingLong(File::length);
                break;
            case "time":
                comparator = Comparator.comparingLong(File::lastModified);
                break;
            default:
                comparator = Comparator.comparing(File::getName, String::compareToIgnoreCase);
                break;
        }
        Collections.sort(otherFiles, isAscending ? comparator : comparator.reversed());
    }

    private void deleteSelectedFiles() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "No files selected for deletion", Toast.LENGTH_SHORT).show();
            return;
        }

        List<File> deletedFiles = new ArrayList<>();

        for (File file : selectedFiles) {
            if (file.exists()) {
                if (file.delete()) {
                    deletedFiles.add(file);
                } else {
                    Toast.makeText(this, "Failed to delete: " + file.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (!deletedFiles.isEmpty()) {
            otherFiles.removeAll(deletedFiles);
            selectedFiles.removeAll(deletedFiles);
            if (adapter != null) adapter.notifyDataSetChanged();
            Toast.makeText(this, "Selected files deleted successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No files were deleted.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);

        if (searchItem != null) {
            androidx.appcompat.widget.SearchView searchView =
                    (androidx.appcompat.widget.SearchView) searchItem.getActionView();

            if (searchView != null) {
                searchView.setQueryHint("Search Files...");
                searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterFiles(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterFiles(newText);
                        return true;
                    }
                });

                searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS |
                        MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

                searchView.setOnCloseListener(() -> {
                    loadFileList();
                    return false;
                });
            }
        }
        return true;
    }

    private void filterFiles(String query) {
        if (query == null) query = "";
        String searchQuery = query.trim(); // Trim the query to remove leading and trailing spaces
        List<File> filteredList = new ArrayList<>();

        // If the search query is empty, reset to the full list
        if (searchQuery.isEmpty()) {
            filteredList.addAll(fullFileList);
        } else {
            for (File file : fullFileList) {
                String fileName = file.getName(); // Get the file name
                String filePath = file.getAbsolutePath(); // Get the full path

                // If case-insensitive search is selected, convert both to lowercase
                if (!isCaseSensitive) {
                    searchQuery = searchQuery.toLowerCase();
                    fileName = fileName.toLowerCase();
                    filePath = filePath.toLowerCase();
                }

                // Apply the search logic based on the selected search type
                switch (selectedSearchType) {
                    case "Contains":
                        if (fileName.contains(searchQuery)) filteredList.add(file);
                        break;
                    case "Starts With":
                        if (fileName.startsWith(searchQuery)) filteredList.add(file);
                        break;
                    case "Ends With":
                        if (fileName.endsWith(searchQuery)) filteredList.add(file);
                        break;
                    case "Path": // <<<< NEW
                        if (filePath.contains(searchQuery)) filteredList.add(file);
                        break;
                    default:
                        if (fileName.contains(searchQuery)) filteredList.add(file);
                        break;
                }
            }
        }

        otherFiles.clear();
        otherFiles.addAll(filteredList);

        // Update the UI with the filtered list
        runOnUiThread(() -> {
            if (otherFiles.isEmpty()) {
                noResultsText.setVisibility(View.VISIBLE);
                gridOtherFiles.setVisibility(View.GONE);
            } else {
                noResultsText.setVisibility(View.GONE);
                gridOtherFiles.setVisibility(View.VISIBLE);
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sortByName) {
            currentSort = "name";
        } else if (id == R.id.sortBySize) {
            currentSort = "size";
        } else if (id == R.id.sortByTime) {
            currentSort = "time";
        } else if (id == R.id.sortOrderToggle) {
            isAscending = !isAscending;
            item.setTitle(isAscending ? "Ascending" : "Descending");
        } else if (id == R.id.deleteSelected) {
            deleteSelectedFiles();
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

            adapter.setShowPath(showPath);    // <-- यहाँ कॉल करो
            return true;
        }

        else if (id == R.id.action_search) {
            Toast.makeText(this, "Search Clicked", Toast.LENGTH_SHORT).show();

            loadFileList();

            List<String> fileNames = new ArrayList<>();
            for (File file : otherFiles) {
                fileNames.add(file.getName());
            }

            Log.d("SearchBottomSheet", "File list size: " + fileNames.size());

            SearchBottomSheet bottomSheet = new SearchBottomSheet(this, selectedSearchType, isCaseSensitive, new SearchBottomSheet.OnSearchOptionSelectedListener() {
                @Override
                public void onSearchOptionSelected(String searchType, boolean caseSensitive) {
                    selectedSearchType = searchType;
                    isCaseSensitive = caseSensitive;

                    String caseSensitivity = caseSensitive ? "Case Sensitive" : "Case Insensitive";
                    Toast.makeText(OtherFilesActivity.this, "Selected: " + searchType + " | " + caseSensitivity, Toast.LENGTH_SHORT).show();
                }
            });

            bottomSheet.show(getSupportFragmentManager(), "SearchBottomSheet");
            return true;
        }

        sortFiles();
        if (adapter != null) adapter.notifyDataSetChanged();
        return true;
    }
    // Hash function for file comparison based on content
    private String getFileHash(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(filePath);

            byte[] byteArray = new byte[1024];
            int bytesRead;
            int totalRead = 0;
            int maxBytes = 1024 * 1024; // 1MB limit

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

    // AsyncTask to hide duplicates based on content
    private class HideDuplicatesTask extends AsyncTask<Void, Void, List<File>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(OtherFilesActivity.this, "Hiding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<File> doInBackground(Void... voids) {
            Set<String> seenHashes = new HashSet<>();
            List<File> uniqueFiles = new ArrayList<>();

            for (File file : otherFiles) {
                if (file != null) {
                    String filePath = file.getAbsolutePath();
                    String fileHash = getFileHash(filePath);
                    if (fileHash != null && !seenHashes.contains(fileHash)) {
                        seenHashes.add(fileHash);
                        uniqueFiles.add(file);
                    }
                }
            }
            return uniqueFiles;
        }

        @Override
        protected void onPostExecute(List<File> result) {
            super.onPostExecute(result);
            if (result.isEmpty()) {
                Toast.makeText(OtherFilesActivity.this, "No duplicates found", Toast.LENGTH_SHORT).show();
            } else {
                otherFiles.clear();
                otherFiles.addAll(result);
                adapter.notifyDataSetChanged();
                Toast.makeText(OtherFilesActivity.this, "Duplicates Hidden Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // AsyncTask to show only duplicates based on content
    private class ShowOnlyDuplicatesTask extends AsyncTask<Void, Void, List<File>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(OtherFilesActivity.this, "Finding duplicates, please wait...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<File> doInBackground(Void... voids) {
            Map<String, Integer> hashCountMap = new HashMap<>();
            List<File> duplicatesOnly = new ArrayList<>();

            for (File file : otherFiles) {
                if (file != null) {
                    String filePath = file.getAbsolutePath();
                    String fileHash = getFileHash(filePath);
                    if (fileHash != null) {
                        hashCountMap.put(fileHash, hashCountMap.getOrDefault(fileHash, 0) + 1);
                    }
                }
            }

            for (File file : otherFiles) {
                if (file != null) {
                    String filePath = file.getAbsolutePath();
                    String fileHash = getFileHash(filePath);
                    if (fileHash != null && hashCountMap.get(fileHash) > 1) {
                        duplicatesOnly.add(file);
                    }
                }
            }

            return duplicatesOnly;
        }

        @Override
        protected void onPostExecute(List<File> result) {
            super.onPostExecute(result);
            if (result.isEmpty()) {
                Toast.makeText(OtherFilesActivity.this, "No duplicate files found", Toast.LENGTH_SHORT).show();
            } else {
                otherFiles.clear();
                otherFiles.addAll(result);
                adapter.notifyDataSetChanged();
                Toast.makeText(OtherFilesActivity.this, "Showing Only Duplicates Based on Content", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Call these from your button clicks:
    private void hideDuplicates() {
        new HideDuplicatesTask().execute();
    }

    private void showOnlyDuplicates() {
        new ShowOnlyDuplicatesTask().execute();
    }



    private void loadFileList() {
        if (fileList == null) fileList = new ArrayList<>();
        else fileList.clear();

        if (fullFileList == null) fullFileList = new ArrayList<>();
        else fullFileList.clear();

        File directory = new File("/storage/emulated/0/");
        searchFiles(directory);

        fullFileList.addAll(otherFiles); // ✅ Ensure full file list is maintained

        Log.d("FileLoader", "Total files loaded: " + fullFileList.size());

        runOnUiThread(() -> {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem toggleItem = menu.findItem(R.id.sortOrderToggle);
        if (toggleItem != null) {
            toggleItem.setTitle(isAscending ? "Ascending" : "Descending");
        }
        return super.onPrepareOptionsMenu(menu);
    }


    private class GridAdapter extends BaseAdapter {
        private final Activity context;
        private final List<File> files;
        private boolean showPath = false;  // Flag to toggle path visibility

        GridAdapter(Activity context, List<File> files) {
            this.context = context;
            this.files = files;
        }

        // Method to update showPath flag
        public void setShowPath(boolean showPath) {
            this.showPath = showPath;
            notifyDataSetChanged();  // Refresh the grid to apply changes
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int position) {
            return files.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
            }

            ImageView fileThumbnail = view.findViewById(R.id.fileThumbnail);
            TextView fileName = view.findViewById(R.id.fileName);
            ImageView shareBtn = view.findViewById(R.id.shareButton);
            ImageView deleteBtn = view.findViewById(R.id.deleteButton);
            CheckBox checkBox = view.findViewById(R.id.selectCheckbox);

            File file = files.get(position);

            // Show folder path or file name based on the toggle
            if (showPath) {
                File parentFolderFile = file.getParentFile();
                if (parentFolderFile != null) {
                    fileName.setText(parentFolderFile.getName() + "/");
                } else {
                    fileName.setText(file.getName());  // Fallback if parent folder is null
                }
            } else {
                fileName.setText(file.getName());
            }

            // Set thumbnail based on file type
            if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {
                fileThumbnail.setImageResource(R.drawable.ic_excel);
            } else if (file.getName().endsWith(".ppt") || file.getName().endsWith(".ext4")) {
                fileThumbnail.setImageResource(R.drawable.ic_ppt);
            } else {
                Glide.with(context)
                        .load(file)
                        .placeholder(R.drawable.ic_unknown)
                        .error(R.drawable.ic_unknown)
                        .into(fileThumbnail);
            }

            // File click to open
            view.setOnClickListener(v -> openFile(file));

            // Share button
            shareBtn.setOnClickListener(v -> {
                try {
                    Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(getMimeType(file.getAbsolutePath()));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(shareIntent, "Share file via"));
                } catch (Exception e) {
                    Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });

            // Delete button
            deleteBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete File")
                        .setMessage("Are you sure you want to delete this file?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if (file.delete()) {
                                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                                files.remove(position);
                                notifyDataSetChanged();
                            } else {
                                Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            });

            // Checkbox selection logic
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(selectedFiles.contains(file));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedFiles.add(file);
                else selectedFiles.remove(file);
            });

            return view;
        }

    private String getMimeType(String path) {
            try {
                String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(path)).toString());
                if (extension != null && !extension.isEmpty()) {
                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                    if (mime != null) return mime;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "/";
        }

    }
}
