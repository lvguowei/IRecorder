
package com.guowei.lv.library;

import android.media.MediaRecorder;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

/**
 * Recording audio files.
 */
class IRecorder {

    private MediaRecorder recorder;

    /**
     * the URI pointing to the recorded audio file
     */
    private Uri fileUri;

    /**
     * Initializes the recorder.
     * <p>
     * This should be called before start().
     */
    void init(Uri uri) {
        fileUri = uri;
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(fileUri.getPath());
    }

    /**
     * Clears up resource.
     * <p>
     * This should be called after stop().
     */
    private void clear() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    /**
     * Returns the URI of the recorded audio file.
     */
    Uri getFileUri() {
        return fileUri;
    }

    /**
     * Starts recording.
     */
    void start() throws IOException {
        if (fileUri != null) {
            recorder.prepare();
            recorder.start();
        }
    }

    /**
     * Stops recording.
     *
     * @return whether this recording is valid or not. If the recording time is
     * too short, and there is no real data recorded, it will return
     * false.
     */
    boolean stop() {
        boolean valid = true;
        try {
            recorder.stop();
        } catch (RuntimeException e) {
            // a RuntimeException is thrown ON PURPOSE to indicate
            // that there is no valid data recorded.
            valid = false;
        } finally {
            clear();
        }
        return valid;

    }

    /**
     * Deletes the audio file
     */
    void deleteAudioFile() {
        if (fileUri != null) {
            File file = new File(fileUri.getPath());
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
