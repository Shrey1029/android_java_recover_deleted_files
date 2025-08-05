// Updated: Caches fileType-specific folders and extensions separately
// Fixed memory leaks using static inner classes with WeakReference
// --- MODIFIED: Added FileName filter functionality
package com.example.fileminer;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchBottomSheet extends BottomSheetDialogFragment {

    private Context context;
    private String selectedSearchType;
    private boolean isCaseSensitive;
    private String fileType;
    private OnSearchOptionSelectedListener searchOptionListener;

    // --- NEW ---
    private String fileNameFilterType; // Holds state for the new filter

    private List<String> allFolders = new ArrayList<>();
    private List<String> excludedFolders = new ArrayList<>();

    private List<String> allExtensions = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>();

    private TextView excludeFoldersTextView;
    private TextView excludeExtensionsTextView;

    // --- MODIFIED INTERFACE ---
    public interface OnSearchOptionSelectedListener {
        void onSearchOptionSelected(String searchType, boolean isCaseSensitive,
                                    List<String> excludedFolders, List<String> excludedExtensions,
                                    String fileNameFilterType); // Added new parameter
    }

    // --- MODIFIED CONSTRUCTOR ---
    public SearchBottomSheet(Context context, String selectedSearchType, boolean isCaseSensitive,
                             List<String> excludedFolders, List<String> excludedExtensions,
                             String fileType, String fileNameFilterType, // Added new parameter
                             OnSearchOptionSelectedListener listener) {
        this.context = context;
        this.selectedSearchType = selectedSearchType;
        this.isCaseSensitive = isCaseSensitive;
        this.excludedFolders = new ArrayList<>(excludedFolders);
        this.excludedExtensions = new ArrayList<>(excludedExtensions);
        this.fileType = fileType;
        this.fileNameFilterType = fileNameFilterType; // Store state of new filter
        this.searchOptionListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_layout);

        RadioGroup searchTypeRadioGroup = dialog.findViewById(R.id.radioGroup);
        RadioGroup caseSensitiveRadioGroup = dialog.findViewById(R.id.radioGroupCase);
        excludeFoldersTextView = dialog.findViewById(R.id.multiExcludeFolders);
        excludeExtensionsTextView = dialog.findViewById(R.id.multiExcludeExtensions);

        // --- NEW ---
        // Find the new RadioGroup for filename filtering
        RadioGroup fileNameFilterGroup = dialog.findViewById(R.id.radioGroupFileNameFilter);


        excludeFoldersTextView.setEnabled(false);
        excludeExtensionsTextView.setEnabled(false);
        excludeFoldersTextView.setText("Loading folders...");
        excludeExtensionsTextView.setText("Loading extensions...");

        if (!SearchCache.folderMap.containsKey(fileType)) {
            new LoadFoldersTask(this, fileType).execute();
        } else {
            allFolders.clear();
            allFolders.addAll(SearchCache.folderMap.get(fileType));
            excludeFoldersTextView.setEnabled(true);
            updateExcludeTextView(excludeFoldersTextView);
        }

        if (!SearchCache.extensionMap.containsKey(fileType)) {
            new LoadExtensionsTask(this, fileType).execute();
        } else {
            allExtensions.clear();
            allExtensions.addAll(SearchCache.extensionMap.get(fileType));
            excludeExtensionsTextView.setEnabled(true);
            updateExcludeExtensionsTextView(excludeExtensionsTextView);
        }

        excludeFoldersTextView.setOnClickListener(v -> showFolderSelectionDialog());
        excludeExtensionsTextView.setOnClickListener(v -> showExtensionSelectionDialog());

        searchTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioContains) {
                selectedSearchType = "Contains";
            } else if (checkedId == R.id.radioStartsWith) {
                selectedSearchType = "Starts With";
            } else if (checkedId == R.id.radioEndsWith) {
                selectedSearchType = "Ends With";
            } else if (checkedId == R.id.radioPath) {
                selectedSearchType = "Path";
            }
            notifyListener();
        });

        caseSensitiveRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isCaseSensitive = (checkedId == R.id.radioCaseSensitive);
            notifyListener();
        });

        // --- NEW ---
        // Add listener for the new filename filter group
        fileNameFilterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioWithText) {
                fileNameFilterType = "With Text";
            } else if (checkedId == R.id.radioWithoutText) {
                fileNameFilterType = "Without Text";
            } else { // Assumes R.id.radioBoth
                fileNameFilterType = "Both";
            }
            notifyListener();
        });

        setInitialRadioButtonState(searchTypeRadioGroup);
        setInitialCaseSensitiveState(caseSensitiveRadioGroup);
        setInitialFileNameFilterState(fileNameFilterGroup); // Set the initial state for the new group

        return dialog;
    }

    // ... (LoadFoldersTask and LoadExtensionsTask are unchanged)
    private static class LoadFoldersTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<SearchBottomSheet> ref;
        private final String fileType;

        LoadFoldersTask(SearchBottomSheet sheet, String fileType) {
            this.ref = new WeakReference<>(sheet);
            this.fileType = fileType;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            Set<String> folderNames = new HashSet<>();
            SearchBottomSheet sheet = ref.get();
            if (sheet != null) {
                sheet.getFolderNamesRecursively(Environment.getExternalStorageDirectory(), folderNames);
            }
            return new ArrayList<>(folderNames);
        }

        @Override
        protected void onPostExecute(List<String> result) {
            SearchBottomSheet sheet = ref.get();
            if (sheet != null) {
                SearchCache.folderMap.put(fileType, result);
                sheet.allFolders.clear();
                sheet.allFolders.addAll(result);
                sheet.excludeFoldersTextView.setEnabled(true);
                sheet.updateExcludeTextView(sheet.excludeFoldersTextView);
            }
        }
    }

    private static class LoadExtensionsTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<SearchBottomSheet> ref;
        private final String fileType;

        LoadExtensionsTask(SearchBottomSheet sheet, String fileType) {
            this.ref = new WeakReference<>(sheet);
            this.fileType = fileType;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            Set<String> extensions = new HashSet<>();
            SearchBottomSheet sheet = ref.get();
            if (sheet != null) {
                sheet.getExtensionsRecursively(Environment.getExternalStorageDirectory(), extensions);
            }
            return new ArrayList<>(extensions);
        }

        @Override
        protected void onPostExecute(List<String> result) {
            SearchBottomSheet sheet = ref.get();
            if (sheet != null) {
                SearchCache.extensionMap.put(fileType, result);
                sheet.allExtensions.clear();
                sheet.allExtensions.addAll(result);
                sheet.excludeExtensionsTextView.setEnabled(true);
                sheet.updateExcludeExtensionsTextView(sheet.excludeExtensionsTextView);
            }
        }
    }

    // ... (Helper methods like isDeletedFile, getFolderNamesRecursively, etc., are unchanged)
    private boolean isDeletedFile(File file) {
        File parentDir = file.getParentFile();
        return parentDir != null && isTrashFolder(parentDir) || file.getName().startsWith(".trashed-");
    }

    private boolean isTrashFolder(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.startsWith(".trashed-") || name.startsWith(".trashed") ||
                name.equals(".recycle") || name.equals(".trash") || name.equals("_.trashed");
    }

    private boolean isHiddenFile(File file) {
        return file.getParentFile() != null && file.getParentFile().getName().startsWith(".");
    }

    private void getFolderNamesRecursively(File dir, Set<String> folderNames) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        boolean hasMatchingFile = false;

        for (File file : files) {
            if (file.isDirectory()) {
                getFolderNamesRecursively(file, folderNames);
            } else {
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex != -1) {
                    String ext = name.substring(dotIndex).toLowerCase(Locale.ROOT);
                    boolean match = isExtensionMatchingType(ext);
                    if ((fileType.equals("Deleted") && isDeletedFile(file)) ||
                            (fileType.equals("Hidden") && isHiddenFile(file)) ||
                            (!fileType.equals("Deleted") && !fileType.equals("Hidden") && match)) {
                        hasMatchingFile = true;
                    }
                }
            }
        }

        if (hasMatchingFile) {
            folderNames.add(dir.getName());
        }
    }

    private void getExtensionsRecursively(File dir, Set<String> extensions) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                getExtensionsRecursively(file, extensions);
            } else {
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < name.length() - 1) {
                    String ext = name.substring(dotIndex).toLowerCase(Locale.ROOT);
                    if ((fileType.equals("Deleted") && isDeletedFile(file)) ||
                            (fileType.equals("Hidden") && isHiddenFile(file)) ||
                            (!fileType.equals("Deleted") && !fileType.equals("Hidden") && isExtensionMatchingType(ext))) {
                        extensions.add(ext);
                    }
                }
            }
        }
    }

    private boolean isExtensionMatchingType(String ext) {
        if (fileType == null || fileType.equals("All")) return true;
        switch (fileType) {
            case "Photo":
                return ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp|heic)");
            case "Video":
                return ext.matches("\\.(mp4|mkv|avi|3gp|webm|mov)");
            case "Audio":
                return ext.matches("\\.(mp3|wav|m4a|aac|ogg|flac|opus)");
            case "Document":
                return ext.matches("\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt)");
            default:
                return true;
        }
    }
    private void showFolderSelectionDialog() {
        boolean[] checkedItems = new boolean[allFolders.size()];
        List<String> tempExcluded = new ArrayList<>(excludedFolders); // Temporary copy

        for (int i = 0; i < allFolders.size(); i++) {
            checkedItems[i] = excludedFolders.contains(allFolders.get(i));
        }

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Select Folders to Exclude")
                .setMultiChoiceItems(allFolders.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                    String folder = allFolders.get(which);
                    if (isChecked && !tempExcluded.contains(folder)) {
                        tempExcluded.add(folder);
                    } else if (!isChecked) {
                        tempExcluded.remove(folder);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    excludedFolders.clear();
                    excludedFolders.addAll(tempExcluded);
                    updateExcludeTextView(excludeFoldersTextView);
                    notifyListener();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showExtensionSelectionDialog() {
        boolean[] checkedItems = new boolean[allExtensions.size()];
        List<String> tempExcluded = new ArrayList<>(excludedExtensions);

        for (int i = 0; i < allExtensions.size(); i++) {
            checkedItems[i] = excludedExtensions.contains(allExtensions.get(i));
        }

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Select Extensions to Exclude")
                .setMultiChoiceItems(allExtensions.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                    String ext = allExtensions.get(which);
                    if (isChecked && !tempExcluded.contains(ext)) {
                        tempExcluded.add(ext);
                    } else if (!isChecked) {
                        tempExcluded.remove(ext);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    excludedExtensions.clear();
                    excludedExtensions.addAll(tempExcluded);
                    updateExcludeExtensionsTextView(excludeExtensionsTextView);
                    notifyListener();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void updateExcludeTextView(TextView view) {
        view.setText(excludedFolders.isEmpty() ? "Select folders to exclude" : TextUtils.join(", ", excludedFolders));
    }

    private void updateExcludeExtensionsTextView(TextView view) {
        view.setText(excludedExtensions.isEmpty() ? "Select file extensions to exclude" : TextUtils.join(", ", excludedExtensions));
    }

    // --- MODIFIED NOTIFY LISTENER ---
    private void notifyListener() {
        if (searchOptionListener != null) {
            searchOptionListener.onSearchOptionSelected(selectedSearchType, isCaseSensitive, excludedFolders, excludedExtensions, fileNameFilterType);
        }
    }

    private void setInitialRadioButtonState(RadioGroup radioGroup) {
        switch (selectedSearchType) {
            case "Starts With":
                radioGroup.check(R.id.radioStartsWith);
                break;
            case "Ends With":
                radioGroup.check(R.id.radioEndsWith);
                break;
            case "Path":
                radioGroup.check(R.id.radioPath);
                break;
            default:
                radioGroup.check(R.id.radioContains);
        }
    }

    private void setInitialCaseSensitiveState(RadioGroup caseSensitiveGroup) {
        caseSensitiveGroup.check(isCaseSensitive ? R.id.radioCaseSensitive : R.id.radioCaseInsensitive);
    }

    // --- NEW ---
    // Sets the initial state for the filename filter radio buttons
    private void setInitialFileNameFilterState(RadioGroup radioGroup) {
        if ("With Text".equals(fileNameFilterType)) {
            radioGroup.check(R.id.radioWithText);
        } else if ("Without Text".equals(fileNameFilterType)) {
            radioGroup.check(R.id.radioWithoutText);
        } else {
            radioGroup.check(R.id.radioBoth);
        }
    }
}