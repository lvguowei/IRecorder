
package com.guowei.lv.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import static com.guowei.lv.library.IRecorderPlayer.State;

/**
 * Handles the audio note recording and playback.
 */
public class IRecorderManager implements IRecorderPlayerListener {

    /**
     * press delay to start recording
     */
    private static final int START_RECORDING_DELAY = 200;

    /**
     * The limit for recording audio
     */
    private static final long RECORDING_LIMIT = 60 * 1000;

    /**
     * How often do we update the progress
     */
    private static final int RECORDING_PROGRESS_UPDATE_RATE = 200;

    private Context context;

    private IRecorderLayout audioRecorderLayout;

    private FrameLayout audioRecorderSendButton;

    private FrameLayout audioRecorderCancelButton;

    private FrameLayout audioRecorderPlayButton;

    private FrameLayout audioRecorderPauseButton;

    private ProgressBar audioRecordingProgressBar;

    private TextView audioRecordingTimerTextView;

    private View audioRecordingStatusLayout;

    private Handler handler;

    private Vibrator vibrator;

    private Animation showAudioRecorderTrayAnim;

    private Animation cancelAnimation;

    private Animation sendAnimation;

    private Runnable recordButtonPressedRunnable;

    private Runnable recordingRunnable;

    private IRecorderListener listener;

    private long recordingStartTime;

    private Button recordButton;

    private RecState state;

    private MediaHandler mediaHandler;

    private IRecorder recorder;

    private IRecorderPlayer player;

    /**
     * The actions when the user lifts his finger
     */
    private enum Action {
        SEND,   // Send the audio
        CANCEL, // cancels and discards the recording
        STOP    // stops recording and wait for user's input
    }

    /**
     * The state of the audio recorder
     */
    private enum RecState {
        IDLE,       // Idle state
        INITIALIZE, // Initializing the recording, it may fail because of security issues
        RECORDING,  // Recording audio
        FINISHED,   // Recording finished
        CLOSED      // tray it closed
    }

    public IRecorderManager(Context context,
                            View baseLayout,
                            int recorderLayout,
                            int statusLayout,
                            int progressbar,
                            int recordTime,
                            Button recordButton,
                            MediaHandler mediaHandler,
                            IRecorder recorder,
                            IRecorderPlayer player,
                            IRecorderListener listener) {

        this.context = context;
        this.mediaHandler = mediaHandler;
        this.recorder = recorder;
        this.player = player;
        this.listener = listener;
        this.state = RecState.IDLE;

        this.recordButton = recordButton;

        initLayout(baseLayout, recorderLayout, statusLayout, progressbar, recordTime);

        handler = new Handler(Looper.getMainLooper());
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setupAnimation();

        recordButtonPressedRunnable = new Runnable() {

            @Override
            public void run() {
                startRecording();
            }
        };

        recordingRunnable = new Runnable() {

            @Override
            public void run() {

                long t = System.currentTimeMillis();
                long elapsed = t - recordingStartTime;
                int progress = (int) (((float) elapsed / RECORDING_LIMIT) * 100);

                audioRecordingProgressBar.setProgress(progress);
                audioRecordingTimerTextView.setText(formatElapsedRecordingTime((int) (elapsed / 1000)));

                if (state == RecState.RECORDING && progress < 100) {
                    handler.postDelayed(recordingRunnable, RECORDING_PROGRESS_UPDATE_RATE);
                } else if (progress >= 100) {
                    stopRecording();
                }
            }
        };

        player.registerProgressListener(this);
    }

    public void release() {

        // cancels recording if is recording
        cancelRecording();

        // release audio play back manager
        player.unregisterProgressListener();
        player.release();
    }

