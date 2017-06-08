package com.guowei.lv.irecorder;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.guowei.lv.library.IRecorder;
import com.guowei.lv.library.IRecorderListener;
import com.guowei.lv.library.IRecorderManager;
import com.guowei.lv.library.IRecorderPlayer;
import com.guowei.lv.library.MediaHandler;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button recordButton = (Button) findViewById(R.id.record_button);
        final IRecorderManager recorderManager = new IRecorderManager(this,
                findViewById(R.id.main_layout),
                R.id.recorder_layout,
                R.id.status_layout,
                R.id.audio_recorder_progressbar,
                R.id.audio_recorder_time,
                recordButton,
                new MediaHandler(this),
                new IRecorder(),
                new IRecorderPlayer(),
                new IRecorderListener() {
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

                    }

                    @Override
                    public void onRecordingError(Exception e) {

                    }

                    @Override
                    public void onPlayingError(int what, int extra, Exception e) {

                    }
                });


    }
}
