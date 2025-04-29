package com.example.fileminer;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SearchBottomSheet extends BottomSheetDialogFragment {

    private Context context;
    private String selectedSearchType; // Will be passed from MainActivity
    private boolean isCaseSensitive; // To track case sensitivity
    private OnSearchOptionSelectedListener searchOptionListener;

    public interface OnSearchOptionSelectedListener {
        void onSearchOptionSelected(String searchType, boolean isCaseSensitive);
    }

    public SearchBottomSheet(Context context, String selectedSearchType, boolean isCaseSensitive, OnSearchOptionSelectedListener listener) {
        this.context = context;
        this.selectedSearchType = selectedSearchType;
        this.isCaseSensitive = isCaseSensitive;
        this.searchOptionListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_layout);

        RadioGroup searchTypeRadioGroup = dialog.findViewById(R.id.radioGroup);
        RadioGroup caseSensitiveRadioGroup = dialog.findViewById(R.id.radioGroupCase);

        setInitialRadioButtonState(searchTypeRadioGroup);
        setInitialCaseSensitiveState(caseSensitiveRadioGroup);

        searchTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newSelectedSearchType = "";

            if (checkedId == R.id.radioContains) {
                newSelectedSearchType = "Contains";
            } else if (checkedId == R.id.radioStartsWith) {
                newSelectedSearchType = "Starts With";
            } else if (checkedId == R.id.radioEndsWith) {
                newSelectedSearchType = "Ends With";
            } else if (checkedId == R.id.radioPath) { // <<<< NEW OPTION
                newSelectedSearchType = "Path";
            }

            if (!newSelectedSearchType.equals(selectedSearchType)) {
                selectedSearchType = newSelectedSearchType;

                if (searchOptionListener != null) {
                    searchOptionListener.onSearchOptionSelected(selectedSearchType, isCaseSensitive);
                }
            }
        });

        caseSensitiveRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCaseSensitive) {
                isCaseSensitive = true;
            } else if (checkedId == R.id.radioCaseInsensitive) {
                isCaseSensitive = false;
            }

            if (searchOptionListener != null) {
                searchOptionListener.onSearchOptionSelected(selectedSearchType, isCaseSensitive);
            }
        });

        return dialog;
    }

    private void setInitialRadioButtonState(RadioGroup radioGroup) {
        if (selectedSearchType.equals("Starts With")) {
            radioGroup.check(R.id.radioStartsWith);
        } else if (selectedSearchType.equals("Ends With")) {
            radioGroup.check(R.id.radioEndsWith);
        } else if (selectedSearchType.equals("Path")) {
            radioGroup.check(R.id.radioPath); // <<<< NEW
        } else {
            radioGroup.check(R.id.radioContains); // Default
        }
    }

    private void setInitialCaseSensitiveState(RadioGroup caseSensitiveGroup) {
        if (isCaseSensitive) {
            caseSensitiveGroup.check(R.id.radioCaseSensitive);
        } else {
            caseSensitiveGroup.check(R.id.radioCaseInsensitive);
        }
    }
}
