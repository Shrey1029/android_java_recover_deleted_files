//package com.example.fileminer;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.provider.Settings;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.Log;
//import android.view.View;
//import android.webkit.MimeTypeMap;
//import android.widget.CheckBox;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.content.FileProvider;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class Manageduplicate extends Activity {
//    private static final int PERMISSION_REQUEST_CODE = 100;
//    private boolean isDataLoaded = false;
//
//    RecyclerView recyclerView;
//    DuplicateAdapter adapter;
//    List<File> allFiles = new ArrayList<>();
//    List<File> duplicateFiles = new ArrayList<>();
//    CheckBox showPathCheckbox;
//    EditText searchPath;
//    ProgressBar progressBar;
//    TextView loadingText;
//    LinearLayout loadingLayout;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_manageduplicate);
//
//        recyclerView = findViewById(R.id.recyclerView);
//        showPathCheckbox = findViewById(R.id.showPathCheckbox);
//        searchPath = findViewById(R.id.searchPath);
//        progressBar = findViewById(R.id.progressBar);
//        loadingText = findViewById(R.id.loadingText);
//        loadingLayout = findViewById(R.id.loadingLayout);
//
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//
//        checkPermissionsAndLoad();
//
//        findViewById(R.id.hideDuplicates).setOnClickListener(v -> showUniqueFiles());
//        findViewById(R.id.showDuplicates).setOnClickListener(v -> showDuplicateFiles());
//
//        showPathCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (adapter != null) {
//                adapter.setShowPaths(isChecked);
//                adapter.notifyDataSetChanged();
//            }
//        });
//
//        searchPath.addTextChangedListener(new TextWatcher() {
//            public void afterTextChanged(Editable s) {
//                filterByPath(s.toString());
//            }
//
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//            public void onTextChanged(CharSequence s, int start, int before, int count) {}
//        });
//    }
//
//    private void checkPermissionsAndLoad() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (!Environment.isExternalStorageManager()) {
//                requestAllFilesAccess();
//            } else {
//                loadFilesAsync();
//            }
//        } else {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                        PERMISSION_REQUEST_CODE);
//            } else {
//                loadFilesAsync();
//            }
//        }
//    }
//
//    private void requestAllFilesAccess() {
//        Toast.makeText(this, "Please grant 'All Files Access'", Toast.LENGTH_LONG).show();
//        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//        intent.setData(Uri.parse("package:" + getPackageName()));
//        startActivity(intent);
//    }
//
//    private void loadFilesAsync() {
//        loadingLayout.setVisibility(View.VISIBLE);
//        recyclerView.setVisibility(View.GONE);
//
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... voids) {
//                File directory = Environment.getExternalStorageDirectory();
//                allFiles = getAllFiles(directory);
//                duplicateFiles = findDuplicateFiles(allFiles);
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void unused) {
//                adapter = new DuplicateAdapter(Manageduplicate.this, allFiles, showPathCheckbox.isChecked());
//                recyclerView.setAdapter(adapter);
//                adapter.setOnFileClickListener(Manageduplicate.this::openFile);
//
//                loadingLayout.setVisibility(View.GONE);
//                recyclerView.setVisibility(View.VISIBLE);
//                isDataLoaded = true;
//            }
//        }.execute();
//    }
//
//    private List<File> getAllFiles(File dir) {
//        List<File> fileList = new ArrayList<>();
//        File[] files = dir.listFiles();
//        if (files != null) {
//            for (File f : files) {
//                if (f.isFile()) {
//                    fileList.add(f);
//                } else if (f.isDirectory()) {
//                    fileList.addAll(getAllFiles(f));
//                }
//            }
//        }
//        return fileList;
//    }
//
//    private List<File> findDuplicateFiles(List<File> files) {
//        Map<String, List<File>> map = new HashMap<>();
//        for (File file : files) {
//            String key = file.getName() + "_" + file.length();
//            map.putIfAbsent(key, new ArrayList<>());
//            map.get(key).add(file);
//        }
//
//        List<File> duplicates = new ArrayList<>();
//        for (List<File> list : map.values()) {
//            if (list.size() > 1) {
//                duplicates.addAll(list);
//            }
//        }
//        return duplicates;
//    }
//
//    private void showDuplicateFiles() {
//        if (adapter != null) {
//            adapter.updateList(duplicateFiles);
//        }
//    }
//
//    private void showUniqueFiles() {
//        new AsyncTask<Void, Void, List<File>>() {
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                loadingLayout.setVisibility(View.VISIBLE); // show progress
//            }
//
//            @Override
//            protected List<File> doInBackground(Void... voids) {
//                Map<String, List<File>> map = new HashMap<>();
//                for (File file : allFiles) {
//                    try {
//                        String key = file.getName() + "_" + file.length();
//                        if (!map.containsKey(key)) {
//                            map.put(key, new ArrayList<>());
//                        }
//                        map.get(key).add(file);
//                    } catch (Exception e) {
//                        Log.e("showUniqueFiles", "Error: " + e.getMessage());
//                    }
//                }
//
//                List<File> uniqueFiles = new ArrayList<>();
//                for (List<File> list : map.values()) {
//                    if (list.size() == 1) {
//                        uniqueFiles.addAll(list);
//                    }
//                }
//
//                return uniqueFiles;
//            }
//
//            @Override
//            protected void onPostExecute(List<File> uniqueFiles) {
//                super.onPostExecute(uniqueFiles);
//                loadingLayout.setVisibility(View.GONE); // hide progress
//                Log.d("showUniqueFiles", "Unique files: " + uniqueFiles.size());
//                adapter.updateList(uniqueFiles);
//            }
//        }.execute();
//    }
//
//
//
//    private void filterByPath(String query) {
//        if (adapter != null) {
//            List<File> filtered = new ArrayList<>();
//            for (File file : allFiles) {
//                if (file.getAbsolutePath().toLowerCase().contains(query.toLowerCase())) {
//                    filtered.add(file);
//                }
//            }
//            adapter.updateList(filtered);
//        }
//    }
//
//    private void openFile(File file) {
//        try {
//            Uri fileUri = FileProvider.getUriForFile(this,
//                    getPackageName() + ".provider", file);
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(fileUri, getMimeType(file.getAbsolutePath()));
//            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            startActivity(intent);
//        } catch (Exception e) {
//            Toast.makeText(this, "Can't open file", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private String getMimeType(String path) {
//        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
//        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (!isDataLoaded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
//            loadFilesAsync();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == PERMISSION_REQUEST_CODE &&
//                grantResults.length > 0 &&
//                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            loadFilesAsync();
//        } else {
//            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//        }
//    }
//}
