package com.example.fileminer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;

public class PermissionUtils {

    public static final int REQUEST_STORAGE_PERMISSION = 100;

   //     Checks if external storage access permission is granted.

    public static boolean checkStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

   //     Requests storage permissions (and notifications on Android 13+).

    @SuppressLint("InlinedApi")
    public static void requestStoragePermission(Activity activity) {
        WeakReference<Activity> weakActivity = new WeakReference<>(activity);
        Activity safeActivity = weakActivity.get();

        if (safeActivity == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ special permissions
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + safeActivity.getPackageName()));
                safeActivity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                safeActivity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION);
            }

        } else {
            // Android 8–10 (API 26–29) — runtime permissions
            ActivityCompat.requestPermissions(safeActivity,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    REQUEST_STORAGE_PERMISSION);
        }
    }
}
