package com.example.fileminer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;

public class RestoredFilesActivity extends Activity {

    private ListView listView;
    private MediaAdapter adapter;
    private ArrayList<MediaItem> restoredFiles;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restored_files);

        listView = findViewById(R.id.listView);
        restoredFiles = new ArrayList<>();

        Intent intent = getIntent();
        String fileType = intent.getStringExtra("fileType");

        Log.d("RestoredFilesActivity", "Received fileType: " + fileType);

        if (fileType != null) {
            switch (fileType) {
                case "Photo":
                    loadMediaFiles(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    break;
                case "Video":
                    loadMediaFiles(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    break;
                case "Audio":
                    loadMediaFiles(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    break;
                case "Document":
                    loadDocumentFiles();
                    break;
                default:
                    loadAllFiles();
                    break;
            }
        }

        adapter = new MediaAdapter(this, restoredFiles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            MediaItem item = restoredFiles.get(position);
            openFile(item.path);
        });
    }

    private void loadMediaFiles(Uri contentUri) {
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        try (Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(0);
                    String displayName = cursor.getString(1);
                    restoredFiles.add(new MediaItem(displayName, filePath));
                    Log.d("RestoredFilesActivity", "File found: " + displayName + " - " + filePath);
                }
            }
        } catch (Exception e) {
            Log.e("RestoredFilesActivity", "Error loading media files", e);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadDocumentFiles() {
        Uri contentUri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        String selection = MediaStore.MediaColumns.MIME_TYPE + " IN (?, ?, ?, ?, ?)";
        String[] selectionArgs = {
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", // PPTX
                "application/vnd.oasis.opendocument.text" // ODT
        };

        try (Cursor cursor = getContentResolver().query(contentUri, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(0);
                    String displayName = cursor.getString(1);
                    restoredFiles.add(new MediaItem(displayName, filePath));
                    Log.d("RestoredFilesActivity", "Document found: " + displayName + " - " + filePath);
                }
            }
        } catch (Exception e) {
            Log.e("RestoredFilesActivity", "Error loading document files", e);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }


    private void loadAllFiles() {
        Uri contentUri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        try (Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(0);
                    String displayName = cursor.getString(1);
                    restoredFiles.add(new MediaItem(displayName, filePath));
                    Log.d("RestoredFilesActivity", "File found: " + displayName + " - " + filePath);
                }
            }
        } catch (Exception e) {
            Log.e("RestoredFilesActivity", "Error loading all files", e);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
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
        return "*/*";
    }


    private static class MediaItem {
        String name;
        String path;

        MediaItem(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

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


            MediaItem currentItem = getItem(position);

            if (currentItem != null) {
                text1.setText(currentItem.name);

                // Image Thumbnail
                if (currentItem.path != null && (currentItem.path.endsWith(".jpg") || currentItem.path.endsWith(".png"))) {
                    imageView.setImageURI(Uri.fromFile(new File(currentItem.path))); // Corrected way
                }
                // Video Thumbnail
                else if (currentItem.path.endsWith(".mp4") || currentItem.path.endsWith(".mkv")) {
                    Bitmap thumbnail = getVideoThumbnail(currentItem.path);
                    if (thumbnail != null) {
                        imageView.setImageBitmap(thumbnail);
                    } else {
                        imageView.setImageResource(R.drawable.ic_video); // Placeholder if thumbnail not found
                    }
                }
                // Audio File
                else if (currentItem.path.endsWith(".mp3") || currentItem.path.endsWith(".wav") || currentItem.path.endsWith(".m4a") ) {
                    imageView.setImageResource(R.drawable.ic_audio); // Corrected audio icon
                }
                // PDF File
                else if (currentItem.path.endsWith(".pdf")) {
                    imageView.setImageResource(R.drawable.ic_pdf);
                }
                else if (currentItem.path.endsWith(".pptx")) {
                    imageView.setImageResource(R.drawable.ic_ppt); // PPTX ke liye icon
                } else if (currentItem.path.endsWith(".odt")) {
                    imageView.setImageResource(R.drawable.ic_excel); // ODT ke liye icon
                }

                // Other Files
                else {
                    imageView.setImageResource(R.drawable.ic_file);
                }
            }

            return listItem;
        }

            private Bitmap getVideoThumbnail(String videoPath) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            return retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        }
    }
}
