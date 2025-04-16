package com.example.fileminer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView; // Add this import


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import android.content.DialogInterface; // Import this

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestoredFilesActivity extends AppCompatActivity {

    private GridView listView;
    private MediaAdapter adapter;
    private ArrayList<MediaItem> restoredFiles;
    private String currentSort = "name"; // Default sorting by name
    private boolean isAscending = true; // Default sort order

    private ArrayList<MediaItem> selectedFiles;
    ProgressBar progressBar;
    private Context context;

    private String selectedSearchType = "Contains"; // Declare variable globally
    private List<String> fileList = new ArrayList<>();
    TextView noResultsText;
    private List<MediaItem> fullMediaItemList = new ArrayList<>();
    private  boolean isCaseSensitive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restored_files);

        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.gridView);
        noResultsText = findViewById(R.id.noResultsText);
        restoredFiles = new ArrayList<>();
        selectedFiles = new ArrayList<>();

        // ✅ Initialize fullMediaItemList
        fullMediaItemList = new ArrayList<>();

        adapter = new MediaAdapter(this, restoredFiles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            MediaItem item = restoredFiles.get(position);
            openFile(item.path);
        });

        Intent intent = getIntent();
        String fileType = intent.getStringExtra("fileType");

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


    private void deleteFile(MediaItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    File file = new File(item.path);
                    if (file.exists() && file.delete()) {
                        restoredFiles.remove(item);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No, Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteSelectedFiles() {
        ArrayList<MediaItem> itemsToDelete = new ArrayList<>();
        for (MediaItem item : restoredFiles) {
            if (item.isSelected) {
                File file = new File(item.path);
                if (file.exists()) {
                    if (file.delete()) {
                        itemsToDelete.add(item);
                    } else {
                        Toast.makeText(this, "Failed to delete: " + item.name, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        if (!itemsToDelete.isEmpty()) {
            restoredFiles.removeAll(itemsToDelete);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Selected files deleted successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No files were deleted.", Toast.LENGTH_SHORT).show();
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
                        if (!query.isEmpty()) {
                            filterFiles(query);
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterFiles(newText);
                        return true;
                    }
                });

                // Ensure search icon is always visible
                searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            }
        }

        return true;
    }
    private void filterFiles(String query) {
        if (query == null) query = "";
        String searchQuery = query.trim(); // No toLowerCase() for case-sensitive search
        List<MediaItem> filteredList = new ArrayList<>();

        // If the search query is empty, display all files
        if (searchQuery.isEmpty()) {
            filteredList.addAll(fullMediaItemList);
        } else {
            for (MediaItem item : fullMediaItemList) {
                if (item != null && item.name != null) {
                    String fileName = item.name; // Keep original case

                    // Check case-sensitivity setting
                    if (!isCaseSensitive) {
                        // For case-insensitive search, convert both the search query and file name to lowercase
                        searchQuery = searchQuery.toLowerCase();
                        fileName = fileName.toLowerCase();
                    }

                    // Apply search logic based on selected search type
                    switch (selectedSearchType) {
                        case "Contains":
                            if (fileName.contains(searchQuery)) filteredList.add(item);
                            break;
                        case "Starts With":
                            if (fileName.startsWith(searchQuery)) filteredList.add(item);
                            break;
                        case "Ends With":
                            if (fileName.endsWith(searchQuery)) filteredList.add(item);
                            break;
                        default:
                            if (fileName.contains(searchQuery)) filteredList.add(item);
                            break;
                    }
                }
            }
        }

        restoredFiles.clear();
        restoredFiles.addAll(filteredList);

        // Update UI with filtered results
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


    private void loadFileList() {
        fileList.clear(); // Clear the list before adding new items
        for (MediaItem item : restoredFiles) {
            fileList.add(item.name); // Assuming 'name' contains the filename
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sortByName) {
            currentSort = "name";
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (item.getItemId() == R.id.sortBySize) {
            currentSort = "size";
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (item.getItemId() == R.id.sortByTime) {
            currentSort = "time";
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (item.getItemId() == R.id.sortOrderToggle) {
            isAscending = !isAscending;
            item.setTitle(isAscending ? "Ascending" : "Descending");
            sortFiles();
            adapter.notifyDataSetChanged();
            return true;
        } else if (item.getItemId() == R.id.deleteSelected) {
            deleteSelectedFiles();
            return true;
        }
        if (item.getItemId() == R.id.action_search) {
            Toast.makeText(this, "Search Clicked", Toast.LENGTH_SHORT).show();

            // Ensure fileList is populated before passing it
            loadFileList();

            // Debugging log to check file list size
            Log.d("SearchBottomSheet", "File list size: " + fileList.size());

            // Create the bottom sheet and pass the current selected search type and case sensitivity
            SearchBottomSheet bottomSheet = new SearchBottomSheet(this, selectedSearchType, isCaseSensitive, new SearchBottomSheet.OnSearchOptionSelectedListener() {
                @Override
                public void onSearchOptionSelected(String searchType, boolean caseSensitive) {
                    // Update the selected search type and case sensitivity when the user makes a selection
                    selectedSearchType = searchType; // Store the selected search type
                    isCaseSensitive = caseSensitive; // Store case sensitivity setting

                    String caseSensitivity = caseSensitive ? "Case Sensitive" : "Case Insensitive";
                    Toast.makeText(RestoredFilesActivity.this, "Selected: " + searchType + " | " + caseSensitivity, Toast.LENGTH_SHORT).show();


                    // Now use the selected search type and case sensitivity for filtering or other logic
                }
            });

            // Show Bottom Sheet for choosing search type
            bottomSheet.show(getSupportFragmentManager(), "SearchBottomSheet");

            return true;
        }

        return super.onOptionsItemSelected(item);
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
                        mediaItems.add(new MediaItem(displayName, filePath));
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

            // ✅ Sync fullMediaItemList for filtering
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

            // ✅ Sync fullMediaItemList
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
                        mediaItems.add(new MediaItem(displayName, filePath));
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

            // ✅ Sync fullMediaItemList
            fullMediaItemList.clear();
            fullMediaItemList.addAll(restoredFiles);

            sortFiles();
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE); // Hide progress bar
        }
    }

    private static class MediaItem {
        String name;
        String path;
        long size;
        long dateModified;
        boolean isSelected = false;

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
    }

    // MediaAdapter class
    private class MediaAdapter extends ArrayAdapter<MediaItem> {

        MediaAdapter(Activity context, ArrayList<MediaItem> mediaItems) {
            super(context, R.layout.media_list_item, mediaItems);
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

                // Handle checkbox state
                checkBox.setOnCheckedChangeListener(null);  // Remove any previous listener
                checkBox.setChecked(currentItem.isSelected);

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> currentItem.isSelected = isChecked);

                // Load thumbnail images or use icons based on the file type
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

        private void loadThumbnail(ImageView imageView, MediaItem currentItem) {
            String filePath = currentItem.path;

            if (filePath.endsWith(".jpg") || filePath.endsWith(".png")) {
                Glide.with(getContext())
                        .load(new File(filePath))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache the images
                        .into(imageView);
            } else if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
                // Use Glide for video thumbnails as well
                Glide.with(getContext())
                        .asBitmap()
                        .load(Uri.fromFile(new File(filePath)))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache the video thumbnails
                        .placeholder(R.drawable.ic_video)  // Show default icon while loading
                        .error(R.drawable.ic_video)  // Show default icon if an error occurs
                        .into(imageView);  // Set the image to ImageView
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

        // Get MIME type based on file extension
        private String getMimeType(String filePath) {
            String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            return mime.getMimeTypeFromExtension(extension);
        }

    }
}
