
package com.guowei.lv.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class MediaHandler {

    private final static String TAG = "app_MediaHandler";

    private final static String FOLDER_AUDIOS = "Audios";

    private String appName;

    public MediaHandler(Context context) {
        this.appName = context.getString(R.string.irecorder_app_name);
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getAudiosFolder() {
        if (!isExternalStorageWritable()) {
            return null;
        }
        File root = Environment.getExternalStorageDirectory();
        File app = new File(root, appName);
        File vid = new File(app, appName + "_" + FOLDER_AUDIOS);

        if (vid.isDirectory() || vid.mkdirs()) {
            return vid;
        }
        return null;
    }

    Uri getNewAudioUri() {
        String fileName = appName + "-" + getFileName(System.currentTimeMillis());

        Uri uri = null;
        try {

            File file = getAudiosFolder();
            if (file == null) {
                return null;
            }
            // create file and see if it is really created
            file = File.createTempFile(fileName, ".mp4", file);
            if (file.exists() && file.isFile()) {
                uri = Uri.fromFile(file);
            }
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "getNewAudioUri: " + e.toString());
        }
        return uri;
    }

    private String getFileName(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat d = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return d.format(c.getTime());
    }
}
