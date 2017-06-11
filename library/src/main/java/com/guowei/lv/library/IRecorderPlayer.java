
package com.guowei.lv.library;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;

/**
 * Plays audio notes.
 */
class IRecorderPlayer {

    /**
     * How often it updates the progress
     */
    private static final int PROGRESS_UPDATE_RATE = 50;

    enum State {

        IDLE,

        PREPARING,

        PLAYING,

        PAUSED,

        COMPLETED
    }

    private MediaPlayer player;

    private Handler handler;

    private boolean updateProgressBar = true;

    private State state;

    private String file;

    private IRecorderPlayerListener listener;

    IRecorderPlayer() {
        handler = new Handler();
    }

    private OnPreparedListener prepareListener = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            changeState(State.PLAYING);
        }
    };

    private OnCompletionListener completeListener = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            changeState(State.COMPLETED);
        }
    };

    private OnErrorListener errorListener = new OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            changeState(State.COMPLETED);
            if (listener != null) {
                listener.onPlaybackError(what, extra, null);
            }
            return true;
        }
    };

    private Runnable updateProgressRunnable = new Runnable() {

        @Override
        public void run() {
            State audioNoteState;

            if (player == null) {
                return;
            }

            if (player.isPlaying()) {
                audioNoteState = State.PLAYING;
            } else {
                audioNoteState = state;
            }
            updateProgress(audioNoteState);
            if (updateProgressBar) {
                handler.postDelayed(updateProgressRunnable, PROGRESS_UPDATE_RATE);
            }
        }
    };

    /**
     * Sets the file path to play.
     */
    void setAudioFile(String file) {
        this.file = file;
    }

    /**
     * Registers the listener.
     */
    void registerProgressListener(IRecorderPlayerListener listener) {
        this.listener = listener;
    }

    /**
     * Unregisters the listener.
     */
    void unregisterProgressListener() {
        this.listener = null;
    }

    /**
     * Plays / pauses the audio file.
     */
    void playOrPause() {

        if (state == State.PREPARING) {
            return;
        }

        if (state == State.PLAYING) {
            changeState(State.PAUSED);
        } else if (state == State.PAUSED) {
            changeState(State.PLAYING);
        } else {
            changeState(State.PREPARING);
        }
    }

    /**
     * Initializes the media player.
     */
    void init() {
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnPreparedListener(prepareListener);
            player.setOnCompletionListener(completeListener);
            player.setOnErrorListener(errorListener);
        }

    }

    /**
     * Releases resources.
     */
    void release() {
        if (player != null) {
            state = State.IDLE;
            file = null;
            player.release();
            player = null;
        }

    }

    /**
     * Updates the audio note widget.
     */
    private void updateAudioNoteState(State state, float ratio) {
        if (file != null) {
            if (listener != null) {
                listener.onPlaybackProgressUpdate(state, ratio);
            }
        }
    }

    /**
     * Changes the IRecorderManager state.
     *
     * @param s the state
     */
    private void changeState(State s) {
        switch (s) {
            case PREPARING:
                try {
                    player.reset();
                    player.setDataSource(file);
                    updateAudioNoteState(State.PREPARING, 0.0F);
                    player.prepareAsync();
                    state = State.PREPARING;
                } catch (Exception e) {
                    changeState(State.COMPLETED);
                    if (listener != null) {
                        listener.onPlaybackError(-1, -1, e);
                    }
                }

                break;
            case PLAYING:
                player.start();
                state = State.PLAYING;
                handler.post(updateProgressRunnable);
                updateAudioNoteState(State.PLAYING, 0.0F);
                break;
            case PAUSED:
                player.pause();
                state = State.PAUSED;
                break;
            case COMPLETED:
                state = State.COMPLETED;
                updateAudioNoteState(State.COMPLETED, 1.0F);
                break;
            default:
                break;
        }
    }

    /**
     * Updates the progress of the audio note widget.
     * <p>
     * And decides whether it should keep updating.
     */
    private void updateProgress(State state) {

        if (state == State.COMPLETED) {
            if (file != null) {
                updateAudioNoteState(state, 1.0F);
            }
            updateProgressBar = false;
            return;
        }

        float ratio = (float) player.getCurrentPosition() / player.getDuration();
        if (file != null) {
            updateAudioNoteState(state, ratio);
        }

        // detects whether should stop updating or not
        updateProgressBar = state != State.PAUSED;
    }

}
