//package com.example.fileminer;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.BaseAdapter;
//import java.util.List;
//
//public class MediaAdapter extends BaseAdapter {
//    private Context context;
//    private List<MediaItem> mediaItems;
//    private LayoutInflater inflater;
//
//    public MediaAdapter(Context context, List<MediaItem> mediaItems) {
//        this.context = context;
//        this.mediaItems = mediaItems;
//        this.inflater = LayoutInflater.from(context);
//    }
//
//    @Override
//    public int getCount() {
//        return mediaItems.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return mediaItems.get(position);
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        if (convertView == null) {
//            convertView = inflater.inflate(R.layout.media_list_item, parent, false);
//        }
//
//        TextView fileName = convertView.findViewById(R.id.fileName);
//        ImageView fileIcon = convertView.findViewById(R.id.fileIcon);
//
//        MediaItem mediaItem = mediaItems.get(position);
//        fileName.setText(mediaItem.getName());
//
//        // Set icon based on file type
//        if (mediaItem.getPath().endsWith(".mp4") || mediaItem.getPath().endsWith(".mkv")) {
//            fileIcon.setImageResource(R.drawable.ic_video);
//        } else if (mediaItem.getPath().endsWith(".mp3") || mediaItem.getPath().endsWith(".wav")) {
//            fileIcon.setImageResource(R.drawable.ic_audio);
//        } else if (mediaItem.getPath().endsWith(".jpg") || mediaItem.getPath().endsWith(".png")) {
//            fileIcon.setImageResource(R.drawable.ic_photo);
//        } else if (mediaItem.getPath().endsWith(".pdf")) {
//            fileIcon.setImageResource(R.drawable.ic_pdf);
//        } else {
//            fileIcon.setImageResource(R.drawable.ic_file);
//        }
//
//        return convertView;
//    }
//}
