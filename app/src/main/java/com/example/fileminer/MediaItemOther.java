package com.example.fileminer;

import static java.lang.reflect.Array.get;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
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
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaItemOther {

    String name;
    String path;
    long size;
    long dateModified;
    public boolean isSelected = false;
    MediaItemOther(String name, String path) {
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

class MediaAdapterOther extends ArrayAdapter<MediaItemOther> {
    private final Activity context;
    private final ArrayList<MediaItemOther> mediaItems;

    private boolean showPath = false;
    public MediaAdapterOther(Activity context, ArrayList<MediaItemOther> mediaItems) {
        super(context, R.layout.media_list_item, mediaItems);
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;

        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.media_list_item, parent, false);
        }


        TextView text1 = listItem.findViewById(R.id.mediaName);
        ImageView imageView = listItem.findViewById(R.id.mediaThumbnail);
        ImageView shareButton = listItem.findViewById(R.id.shareButton);
        ImageView deleteButton = listItem.findViewById(R.id.deleteButton);
        CheckBox checkBox = listItem.findViewById(R.id.checkBox);


        MediaItemOther currentItem = mediaItems.get(position);

        if (currentItem != null && currentItem.path != null) {
            text1.setText(currentItem.name);

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

            //=============== Handle checkbox state
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(currentItem.isSelected);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentItem.isSelected = isChecked;

                if (context instanceof OtherFilesActivity) {
                    ((OtherFilesActivity) context).updateSelectionToolbar();
                }
            });
            // ================ Load thumbnail images or use icons based on the file type
            loadThumbnail(imageView, currentItem);

            // ==============Share Button functionality
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

            listItem.setOnClickListener(v -> openFile(currentItem));

            //------------- Delete Button functionality
            deleteButton.setOnClickListener(v -> {
                if (context instanceof OtherFilesActivity) {
                    ((OtherFilesActivity) context).deleteFile(currentItem);
                }
            });
        }

        return listItem;
    }

    public void setShowPath(boolean showPath) {
        this.showPath = showPath;
        notifyDataSetChanged();
    }

    private void loadThumbnail(ImageView imageView, MediaItemOther currentItem) {
        if (currentItem == null || currentItem.path == null || imageView == null) {
            return;
        }

        String filePath = currentItem.path;

        File file = new File(filePath);
        if (!file.exists()) {
            imageView.setImageResource(R.drawable.ic_file);
            return;
        }

        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") ||
                filePath.endsWith(".png") || filePath.endsWith(".gif") ||
                filePath.endsWith(".bmp") || filePath.endsWith(".webp") ||
                filePath.endsWith(".tiff") || filePath.endsWith(".svg")) {

            Glide.with(getContext())
                    .load(file)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        }
        else if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
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

    private void openFile(MediaItemOther currentItem) {
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

