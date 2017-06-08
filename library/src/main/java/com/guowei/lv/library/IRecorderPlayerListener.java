
package com.guowei.lv.library;

interface IRecorderPlayerListener {

    void onPlaybackProgressUpdate(IRecorderPlayer.State state, float ratio);

    /**
     * The error codes are from MediaPlayer class.
     *
     * @param what  MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_SERVER_DIED
     * @param extra MEDIA_ERROR_IO MEDIA_ERROR_MALFORMED MEDIA_ERROR_UNSUPPORTED
     *              MEDIA_ERROR_TIMED_OUT
     * @param e     can be null. If not null, what and extra will be both -1.
     */
    void onPlaybackError(int what, int extra, Exception e);
}
