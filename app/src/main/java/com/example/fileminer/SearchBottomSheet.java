package com.example.fileminer;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SearchBottomSheet extends BottomSheetDialogFragment {

    private Context context;
    private String selectedSearchType; // Will be passed from MainActivity
    private boolean isCaseSensitive; // To track case sensitivity
    private OnSearchOptionSelectedListener searchOptionListener;

    // Interface for communicating search type selection
    public interface OnSearchOptionSelectedListener {
        void onSearchOptionSelected(String searchType, boolean isCaseSensitive);
    }

    // Constructor
    public SearchBottomSheet(Context context, String selectedSearchType, boolean isCaseSensitive, OnSearchOptionSelectedListener listener) {
        this.context = context;
        this.selectedSearchType = selectedSearchType; // Set the initial search type
        this.isCaseSensitive = isCaseSensitive; // Set the initial case sensitivity
        this.searchOptionListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.bottom_sheet_layout); // This layout should now only contain the RadioGroup for search type and case sensitivity

        RadioGroup searchTypeRadioGroup = dialog.findViewById(R.id.radioGroup);
        RadioGroup caseSensitiveRadioGroup = dialog.findViewById(R.id.radioGroupCase);

        // Set the initial checked state based on selectedSearchType
        setInitialRadioButtonState(searchTypeRadioGroup);
        setInitialCaseSensitiveState(caseSensitiveRadioGroup);

        // Handle radio button selection for search type
        searchTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newSelectedSearchType = "";

            if (checkedId == R.id.radioContains) {
                newSelectedSearchType = "Contains";
            } else if (checkedId == R.id.radioStartsWith) {
                newSelectedSearchType = "Starts With";
            } else if (checkedId == R.id.radioEndsWith) {
                newSelectedSearchType = "Ends With";
            }

            // Only update if the selection is different from the last one
            if (!newSelectedSearchType.equals(selectedSearchType)) {
                selectedSearchType = newSelectedSearchType;

                // Notify listener (MainActivity) when search type is selected
                if (searchOptionListener != null) {
                    searchOptionListener.onSearchOptionSelected(selectedSearchType, isCaseSensitive);
                }
            }
        });

        // Handle case sensitivity radio button selection
        caseSensitiveRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCaseSensitive) {
                isCaseSensitive = true;
            } else if (checkedId == R.id.radioCaseInsensitive) {
                isCaseSensitive = false;
            }

            // Notify listener (MainActivity) when case sensitivity is toggled
            if (searchOptionListener != null) {
                searchOptionListener.onSearchOptionSelected(selectedSearchType, isCaseSensitive);
            }
        });

        return dialog;
    }

    // Set initial radio button state based on selectedSearchType
    private void setInitialRadioButtonState(RadioGroup radioGroup) {
        if (selectedSearchType.equals("Starts With")) {
            radioGroup.check(R.id.radioStartsWith);
        } else if (selectedSearchType.equals("Ends With")) {
            radioGroup.check(R.id.radioEndsWith);
        }else {
            radioGroup.check(R.id.radioContains); // Default to "Contains"
        }
    }

    // Set initial state for case sensitivity radio buttons
    private void setInitialCaseSensitiveState(RadioGroup caseSensitiveGroup) {
        if (isCaseSensitive) {
            caseSensitiveGroup.check(R.id.radioCaseSensitive);
        } else {
            caseSensitiveGroup.check(R.id.radioCaseInsensitive);
        }
    }
}
