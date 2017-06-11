
package com.guowei.lv.library;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Layout for audio recording.
 */
public class IRecorderLayout extends RelativeLayout implements IRecorderPlayerListener {

    /**
     * Need to expand the activation boundary after the tray expands (dp)
     */
    private static final int ACTIVATION_POINT_ADDON = 10;

    /**
     * The ratio of the enlargement of the tray
     */
    private final static float TRAY_SCALE_RATIO = 1.1f;

    /**
     * How far the buttons move during animation (dp)
     */
    private final static float BUTTONS_MOVE_DISTANCE = 10f;

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

    /**
     * Offset for activate the buttons (dp)
     */
    private int activationPointOffset = 10;

    private final static int EXPAND_ANIMATION_DURATION = 300;

    private final static int SHRINK_ANIMATION_DURATION = 400;

    private Handler handler;

    private View recorderButton;

    private View recorderTray;

    private View recorderStatusLayout;

    private View recordButton;

    private View playButton;

    private View pauseButton;

    private View cancelButton;

    private View sendButton;

    private View cancelButtonBg;

    private View sendButtonBg;

    private View audioRecorderBgView;

    private TextView audioRecordingTimerTextView;

    private ProgressBar audioRecordingProgressBar;

    private RecState state;

    private MediaHandler mediaHandler;

    private Vibrator vibrator;

    private Runnable recordButtonPressedRunnable;

    private AnimationType animationType = AnimationType.SHRINK;

    private boolean duringAnimation;

    private Animation showAudioRecorderTrayAnim;

    private Animation cancelAnimation;

    private Animation sendAnimation;

    private IRecorder recorder;

    private IRecorderListener listener;

    private long recordingStartTime;

    private Runnable recordingRunnable;

    private IRecorderPlayer player;


    private AnimatorListener animListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
            duringAnimation = true;

        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            duringAnimation = false;
            activationPointOffset += ACTIVATION_POINT_ADDON;

        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    private AnimatorListener reverseAnimListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
            duringAnimation = true;

        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            duringAnimation = false;
            activationPointOffset -= ACTIVATION_POINT_ADDON;

        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    public IRecorderLayout(Context context) {
        super(context);
        init();
    }

    public IRecorderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setListener(IRecorderListener listener) {
        this.listener = listener;
    }

    public void record() {
        recordButton.setVisibility(View.VISIBLE);
        playButton.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
    }

    public void stop() {
        recordButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
    }

    public void play() {
        recordButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(View.VISIBLE);
    }

    public void pauseOrComplete() {
        recordButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
    }

