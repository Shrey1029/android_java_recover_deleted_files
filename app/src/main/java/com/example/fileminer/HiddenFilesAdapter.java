package com.example.fileminer;

import android.content.Context;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class HiddenFilesAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> files;

    public HiddenFilesAdapter(Context context, List<String> files) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_hidden_file, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.imageView);

        String filePath = files.get(position);
        File file = new File(filePath);

        if (isImage(file)) {
            Glide.with(context).load(file).into(imageView);
        } else if (isVideo(file)) {
            imageView.setImageBitmap(ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MINI_KIND));
        }

        return convertView;
    }

    private boolean isImage(File file) {
        String[] photoExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        String fileName = file.getName().toLowerCase();
        for (String ext : photoExtensions) {
            if (fileName.endsWith(ext)) return true;
        }
        return false;
    }

    private boolean isVideo(File file) {
        String[] videoExtensions = {".mp4", ".mkv", ".avi", ".mov", ".flv"};
        String fileName = file.getName().toLowerCase();
        for (String ext : videoExtensions) {
            if (fileName.endsWith(ext)) return true;
        }
        return false;
    }
}