    public boolean onBackPressed() {
        player.release();
        if (audioRecorderLayout.getVisibility() == View.VISIBLE) {
            cancelRecording();
            audioRecorderLayout.startAnimation(cancelAnimation);
            return true;
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    private String formatElapsedRecordingTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Initializes the recording and playing layout components.
     */
    private void initLayout(View baseLayout, final int recorderLayout, int statusLayout, int progressbar, int recordTime) {

        audioRecorderLayout = (IRecorderLayout) baseLayout.findViewById(recorderLayout);
        audioRecordingStatusLayout = baseLayout.findViewById(statusLayout);

        audioRecorderSendButton = (FrameLayout) audioRecorderLayout.findViewById(R.id.send_button);
        audioRecorderCancelButton = (FrameLayout) audioRecorderLayout.findViewById(R.id.cancel_button);
        audioRecorderPlayButton = (FrameLayout) audioRecorderLayout.findViewById(R.id.play_button);
        audioRecorderPauseButton = (FrameLayout) audioRecorderLayout.findViewById(R.id.pause_button);

        audioRecordingProgressBar = (ProgressBar) audioRecordingStatusLayout
                .findViewById(progressbar);
        audioRecordingTimerTextView = (TextView) audioRecordingStatusLayout.findViewById(recordTime);

        recordButton.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        recordButton.setVisibility(View.GONE);
                        handleRecordButtonDown();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        handleRecordButtonMove(event);
                        break;
                    case MotionEvent.ACTION_UP:
                        handleRecordButtonUp();
                        break;
                    default:
                }
                return false;
            }
        });

        audioRecorderSendButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendAudioNote();
            }
        });

        audioRecorderCancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                recordButton.setVisibility(View.VISIBLE);
                cancelRecording();
            }
        });

        audioRecorderPlayButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                player.init();
                player.setAudioFile(getAudioNoteUri().getPath());
                player.playOrPause();
            }
        });

        audioRecorderPauseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                player.playOrPause();
            }
        });
    }

    private void handleRecordButtonDown() {
        state = RecState.IDLE;
        handler.postDelayed(recordButtonPressedRunnable, START_RECORDING_DELAY);
    }

    private void handleRecordButtonMove(MotionEvent event) {
        audioRecorderLayout.onFingerMovement(event.getX(), event.getY());
    }

    private void handleRecordButtonUp() {
        handler.removeCallbacks(recordButtonPressedRunnable);

        switch (state) {
            case RECORDING:
            case FINISHED:
                switch (getAction()) {
                    case SEND:
                        sendAudioNote();
                        break;
                    case CANCEL:
                        cancelAudio();
                        break;
                    case STOP:
                        stopRecording();
                }
                break;

            case IDLE:
                audioRecordingProgressBar.setProgress(0);
                recordButton.setVisibility(View.VISIBLE);
                if (listener != null) {
                    listener.onPressTooShort();
                }
        }
    }


    private void sendAudioNote() {
        recordButton.setVisibility(View.VISIBLE);
        sendRecording();
        audioRecorderLayout.startAnimation(sendAnimation);
    }

    private Uri getAudioNoteUri() {
        return recorder.getFileUri();
    }

    private void cancelAudio() {
        recordButton.setVisibility(View.VISIBLE);
        cancelRecording();
        audioRecorderLayout.startAnimation(cancelAnimation);
    }


    private void startRecording() {

        state = RecState.INITIALIZE;

        // obtain a new audio uri, if not possible then exit
        Uri uri = mediaHandler.getNewAudioUri();
        if (uri == null) {
            listener.onRecordingError(null);
            return;
        }

        try {

            vibrator.vibrate(50);
            audioRecorderLayout.setVisibility(View.VISIBLE);
            audioRecorderLayout.startAnimation(showAudioRecorderTrayAnim);

            audioRecorderLayout.record();
            audioRecordingStatusLayout.setVisibility(View.VISIBLE);

            recorder.init(uri);
            recorder.start();

            // switch to recording state
            state = RecState.RECORDING;
            recordingStartTime = System.currentTimeMillis();
            handler.post(recordingRunnable);

            if (listener != null) {
                listener.onStartRecording();
            }
        } catch (Exception e) {
            resetRecordingStatusLayout();
            resetRecorderLayout();

            if (listener != null) {
                listener.onRecordingError(e);
            }
        }
    }

    private void resetRecordingStatusLayout() {
        audioRecordingStatusLayout.setVisibility(View.GONE);
        audioRecordingProgressBar.setProgress(0);
    }

    private void resetRecorderLayout() {
        audioRecorderLayout.cleanup();
        audioRecorderLayout.setVisibility(View.GONE);
    }

    private void stopRecording() {
        boolean valid = recorder.stop();

        if (!valid && state == RecState.RECORDING) {
            resetRecorderLayout();

            // resets the status view
            resetRecordingStatusLayout();

            if (listener != null) {
                //listener.onPressTooShort();
                listener.onCancelRecording();
            }
            state = RecState.CLOSED;
        } else {
            audioRecorderLayout.stop();
            state = RecState.FINISHED;
        }
    }

    private void sendRecording() {
        if (state == RecState.RECORDING) {
            boolean valid = recorder.stop();
            if (valid) {
                if (listener != null) {
                    listener.onSendAudioNote(recorder.getFileUri());
                }
            } else {
                resetRecorderLayout();
                if (listener != null) {
                    listener.onPressTooShort();
                    listener.onCancelRecording();
                }

            }
        } else {
            if (listener != null) {
                listener.onSendAudioNote(recorder.getFileUri());
            }
        }

        resetRecordingStatusLayout();
        resetRecorderLayout();
        state = RecState.CLOSED;
    }

    private void cancelRecording() {
        player.release();
        if (state == RecState.RECORDING) {
            boolean valid = recorder.stop();
            if (!valid) {
                Toast.makeText(context, context.getString(R.string.irecorder_press_longer), Toast.LENGTH_SHORT).show();
            }
        }
        recorder.deleteAudioFile();
        resetRecorderLayout();
        resetRecordingStatusLayout();
        if (listener != null) {
            listener.onCancelRecording();
        }
        state = RecState.CLOSED;
    }

    private void setupAnimation() {

        // Animation for showing the audio recorder tray
        showAudioRecorderTrayAnim = AnimationUtils.loadAnimation(context, R.anim.show_audio_recorder_tray);

        // Animation for canceling the audio
        cancelAnimation = AnimationUtils.loadAnimation(context, R.anim.dismiss_audio_recorder_tray);

        // Animation for sending the audio
        sendAnimation = AnimationUtils.loadAnimation(context, R.anim.dismiss_audio_recorder_tray);
    }

    private Action getAction() {

        Action act = Action.STOP;
        if (audioRecorderLayout.isSendButtonActivated()) {
            act = Action.SEND;
        } else if (audioRecorderLayout.isCancelButtonActivated()) {
            act = Action.CANCEL;
        }
        return act;
    }

    @Override
    public void onPlaybackError(int what, int extra, Exception e) {
        if (listener != null) {
            listener.onPlayingError(what, extra, e);
        }
    }

    @Override
    public void onPlaybackProgressUpdate(State state, float ratio) {
        audioRecordingProgressBar.setProgress((int) (100 * ratio));
        switch (state) {
            case PLAYING:
                audioRecorderLayout.play();
                break;
            case PAUSED:
            case COMPLETED:
                audioRecorderLayout.pauseOrComplete();
                break;
            default:
        }

    }

}
