//package com.example.fileminer;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.CheckBox;
//import android.widget.GridView;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//public class FileAdapterTwo extends android.widget.BaseAdapter {
//    private Context context;
//    private List<FileItem> fileItems;
//    private List<FileItem> selectedFiles = new ArrayList<>();
//
//    public FileAdapterTwo(Context context, List<FileItem> fileItems) {
//        this.context = context;
//        this.fileItems = fileItems;
//    }
//
//    @Override
//    public int getCount() {
//        return fileItems.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return fileItems.get(position);
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
//            convertView = LayoutInflater.from(context).inflate(R.layout.item_hidden_file, parent, false);
//        }
//
//        FileItem fileItem = fileItems.get(position);
//        TextView fileName = convertView.findViewById(R.id.fileName);
//        CheckBox checkBox = convertView.findViewById(R.id.checkbox);
//
//        fileName.setText(fileItem.getFile().getName());
//
//        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                selectedFiles.add(fileItem);
//            } else {
//                selectedFiles.remove(fileItem);
//            }
//        });
//
//        return convertView;
//    }
//
//    // Get the selected files
//    public List<FileItem> getSelectedFiles() {
//        return selectedFiles;
//    }
//}
