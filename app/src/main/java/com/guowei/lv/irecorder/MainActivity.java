package com.guowei.lv.irecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.guowei.lv.library.IRecorderLayout;
import com.guowei.lv.library.IRecorderListener;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 1;
    private IRecorderLayout recorderLayout;

    @Override
    protected void onPause() {
        super.onPause();
        recorderLayout.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recorderLayout = (IRecorderLayout) findViewById(R.id.recorder_layout);
        recorderLayout.setListener(new IRecorderListener() {
            @Override
            public void onSendAudioNote(Uri fileUri) {

            }

            @Override
            public void onCancelRecording() {

            }

            @Override
            public void onStartRecording() {

            }

            @Override
            public void onPressTooShort() {
                Toast.makeText(MainActivity.this, "Press longer", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRecordingError(Exception e) {

            }

            @Override
            public void onPlayingError(int what, int extra, Exception e) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!recorderLayout.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


}
