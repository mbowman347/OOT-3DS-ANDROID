package org.triaevum.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

/**
 * First-launch prompt for All Files Access (Android 11+) and legacy
 * WRITE_EXTERNAL_STORAGE (Android 10). MANAGE_EXTERNAL_STORAGE cannot use
 * the normal runtime-permission dialog; the system Settings screen is required.
 */
final class StorageAccessHelper {
    static final int REQUEST_LEGACY_STORAGE = 4101;
    static final int REQUEST_MANAGE_STORAGE = 4102;

    private static final String PREFS = "org.triaevum.android_preferences";
    private static final String KEY_PROMPTED = "storage_all_files_prompted";

    private StorageAccessHelper() {}

    static boolean hasAllFilesAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;
    }

    static boolean shouldPromptOnFirstLaunch(Context context) {
        if (hasAllFilesAccess(context)) {
            return false;
        }
        return !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROMPTED, false);
    }

    static void markPrompted(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROMPTED, true)
            .apply();
    }

    static void showFirstLaunchPrompt(Activity activity, Runnable onSettingsOpened, Runnable onSkip) {
        new AlertDialog.Builder(activity)
            .setTitle("All files access")
            .setMessage("TriAevum needs permission to manage external storage "
                + "and access all files. On the next screen, turn on access "
                + "for this app.")
            .setCancelable(false)
            .setPositiveButton("Allow", (dialog, which) -> {
                markPrompted(activity);
                requestAllFilesAccess(activity);
                onSettingsOpened.run();
            })
            .setNegativeButton("Not now", (dialog, which) -> {
                markPrompted(activity);
                onSkip.run();
            })
            .show();
    }

    static void requestAllFilesAccess(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            try {
                activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
            } catch (Exception ignored) {
                Intent fallback = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(fallback, REQUEST_MANAGE_STORAGE);
            }
            return;
        }
        activity.requestPermissions(
            new String[] {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            },
            REQUEST_LEGACY_STORAGE);
    }
}
