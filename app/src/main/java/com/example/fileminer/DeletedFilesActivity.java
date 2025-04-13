//package com.example.fileminer;
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.Intent;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.graphics.drawable.Drawable;
//import android.media.MediaMetadataRetriever;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.ViewGroup;
//import android.webkit.MimeTypeMap;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.CheckBox;
//import android.widget.GridView;
//import android.widget.ImageView;
//import android.widget.ListView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.content.FileProvider;
//
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.engine.DiskCacheStrategy;
//import com.bumptech.glide.request.target.SimpleTarget;
//import com.bumptech.glide.request.transition.Transition;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Collections;
//
//public class DeletedFilesActivity extends Activity {
//
//    private GridView listView;
//    private DeletedFilesActivity.MediaAdapter adapter;
//    private ArrayList<DeletedFilesActivity.MediaItem> restoredFiles;
//    private String currentSort = "name"; // Default sorting by name
//    private boolean isAscending = true; // Default sort order
//
//    private ArrayList<DeletedFilesActivity.MediaItem> selectedFiles;
//    ProgressBar progressBar;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_deleted_files);
//
//        // Receive the deleted files from the intent
//        progressBar = findViewById(R.id.progressBar);
//        listView = findViewById(R.id.gridView);
//        restoredFiles = new ArrayList<>();
//        selectedFiles = new ArrayList<>();
//
//        Intent intent = getIntent();
//        String fileType = intent.getStringExtra("fileType");
//
//        Log.d("RestoredFilesActivity", "Received fileType: " + fileType);
//
//        if (fileType != null) {
//            switch (fileType) {
//                case "Photo":
//                    new DeletedFilesActivity.LoadMediaFilesTask().execute(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    break;
//                case "Video":
//                    new DeletedFilesActivity.LoadMediaFilesTask().execute(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
//                    break;
//                case "Audio":
//                    new DeletedFilesActivity.LoadMediaFilesTask().execute(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
//                    break;
//                case "Document":
//                    new DeletedFilesActivity.LoadDocumentFilesTask().execute();
//                    break;
//                default:
//                    new DeletedFilesActivity.LoadAllFilesTask().execute();
//                    break;
//            }
//        }
//
//        adapter = new DeletedFilesActivity.MediaAdapter(this, restoredFiles);
//        listView.setAdapter(adapter);
//
//        listView.setOnItemClickListener((parent, view, position, id) -> {
//            DeletedFilesActivity.MediaItem item = restoredFiles.get(position);
//            openFile(item.path);
//        });
//    }
//
//    private void deleteFile(DeletedFilesActivity.MediaItem item) {
//        new AlertDialog.Builder(this)
//                .setTitle("Delete File")
//                .setMessage("Are you sure you want to delete this file?")
//                .setPositiveButton("Yes, Delete", (dialog, which) -> {
//                    File file = new File(item.path);
//                    if (file.exists() && file.delete()) {
//                        restoredFiles.remove(item);
//                        adapter.notifyDataSetChanged();
//                        Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .setNegativeButton("No, Cancel", (dialog, which) -> dialog.dismiss())
//                .show();
//    }
//
//    private void deleteSelectedFiles() {
//        ArrayList<DeletedFilesActivity.MediaItem> itemsToDelete = new ArrayList<>();
//        for (DeletedFilesActivity.MediaItem item : restoredFiles) {
//            if (item.isSelected) {
//                File file = new File(item.path);
//                if (file.exists()) {
//                    if (file.delete()) {
//                        itemsToDelete.add(item);
//                    } else {
//                        Toast.makeText(this, "Failed to delete: " + item.name, Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//        }
//
//        if (!itemsToDelete.isEmpty()) {
//            restoredFiles.removeAll(itemsToDelete);
//            adapter.notifyDataSetChanged();
//            Toast.makeText(this, "Selected files deleted successfully!", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "No files were deleted.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void openFile(String filePath) {
//        if (filePath == null) return;
//
//        File file = new File(filePath);
//        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
//
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(uri, getMimeType(filePath));
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        try {
//            startActivity(intent);
//        } catch (Exception e) {
//            Log.e("RestoredFilesActivity", "No suitable app found to open this file", e);
//        }
//    }
//
//    private String getMimeType(String filePath) {
//        if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
//            return "video/*";
//        } else if (filePath.endsWith(".mp3") || filePath.endsWith(".wav") || filePath.endsWith(".m4a")) {
//            return "audio/*";
//        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".png")) {
//            return "image/*";
//        } else if (filePath.endsWith(".pdf")) {
//            return "application/pdf";
//        } else if (filePath.endsWith(".doc") || filePath.endsWith(".docx")) {
//            return "application/msword";
//        } else if (filePath.endsWith(".pptx")) {
//            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
//        } else if (filePath.endsWith(".odt")) {
//            return "application/vnd.oasis.opendocument.text";
//        }
//        return "/";
//    }
//
//    private void sortFiles() {
//        if ("name".equals(currentSort)) {
//            Collections.sort(restoredFiles, (a, b) -> isAscending ?
//                    a.name.compareToIgnoreCase(b.name) :
//                    b.name.compareToIgnoreCase(a.name));
//        } else if ("size".equals(currentSort)) {
//            Collections.sort(restoredFiles, (a, b) -> {
//                if (a.size == 0 && b.size == 0) return 0;
//                else if (a.size == 0) return isAscending ? 1 : -1;
//                else if (b.size == 0) return isAscending ? -1 : 1;
//                return isAscending ? Long.compare(a.size, b.size) : Long.compare(b.size, a.size);
//            });
//        } else if ("time".equals(currentSort)) {
//            Collections.sort(restoredFiles, (a, b) ->
//                    isAscending ? Long.compare(a.dateModified, b.dateModified) : Long.compare(b.dateModified, a.dateModified));
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.sort_menu, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.sortByName) {
//            currentSort = "name";
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        } else if (item.getItemId() == R.id.sortBySize) {
//            currentSort = "size";
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        } else if (item.getItemId() == R.id.sortByTime) {
//            currentSort = "time";
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        } else if (item.getItemId() == R.id.sortOrderToggle) {
//            isAscending = !isAscending;
//            item.setTitle(isAscending ? "Ascending" : "Descending");
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            return true;
//        } else if (item.getItemId() == R.id.deleteSelected) {
//            deleteSelectedFiles();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private class LoadMediaFilesTask extends AsyncTask<Uri, Void, ArrayList<DeletedFilesActivity.MediaItem>> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressBar.setVisibility(View.VISIBLE); // Show progress bar
//        }
//
//        @Override
//        protected ArrayList<DeletedFilesActivity.MediaItem> doInBackground(Uri... uris) {
//            ArrayList<DeletedFilesActivity.MediaItem> mediaItems = new ArrayList<>();
//            Uri contentUri = uris[0];
//            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
//
//            try (Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null)) {
//                if (cursor != null) {
//                    while (cursor.moveToNext()) {
//                        String filePath = cursor.getString(0);
//                        String displayName = cursor.getString(1);
//                        mediaItems.add(new DeletedFilesActivity.MediaItem(displayName, filePath));
//                    }
//                }
//            } catch (Exception e) {
//                Log.e("RestoredFilesActivity", "Error loading media files", e);
//            }
//
//            return mediaItems;
//        }
//
//        @Override
//        protected void onPostExecute(ArrayList<DeletedFilesActivity.MediaItem> mediaItems) {
//            restoredFiles.clear();
//            restoredFiles.addAll(mediaItems);
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            progressBar.setVisibility(View.GONE); // Hide progress bar
//        }
//    }
//    private class LoadDocumentFilesTask extends AsyncTask<Void, Void, ArrayList<DeletedFilesActivity.MediaItem>> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressBar.setVisibility(View.VISIBLE); // Show progress bar
//        }
//
//        @Override
//        protected ArrayList<DeletedFilesActivity.MediaItem> doInBackground(Void... voids) {
//            ArrayList<DeletedFilesActivity.MediaItem> mediaItems = new ArrayList<>();
//            Uri contentUri = MediaStore.Files.getContentUri("external");
//            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
//            String selection = MediaStore.MediaColumns.MIME_TYPE + " IN (?, ?, ?, ?, ?)";
//            String[] selectionArgs = {
//                    "application/pdf",
//                    "application/msword",
//                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
//                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
//                    "application/vnd.oasis.opendocument.text"
//            };
//
//            try (Cursor cursor = getContentResolver().query(contentUri, projection, selection, selectionArgs, null)) {
//                if (cursor != null) {
//                    while (cursor.moveToNext()) {
//                        String filePath = cursor.getString(0);
//                        String displayName = cursor.getString(1);
//                        mediaItems.add(new DeletedFilesActivity.MediaItem(displayName, filePath));
//                    }
//                }
//            } catch (Exception e) {
//                Log.e("RestoredFilesActivity", "Error loading document files", e);
//            }
//
//            return mediaItems;
//        }
//
//        @Override
//        protected void onPostExecute(ArrayList<DeletedFilesActivity.MediaItem> mediaItems) {
//            restoredFiles.clear();
//            restoredFiles.addAll(mediaItems);
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            progressBar.setVisibility(View.GONE); // Hide progress bar
//        }
//    }
//    private class LoadAllFilesTask extends AsyncTask<Void, Void, ArrayList<DeletedFilesActivity.MediaItem>> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressBar.setVisibility(View.VISIBLE); // Show progress bar
//        }
//
//        @Override
//        protected ArrayList<DeletedFilesActivity.MediaItem> doInBackground(Void... voids) {
//            ArrayList<DeletedFilesActivity.MediaItem> mediaItems = new ArrayList<>();
//            Uri contentUri = MediaStore.Files.getContentUri("external");
//            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
//
//            try (Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null)) {
//                if (cursor != null) {
//                    while (cursor.moveToNext()) {
//                        String filePath = cursor.getString(0);
//                        String displayName = cursor.getString(1);
//                        mediaItems.add(new DeletedFilesActivity.MediaItem(displayName, filePath));
//                    }
//                }
//            } catch (Exception e) {
//                Log.e("RestoredFilesActivity", "Error loading all files", e);
//            }
//
//            return mediaItems;
//        }
//
//        @Override
//        protected void onPostExecute(ArrayList<DeletedFilesActivity.MediaItem> mediaItems) {
//            restoredFiles.clear();
//            restoredFiles.addAll(mediaItems);
//            sortFiles();
//            adapter.notifyDataSetChanged();
//            progressBar.setVisibility(View.GONE); // Hide progress bar
//        }
//    }
//
//
//    private static class MediaItem {
//        String name;
//        String path;
//        long size;
//        long dateModified;
//        boolean isSelected = false;
//
//        MediaItem(String name, String path) {
//            this.name = name;
//            this.path = path;
//
//            try {
//                File file = new File(path);
//                if (file.exists()) {
//                    this.size = file.length();
//                    this.dateModified = file.lastModified();
//                } else {
//                    Log.e("MediaItem", "File not found: " + path);
//                    this.size = 0;
//                    this.dateModified = 0;
//                }
//            } catch (Exception e) {
//                Log.e("MediaItem", "Error retrieving file size or dateModified for file: " + path, e);
//                this.size = 0;
//                this.dateModified = 0;
//            }
//        }
//    }
//
//    // MediaAdapter class
//    private class MediaAdapter extends ArrayAdapter<DeletedFilesActivity.MediaItem> {
//
//        MediaAdapter(Activity context, ArrayList<DeletedFilesActivity.MediaItem> mediaItems) {
//            super(context, R.layout.media_list_item, mediaItems);
//        }
//
//        @NonNull
//        @Override
//        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//            View listItem = convertView;
//
//            if (listItem == null) {
//                listItem = getLayoutInflater().inflate(R.layout.media_list_item, parent, false);
//            }
//
//            TextView text1 = listItem.findViewById(R.id.mediaName);
//            ImageView imageView = listItem.findViewById(R.id.mediaThumbnail);
//            ImageView shareButton = listItem.findViewById(R.id.shareButton);
//            ImageView deleteButton = listItem.findViewById(R.id.deleteButton);
//            CheckBox checkBox = listItem.findViewById(R.id.checkBox);
//
//            DeletedFilesActivity.MediaItem currentItem = getItem(position);
//
//            if (currentItem != null && currentItem.path != null) {
//                text1.setText(currentItem.name);
//
//                // Handle checkbox state
//                checkBox.setOnCheckedChangeListener(null);  // Remove any previous listener
//                checkBox.setChecked(currentItem.isSelected);
//
//                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> currentItem.isSelected = isChecked);
//
//                // Load thumbnail images or use icons based on the file type
//                loadThumbnail(imageView, currentItem);
//
//                // Share Button functionality
//                shareButton.setOnClickListener(v -> {
//                    File file = new File(currentItem.path);
//                    Uri uri = FileProvider.getUriForFile(
//                            getContext(),
//                            getContext().getPackageName() + ".provider",
//                            file
//                    );
//
//                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
//                    shareIntent.setType(getMimeType(currentItem.path));
//                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
//                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//                    try {
//                        getContext().startActivity(Intent.createChooser(shareIntent, "Share file via"));
//                    } catch (Exception e) {
//                        Toast.makeText(getContext(), "Error sharing file", Toast.LENGTH_SHORT).show();
//                        e.printStackTrace();
//                    }
//                });
//
//                // Open file on item click
//                listItem.setOnClickListener(v -> openFile(currentItem));
//
//                // Delete Button functionality
//                deleteButton.setOnClickListener(v -> deleteFile(currentItem));
//            }
//
//            return listItem;
//        }
//
//        private void loadThumbnail(ImageView imageView, DeletedFilesActivity.MediaItem currentItem) {
//            String filePath = currentItem.path;
//
//            if (filePath.endsWith(".jpg") || filePath.endsWith(".png")) {
//                Glide.with(getContext())
//                        .load(new File(filePath))
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache the images
//                        .into(imageView);
//            } else if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
//                // Use Glide for video thumbnails as well
//                Glide.with(getContext())
//                        .asBitmap()
//                        .load(Uri.fromFile(new File(filePath)))
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache the video thumbnails
//                        .placeholder(R.drawable.ic_video)  // Show default icon while loading
//                        .error(R.drawable.ic_video)  // Show default icon if an error occurs
//                        .into(imageView);  // Set the image to ImageView
//            } else if (filePath.endsWith(".mp3") || filePath.endsWith(".wav") || filePath.endsWith(".m4a")) {
//                imageView.setImageResource(R.drawable.ic_audio);
//            } else if (filePath.endsWith(".pdf")) {
//                imageView.setImageResource(R.drawable.ic_pdf);
//            } else if (filePath.endsWith(".pptx")) {
//                imageView.setImageResource(R.drawable.ic_ppt);
//            } else if (filePath.endsWith(".odt")) {
//                imageView.setImageResource(R.drawable.ic_excel);
//            } else {
//                imageView.setImageResource(R.drawable.ic_file);
//            }
//        }
//
//        private void openFile(DeletedFilesActivity.MediaItem currentItem) {
//            try {
//                File file = new File(currentItem.path);
//                Uri uri = FileProvider.getUriForFile(
//                        getContext(),
//                        getContext().getPackageName() + ".provider",
//                        file
//                );
//                Intent openIntent = new Intent(Intent.ACTION_VIEW);
//                openIntent.setDataAndType(uri, getMimeType(currentItem.path));
//                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//                getContext().startActivity(Intent.createChooser(openIntent, "Open with"));
//            } catch (Exception e) {
//                Toast.makeText(getContext(), "No app found to open this file", Toast.LENGTH_SHORT).show();
//                e.printStackTrace();
//            }
//        }
//
//        // Get MIME type based on file extension
//        private String getMimeType(String filePath) {
//            String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
//            MimeTypeMap mime = MimeTypeMap.getSingleton();
//            return mime.getMimeTypeFromExtension(extension);
//        }
//
//    }
//}
