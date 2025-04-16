//package com.example.fileminer;
//
//import android.os.Parcel;
//import android.os.Parcelable;
//import java.io.File;
//
//class MediaItem implements Parcelable {
//    private String name;
//    private String path;
//    private long size; // File size
//    private long dateModified; // File date modified
//    private boolean isSelected; // To track selection state
//
//    public MediaItem(String name, String path) {
//        this.name = name;
//        this.path = path;
//        this.isSelected = false; // Default is not selected
//
//        try {
//            File file = new File(path);
//            if (file.exists()) {
//                this.size = file.length();  // Get file size
//                this.dateModified = file.lastModified(); // Get last modified time
//            } else {
//                this.size = 0;
//                this.dateModified = 0;
//            }
//        } catch (Exception e) {
//            this.size = 0;
//            this.dateModified = 0;
//        }
//    }
//
//    // Getter methods
//    public String getName() {
//        return name;
//    }
//
//    public String getPath() {
//        return path;
//    }
//
//    public long getSize() {
//        return size;
//    }
//
//    public long getDateModified() {
//        return dateModified;
//    }
//
//    public boolean isSelected() {
//        return isSelected;
//    }
//
//    public void setSelected(boolean selected) {
//        isSelected = selected;
//    }
//
//    // Parcelable implementation
//    protected MediaItem(Parcel in) {
//        name = in.readString();
//        path = in.readString();
//        size = in.readLong();
//        dateModified = in.readLong();
//        isSelected = in.readByte() != 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeString(name);
//        dest.writeString(path);
//        dest.writeLong(size);
//        dest.writeLong(dateModified);
//        dest.writeByte((byte) (isSelected ? 1 : 0));
//    }
//
//    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    public static final Creator<MediaItem> CREATOR = new Creator<MediaItem>() {
//        @Override
//        public MediaItem createFromParcel(Parcel in) {
//            return new MediaItem(in);
//        }
//
//        @Override
//        public MediaItem[] newArray(int size) {
//            return new MediaItem[size];
//        }
//    };
//}
