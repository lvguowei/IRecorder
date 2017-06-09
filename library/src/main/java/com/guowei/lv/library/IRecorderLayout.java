
package com.guowei.lv.library;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Layout for audio recording.
 */
public class IRecorderLayout extends RelativeLayout {

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
     * Offset for activate the buttons (dp)
     */
    private int activationPointOffset = 10;

    private final static int EXPAND_ANIMATION_DURATION = 300;

    private final static int SHRINK_ANIMATION_DURATION = 400;

    private FrameLayout recordButton;

    private FrameLayout playButton;

    private FrameLayout pauseButton;

    private FrameLayout cancelButton;

    private FrameLayout sendButton;

    private ImageView cancelButtonBg;

    private ImageView sendButtonBg;

    private ImageView audioRecorderBgView;

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

    private AnimationType animationType = AnimationType.SHRINK;

    private boolean duringAnimation;

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

    /**
     * Initializes the view
     */
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.irecorder_layout, this, true);
        recordButton = (FrameLayout) findViewById(R.id.record_button);
        playButton = (FrameLayout) findViewById(R.id.play_button);
        pauseButton = (FrameLayout) findViewById(R.id.pause_button);
        cancelButton = (FrameLayout) findViewById(R.id.cancel_button);
        sendButton = (FrameLayout) findViewById(R.id.send_button);
        cancelButtonBg = (ImageView) findViewById(R.id.cancel_button_bg);
        sendButtonBg = (ImageView) findViewById(R.id.send_button_bg);
        audioRecorderBgView = (ImageView) findViewById(R.id.audio_recorder_bg_view);

    }

    /**
     *
     */
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

    /**
     *
     */
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

}
