// StorageViewModel.java
package com.example.fileminer;

import androidx.lifecycle.ViewModel;

// ViewModel to store progress state across configuration changes
public class StorageViewModel extends ViewModel {
    public int lastProgress = 0;
}
