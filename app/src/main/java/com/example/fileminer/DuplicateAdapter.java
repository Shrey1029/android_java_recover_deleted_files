//package com.example.fileminer;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.media.ThumbnailUtils;
//import android.os.Handler;
//import android.os.Looper;
//import android.provider.MediaStore;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.request.RequestOptions;
//
//import java.io.File;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class DuplicateAdapter extends RecyclerView.Adapter<DuplicateAdapter.FileViewHolder> {
//
//    private final Context context;
//    private List<File> fileList;
//    private boolean showPaths;
//    private OnFileClickListener onFileClickListener;
//
//    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
//    private final Handler mainHandler = new Handler(Looper.getMainLooper());
//
//    public interface OnFileClickListener {
//        void onFileClick(File file);
//    }
//
//    public void setOnFileClickListener(OnFileClickListener listener) {
//        this.onFileClickListener = listener;
//    }
//
//    public DuplicateAdapter(Context context, List<File> fileList, boolean showPaths) {
//        this.context = context;
//        this.fileList = fileList;
//        this.showPaths = showPaths;
//    }
//
//    public void setShowPaths(boolean showPaths) {
//        this.showPaths = showPaths;
//        notifyDataSetChanged();
//    }
//
//    public void updateList(List<File> newList) {
//        this.fileList = newList;
//        notifyDataSetChanged();
//    }
//
//    @Override
//    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.duplicate_fileitem, parent, false);
//        return new FileViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(FileViewHolder holder, int position) {
//        File file = fileList.get(position);
//        String path = file.getAbsolutePath().toLowerCase();
//
//        holder.fileName1.setText(file.getName());
//        holder.filePath.setText(file.getAbsolutePath());
//        holder.filePath.setVisibility(showPaths ? View.VISIBLE : View.GONE);
//
//        // Reset thumbnail to default
//        holder.thumbnail.setImageResource(R.drawable.ic_file);
//
//        if (isImage(path)) {
//            Glide.with(context)
//                    .load(file)
//                    .apply(new RequestOptions()
//                            .centerCrop()
//                            .override(200, 200)
//                            .placeholder(R.drawable.ic_file)
//                            .error(R.drawable.ic_file))
//                    .into(holder.thumbnail);
//        } else if (isVideo(path)) {
//            executorService.execute(() -> {
//                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
//                mainHandler.post(() -> {
//                    if (thumbnail != null) {
//                        holder.thumbnail.setImageBitmap(thumbnail);
//                    } else {
//                        holder.thumbnail.setImageResource(R.drawable.ic_video);
//                    }
//                });
//            });
//        } else if (isAudio(path)) {
//            holder.thumbnail.setImageResource(R.drawable.ic_audio); // Use your custom audio icon
//        } else {
//            holder.thumbnail.setImageResource(R.drawable.ic_file);
//        }
//
//        holder.itemView.setOnClickListener(v -> {
//            if (onFileClickListener != null) {
//                onFileClickListener.onFileClick(file);
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return fileList.size();
//    }
//
//    private boolean isImage(String path) {
//        return path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")
//                || path.endsWith(".webp") || path.endsWith(".gif");
//    }
//
//    private boolean isVideo(String path) {
//        return path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".3gp") || path.endsWith(".avi");
//    }
//
//    private boolean isAudio(String path) {
//        return path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".aac") || path.endsWith(".flac");
//    }
//
//    static class FileViewHolder extends RecyclerView.ViewHolder {
//        TextView fileName1, filePath;
//        ImageView thumbnail;
//
//        public FileViewHolder(View itemView) {
//            super(itemView);
//            fileName1 = itemView.findViewById(R.id.fileName1);
//            filePath = itemView.findViewById(R.id.filePath2);
//            thumbnail = itemView.findViewById(R.id.imageView1);
//        }
//    }
//}
