package com.example.fileminer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaItemDeleted {

    String name;
    String path;
    long size;
    long dateModified;
    public boolean isSelected = false;

    MediaItemDeleted(String name, String path) {
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

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
    public String getFilePath() {
        return path;
    }
    public void setFilePath(String newPath) {
        this.path = newPath;
    }
}
class MediaAdapterDeletd extends ArrayAdapter<MediaItemDeleted> {

    private Context context;
    private List<MediaItemDeleted> mediaItems;
    private boolean showPath = false;

    public MediaAdapterDeletd(@NonNull Context context, @NonNull List<MediaItemDeleted> mediaItems) {
        super(context, R.layout.media_list_item, mediaItems);
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @Override
    public int getCount() {
        return mediaItems != null ? mediaItems.size() : 0;
    }

    @Override
    public MediaItemDeleted getItem(int position) {
        if (mediaItems != null && position >= 0 && position < mediaItems.size()) {
            return mediaItems.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;

    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        MediaItemDeleted currentItem = getItem(position);
        if (currentItem == null) {
            return new View(getContext());
        }

        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.media_list_item, parent, false);
        }

        // View references
        TextView text1 = listItem.findViewById(R.id.mediaName);
        ImageView imageView = listItem.findViewById(R.id.mediaThumbnail);
        ImageView shareButton = listItem.findViewById(R.id.shareButton);
        ImageView deleteButton = listItem.findViewById(R.id.deleteButton);
        CheckBox checkBox = listItem.findViewById(R.id.checkBox);

        // Get current item
        if (currentItem != null && currentItem.path != null) {
            if (showPath) {
                File file = new File(currentItem.path);
                File parentFolderFile = file.getParentFile();

                if (parentFolderFile != null) {
                    String parentFolder = parentFolderFile.getName();
                    text1.setText(parentFolder + "/");
                } else {
                    text1.setText(currentItem.name);
                }
            } else {
                text1.setText(currentItem.name);
            }

            // Checkbox
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(currentItem.isSelected);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentItem.isSelected = isChecked;
                if (context instanceof AllDataRecovery) {
                    ((AllDataRecovery) context).updateSelectionToolbar();
                }
            });


            //----- Thumbnail loader
            loadThumbnail(imageView, currentItem);


           //----Share Button functionality
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

            //----Open file on item click
            listItem.setOnClickListener(v -> openFile(currentItem));

            //-----Delete Button functionality
            deleteButton.setOnClickListener(v -> {
                if (context instanceof AllDataRecovery) {
                    ((AllDataRecovery) context).deleteFile(currentItem);
                }
            });
        }

        return listItem;
    }

    public void setShowPath(boolean showPath) {
        this.showPath = showPath;
        notifyDataSetChanged();
    }

    private void loadThumbnail(ImageView imageView, MediaItemDeleted currentItem) {
        if (currentItem == null || currentItem.path == null || imageView == null) {
            return;
        }

        String filePath = currentItem.path;

        File file = new File(filePath);
        if (!file.exists()) {
            imageView.setImageResource(R.drawable.ic_file);
            return;
        }

        if (filePath.endsWith(".jpg") || filePath.endsWith(".png")) {
            Glide.with(getContext())
                    .load(file)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } else if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
            Glide.with(getContext())
                    .asBitmap()
                    .load(Uri.fromFile(file))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_video)
                    .error(R.drawable.ic_video)
                    .into(imageView);
        } else if (filePath.endsWith(".mp3") || filePath.endsWith(".wav") || filePath.endsWith(".m4a")) {
            imageView.setImageResource(R.drawable.ic_audio);
        } else if (filePath.endsWith(".pdf")) {
            imageView.setImageResource(R.drawable.ic_pdf);
        } else if (filePath.endsWith(".pptx")) {
            imageView.setImageResource(R.drawable.ic_ppt);
        } else if (filePath.endsWith(".odt") || filePath.endsWith(".xlsx") || filePath.endsWith(".xls")) {
            imageView.setImageResource(R.drawable.ic_excel);
        } else {
            imageView.setImageResource(R.drawable.ic_file);
        }
    }
    private void openFile(MediaItemDeleted currentItem) {
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

    private String getMimeType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(extension);

    }
}