    public void onFingerMovement(float dx, float dy) {

        if (duringAnimation) {
            return;
        }

        // Gets the absolute position of the record button
        int[] recLoc = new int[2];
        recordButton.getLocationOnScreen(recLoc);

        // Gets the absolute position of the cancel button
        int[] cancelLoc = new int[2];
        int[] sendLoc = new int[2];
        cancelButton.getLocationOnScreen(cancelLoc);
        sendButton.getLocationOnScreen(sendLoc);

        // Activates the send or cancel button
        int cancelR = cancelButtonBg.getWidth() / 2;
        int sendR = sendButtonBg.getWidth() / 2;

        int cancelFingerDistance = (int) Math.sqrt(Math.pow((cancelLoc[0] + cancelR - (recLoc[0] + dx)), 2)
                + Math.pow((cancelLoc[1] + cancelR - (recLoc[1] + dy)), 2));
        int sendFingerDistance = (int) Math.sqrt(Math.pow((sendLoc[0] + sendR - (recLoc[0] + dx)), 2)
                + Math.pow((sendLoc[1] + sendR - (recLoc[1] + dy)), 2));

        if (cancelFingerDistance <= cancelR + convertDpToPixel(getContext(), activationPointOffset)) {
            if (animationType != AnimationType.EXPAND) {
                animateRecorderLayout();
            }
            animationType = AnimationType.EXPAND;
            cancelButtonBg.setVisibility(View.VISIBLE);

        } else if (sendFingerDistance <= sendR + convertDpToPixel(getContext(), activationPointOffset)) {
            if (animationType != AnimationType.EXPAND) {
                animateRecorderLayout();
            }
            animationType = AnimationType.EXPAND;
            sendButtonBg.setVisibility(View.VISIBLE);
        } else {
            if (animationType != AnimationType.SHRINK) {
                reverseAnimateRecorderLayout();
                animationType = AnimationType.SHRINK;
            }
            animationType = AnimationType.SHRINK;
            cancelButtonBg.setVisibility(View.INVISIBLE);
            sendButtonBg.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Clean up the resources and reset the views.
     */
    public void cleanup() {
        // resets the views
        sendButtonBg.setVisibility(View.INVISIBLE);
        cancelButtonBg.setVisibility(View.INVISIBLE);

        // resets the scale and translate animations
        audioRecorderBgView.animate().scaleX(1f).scaleY(1f);
        sendButton.animate().translationY(0f);
        cancelButton.animate().translationX(0f);

        playButton.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
        recordButton.setVisibility(View.VISIBLE);
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.irecorder_layout, this, true);
        recorderButton = findViewById(R.id.recorder_button);
        recorderTray = findViewById(R.id.recorder_tray);
        recorderStatusLayout = findViewById(R.id.recorder_status_layout);
        recordButton = findViewById(R.id.record_button);
        playButton = findViewById(R.id.play_button);
        pauseButton = findViewById(R.id.pause_button);
        cancelButton = findViewById(R.id.cancel_button);
        sendButton = findViewById(R.id.send_button);
        cancelButtonBg = findViewById(R.id.cancel_button_bg);
        sendButtonBg = findViewById(R.id.send_button_bg);
        audioRecorderBgView = findViewById(R.id.audio_recorder_bg_view);
        audioRecordingProgressBar = (ProgressBar) findViewById(R.id.recorder_progressbar);
        audioRecordingTimerTextView = (TextView) findViewById(R.id.recorder_time);

        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        recorder = new IRecorder();
        player = new IRecorderPlayer();
        mediaHandler = new MediaHandler(getContext());

        sendButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendAudioNote();
            }
        });

        cancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                recorderButton.setVisibility(View.VISIBLE);
                cancelRecording();
            }
        });

        playButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                player.init();
                player.setAudioFile(getAudioNoteUri().getPath());
                player.playOrPause();
            }
        });

        pauseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                player.playOrPause();
            }
        });

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

        recordButtonPressedRunnable = new Runnable() {

            @Override
            public void run() {
                startRecording();
            }
        };

        recorderButton.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                Log.d("test", "onTouch: " + action);
                switch (action) {

                    case MotionEvent.ACTION_DOWN:
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
                return true;
            }
        });

        handler = new Handler(Looper.getMainLooper());
        setupAnimation();

        player.registerProgressListener(this);

    }

    private Uri getAudioNoteUri() {
        return recorder.getFileUri();
    }

    private void setupAnimation() {

        // Animation for showing the audio recorder tray
        showAudioRecorderTrayAnim = AnimationUtils.loadAnimation(getContext(), R.anim.show_audio_recorder_tray);

        // Animation for canceling the audio
        cancelAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.dismiss_audio_recorder_tray);

        // Animation for sending the audio
        sendAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.dismiss_audio_recorder_tray);
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

            recorderTray.setVisibility(View.VISIBLE);
            recorderStatusLayout.setVisibility(View.VISIBLE);

            startAnimation(showAudioRecorderTrayAnim);
            record();
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
        recorderStatusLayout.setVisibility(View.GONE);
        audioRecordingProgressBar.setProgress(0);
    }

    private void resetRecorderLayout() {
        cleanup();
        recorderTray.setVisibility(View.GONE);
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
            stop();
            state = RecState.FINISHED;
        }
    }

    private void handleRecordButtonDown() {
        state = RecState.IDLE;
        handler.postDelayed(recordButtonPressedRunnable, START_RECORDING_DELAY);
    }

    private void handleRecordButtonMove(MotionEvent event) {
        onFingerMovement(event.getX(), event.getY());
    }

    private Action getAction() {
        Action act = Action.STOP;
        if (isSendButtonActivated()) {
            act = Action.SEND;
        } else if (isCancelButtonActivated()) {
            act = Action.CANCEL;
        }
        return act;
    }

    private void sendAudioNote() {
        recorderButton.setVisibility(View.VISIBLE);
        sendRecording();
        recorderTray.startAnimation(sendAnimation);
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
                recorderButton.setVisibility(View.VISIBLE);
                if (listener != null) {
                    listener.onPressTooShort();
                }
        }
    }

    private void cancelAudio() {
        recorderButton.setVisibility(View.VISIBLE);
        cancelRecording();
        recorderTray.startAnimation(cancelAnimation);
    }

    private void cancelRecording() {
        player.release();
        if (state == RecState.RECORDING) {
            boolean valid = recorder.stop();
            if (!valid) {
                Toast.makeText(getContext(), getContext().getString(R.string.irecorder_press_longer), Toast.LENGTH_SHORT).show();
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

    private void animateRecorderLayout() {

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(audioRecorderBgView, "scaleX", TRAY_SCALE_RATIO);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(audioRecorderBgView, "scaleY", TRAY_SCALE_RATIO);

        ObjectAnimator animUp = ObjectAnimator.ofFloat(sendButton, "translationY", sendButton.getTranslationY(),
                -convertDpToPixel(getContext(), BUTTONS_MOVE_DISTANCE));
        ObjectAnimator animLeft = ObjectAnimator.ofFloat(cancelButton, "translationX", cancelButton.getTranslationX(),
                -convertDpToPixel(getContext(), BUTTONS_MOVE_DISTANCE));

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(animUp).with(animLeft).with(scaleX).with(scaleY);
        animSet.setDuration(EXPAND_ANIMATION_DURATION);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animSet.addListener(animListener);
        animSet.start();
    }

    private void reverseAnimateRecorderLayout() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(audioRecorderBgView, "scaleX", 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(audioRecorderBgView, "scaleY", 1f);

        ObjectAnimator animUp = ObjectAnimator.ofFloat(sendButton, "translationY", sendButton.getTranslationY(), 0f);
        ObjectAnimator animLeft = ObjectAnimator.ofFloat(cancelButton, "translationX", cancelButton.getTranslationX(),
                0f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(animUp).with(animLeft).with(scaleX).with(scaleY);
        animSet.setDuration(SHRINK_ANIMATION_DURATION);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animSet.addListener(reverseAnimListener);
        animSet.start();
    }

    public boolean isSendButtonActivated() {
        return sendButtonBg.getVisibility() == View.VISIBLE;
    }

    public boolean isCancelButtonActivated() {
        return cancelButtonBg.getVisibility() == View.VISIBLE;
    }

    private static float convertDpToPixel(Context context, float dp) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    @SuppressLint("DefaultLocale")
    private String formatElapsedRecordingTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
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
        if (recorderTray.getVisibility() == View.VISIBLE) {
            cancelRecording();
            recorderTray.startAnimation(cancelAnimation);
            return true;
        }
        return false;
    }

    @Override
    public void onPlaybackError(int what, int extra, Exception e) {
        if (listener != null) {
            listener.onPlayingError(what, extra, e);
        }
    }

    @Override
    public void onPlaybackProgressUpdate(IRecorderPlayer.State state, float ratio) {
        audioRecordingProgressBar.setProgress((int) (100 * ratio));
        switch (state) {
            case PLAYING:
                play();
                break;
            case PAUSED:
            case COMPLETED:
                pauseOrComplete();
                break;
            default:
        }

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

    private enum AnimationType {

        /**
         * expands the tray out
         */
        EXPAND,

        /**
         * shrinks the tray back to its original place
         */
        SHRINK
    }

    /**
     * The actions when the user lifts his finger
     */
    private enum Action {
        SEND,   // Send the audio
        CANCEL, // cancels and discards the recording
        STOP    // stops recording and wait for user's input
    }


}
