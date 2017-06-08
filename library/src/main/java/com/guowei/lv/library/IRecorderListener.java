
package com.guowei.lv.library;

import android.net.Uri;

/**
 * Interface for recording and sending audio notes
 */
public interface IRecorderListener {

    void onSendAudioNote(Uri fileUri);


    void onCancelRecording();


    void onStartRecording();


    void onPressTooShort();


    void onRecordingError(Exception e);

    /**
     * The error codes are from MediaPlayer class.
     *
     * @param what  MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_SERVER_DIED
     * @param extra MEDIA_ERROR_IO MEDIA_ERROR_MALFORMED MEDIA_ERROR_UNSUPPORTED
     *              MEDIA_ERROR_TIMED_OUT
     * @param e     can be null. If not null, what and extra will be both -1.
     */
    void onPlayingError(int what, int extra, Exception e);
}
