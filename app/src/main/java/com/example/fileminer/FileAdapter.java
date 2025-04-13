package com.example.fileminer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class FileAdapter extends BaseAdapter {
    private Context context;
    private List<File> files;
    private Map<String, Long> fileTimestamps = new HashMap<>();
    private boolean isFilesLoaded = false;

    public FileAdapter(Context context, List<File> files) {
        this.context = context;
        this.files = filterDeletedFiles(files);
        cacheFileTimestamps();
        isFilesLoaded = true;
    }

    private List<File> filterDeletedFiles(List<File> files) {
        return files.stream().filter(File::exists).toList(); // Only show files that still exist (not permanently deleted)
    }

    private void cacheFileTimestamps() {
        for (File file : files) {
            fileTimestamps.put(file.getAbsolutePath(), file.lastModified());
        }
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.file_item, parent, false);
        }

        ImageView fileIcon = convertView.findViewById(R.id.fileIcon);
        TextView fileName = convertView.findViewById(R.id.fileName);

        File file = files.get(position);

        if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            fileIcon.setImageBitmap(bitmap);
            fileName.setVisibility(View.GONE);
        } else if (file.getName().endsWith(".mp4") || file.getName().endsWith(".avi")) {
            Bitmap videoThumbnail = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
            if (videoThumbnail != null) {
                fileIcon.setImageBitmap(videoThumbnail);
            } else {
                fileIcon.setImageResource(R.drawable.ic_video);
            }
            fileName.setText(file.getName());
            fileName.setVisibility(View.VISIBLE);
        } else if (file.getName().endsWith(".pdf")) {
            fileIcon.setImageResource(R.drawable.ic_pdf);
            fileName.setText(file.getName());
            fileName.setVisibility(View.VISIBLE);
        } else if (file.getName().endsWith(".mp3") || file.getName().endsWith(".wav")) {
            fileIcon.setImageResource(R.drawable.ic_audio);
            fileName.setText(file.getName());
            fileName.setVisibility(View.VISIBLE);
        } else if (file.getName().endsWith(".doc") ||  file.getName().endsWith(".docx")) {
            fileIcon.setImageResource(R.drawable.ic_pdf);
            fileName.setText(file.getName());
            fileName.setVisibility(View.VISIBLE);
        } else {
            fileIcon.setImageResource(R.drawable.ic_file);
            fileName.setText(file.getName());
            fileName.setVisibility(View.VISIBLE);
        }

        convertView.setOnClickListener(v -> {
            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, getMimeType(file.getName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        });

        return convertView;
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) return "image/*";
        if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")) return "video/*";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".doc") ||  fileName.endsWith(".docx")) return "application/msword";
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) return "audio/*";
        return "/";

    }
}
