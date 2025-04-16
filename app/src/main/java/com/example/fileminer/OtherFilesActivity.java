package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.net.Uri;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

                // If case-insensitive search is selected, convert both to lowercase
                if (!isCaseSensitive) {
                    searchQuery = searchQuery.toLowerCase();
                    fileName = fileName.toLowerCase();
                }

                // Apply the search logic based on the selected search type
                if (selectedSearchType.equals("Contains") && fileName.contains(searchQuery)) {
                    filteredList.add(file);
                } else if (selectedSearchType.equals("Starts With") && fileName.startsWith(searchQuery)) {
                    filteredList.add(file);
                } else if (selectedSearchType.equals("Ends With") && fileName.endsWith(searchQuery)) {
                    filteredList.add(file);
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
        } else if (item.getItemId() == R.id.action_search) {
            Toast.makeText(this, "Search Clicked", Toast.LENGTH_SHORT).show();

            // Ensure fileList is populated before passing it
            loadFileList();

            // Convert fileList (List<File>) to List<String> (file names)
            List<String> fileNames = new ArrayList<>();
            for (File file : otherFiles) {
                fileNames.add(file.getName());
            }

            // Debugging log to check file list size
            Log.d("SearchBottomSheet", "File list size: " + fileNames.size());

            // Create the bottom sheet and pass the current selected search type and case sensitivity
            SearchBottomSheet bottomSheet = new SearchBottomSheet(this, selectedSearchType, isCaseSensitive, new SearchBottomSheet.OnSearchOptionSelectedListener() {
                @Override
                public void onSearchOptionSelected(String searchType, boolean caseSensitive) {
                    // Update the selected search type and case sensitivity when the user makes a selection
                    selectedSearchType = searchType; // Store the selected search type
                    isCaseSensitive = caseSensitive; // Store the case sensitivity setting

                    String caseSensitivity = caseSensitive ? "Case Sensitive" : "Case Insensitive";
                    Toast.makeText(OtherFilesActivity.this, "Selected: " + searchType + " | " + caseSensitivity, Toast.LENGTH_SHORT).show();


                    // Now use the selected search type and case sensitivity for filtering or other logic
                }
            });

            // Show Bottom Sheet for choosing search type
            bottomSheet.show(getSupportFragmentManager(), "SearchBottomSheet");

            return true;
        }

        // Sort files (if needed)
        sortFiles();
        if (adapter != null) adapter.notifyDataSetChanged();
        return true;
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

        GridAdapter(Activity context, List<File> files) {
            this.context = context;
            this.files = files;
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
            fileName.setText(file.getName());

            // Set thumbnail
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


            // Checkbox selection
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
