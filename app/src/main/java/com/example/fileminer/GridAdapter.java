//package com.example.fileminer;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.ImageView;
//import android.widget.TextView;
//import java.io.File;
//import java.util.List;
//
//public class GridAdapter extends BaseAdapter {
//    private Context context;
//    private List<File> files;
//
//    public GridAdapter(Context context, List<File> files) {
//        this.context = context;
//        this.files = files;
//    }
//
//    @Override
//    public int getCount() {
//        return files.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return files.get(position);
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
//            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
//        }
//
//        ImageView fileThumbnail = convertView.findViewById(R.id.fileThumbnail);
//        TextView fileName = convertView.findViewById(R.id.fileName);
////        TextView filePath = convertView.findViewById(R.id.filePath);
//
//        File file = files.get(position);
//        fileName.setText(file.getName());
////        filePath.setText(file.getAbsolutePath());
//
//        if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {
//            fileThumbnail.setImageResource(R.drawable.ic_excel);
//        } else if (file.getName().endsWith(".ppt") || file.getName().endsWith(".ext4")) {
//            fileThumbnail.setImageResource(R.drawable.ic_ppt);
//        } else {
//            fileThumbnail.setImageResource(R.drawable.ic_unknown);
//        }
//
//        return convertView;
//    }
//}
