package org.littlebrother.telegram.ui.Cells;

import static org.littlebrother.telegram.messenger.AndroidUtilities.dp;
import static org.littlebrother.telegram.messenger.LocaleController.formatString;
import static org.littlebrother.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import org.checkerframework.checker.units.qual.A;
import org.littlebrother.telegram.messenger.AndroidUtilities;
import org.littlebrother.telegram.messenger.LocaleController;
import org.littlebrother.telegram.messenger.Utilities;
import org.littlebrother.telegram.ui.ActionBar.Theme;
import org.littlebrother.telegram.ui.ChannelMonetizationLayout;
import org.littlebrother.telegram.ui.Components.AnimatedEmojiDrawable;
import org.littlebrother.telegram.ui.Components.AnimatedTextView;
import org.littlebrother.telegram.ui.Components.CubicBezierInterpolator;
import org.littlebrother.telegram.ui.Components.LayoutHelper;
import org.littlebrother.telegram.ui.Components.SeekBarView;

import java.util.Arrays;

public class SlideIntChooseView extends FrameLayout {

    private final Theme.ResourcesProvider resourcesProvider;

    private final AnimatedTextView minText;
    private final AnimatedTextView valueText;
    private final AnimatedTextView maxText;
    private final SeekBarView seekBarView;

    private int stepsCount;

    public SlideIntChooseView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        this.resourcesProvider = resourcesProvider;

        minText = new AnimatedTextView(context, true, true, true);
        minText.setAnimationProperties(.3f, 0, 220, CubicBezierInterpolator.EASE_OUT_QUINT);
        minText.setTextSize(dp(13));
        minText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        minText.setGravity(Gravity.LEFT);
        minText.setEmojiCacheType(AnimatedEmojiDrawable.CACHE_TYPE_COLORABLE);
        minText.setEmojiColor(Color.WHITE);
        addView(minText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 25, Gravity.TOP, 22, 13, 22, 0));

        valueText = new AnimatedTextView(context, false, true, true);
        valueText.setAnimationProperties(.3f, 0, 220, CubicBezierInterpolator.EASE_OUT_QUINT);
        valueText.setTextSize(dp(13));
        valueText.setGravity(Gravity.CENTER);
        valueText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText, resourcesProvider));
        valueText.setEmojiColor(Color.WHITE);
        valueText.setEmojiCacheType(AnimatedEmojiDrawable.CACHE_TYPE_COLORABLE);
        addView(valueText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 25, Gravity.TOP, 22, 13, 22, 0));

        maxText = new AnimatedTextView(context, true, true, true);
        maxText.setAnimationProperties(.3f, 0, 220, CubicBezierInterpolator.EASE_OUT_QUINT);
        maxText.setTextSize(dp(13));
        maxText.setGravity(Gravity.RIGHT);
        maxText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        maxText.setEmojiColor(Color.WHITE);
        maxText.setEmojiCacheType(AnimatedEmojiDrawable.CACHE_TYPE_COLORABLE);
        addView(maxText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 25, Gravity.TOP, 22, 13, 22, 0));

        seekBarView = new SeekBarView(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onTouchEvent(event);
            }
        };
        seekBarView.setReportChanges(true);
        seekBarView.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                if (options == null || whenChanged == null) {
                    return;
                }
                final int newValue = (int) Math.round(options.min + stepsCount * progress);
                if (value != newValue) {
                    value = newValue;
                    AndroidUtilities.vibrateCursor(seekBarView);
                    updateTexts(value, true);
                    if (whenChanged != null) {
                        whenChanged.run(value);
                    }
                }
            }

            @Override
            public int getStepsCount() {
                return stepsCount;
            }
        });
        addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.FILL_HORIZONTAL, 6, 30, 6, 0));
    }

    private int value;
    private Utilities.Callback<Integer> whenChanged;
    private Options options;

    public void set(
        int value,
        Options options,
        Utilities.Callback<Integer> whenChanged
    ) {
        this.value = value;
        this.options = options;
        this.whenChanged = whenChanged;

        stepsCount = options.max - options.min;
        seekBarView.setProgress((value - options.min) / (float) stepsCount, false);

        updateTexts(value, false);
    }

    public void updateTexts(int value, boolean animated) {
        minText.cancelAnimation();
        maxText.cancelAnimation();
        if (!TextUtils.isEmpty(options.resId)) {
            valueText.cancelAnimation();
            valueText.setText(LocaleController.formatPluralString(options.resId, value), animated);
            minText.setText("" + options.min, animated);
            maxText.setText("" + options.max, animated);
        } else {
            int valueResId;
            if (value <= options.min) {
                valueResId = options.valueMinStringResId;
            } else if (value < options.max) {
                valueResId = options.valueStringResId;
            } else {
                valueResId = options.valueMaxStringResId;
            }
            valueText.cancelAnimation();
            valueText.setText(processText(valueResId, value), animated);
            minText.setText(processText(options.minStringResId, options.min), animated);
            maxText.setText(processText(options.maxStringResId, options.max), animated);
        }
        maxText.setTextColor(Theme.getColor(value >= options.max ? Theme.key_windowBackgroundWhiteValueText : Theme.key_windowBackgroundWhiteGrayText, resourcesProvider), animated);
        setMaxTextEmojiSaturation(value >= options.max ? 1f : 0f, animated);
    }

    private float maxTextEmojiSaturation;
    private float toMaxTextEmojiSaturation = -1f;
    private ValueAnimator maxTextEmojiSaturationAnimator;
    private void setMaxTextEmojiSaturation(float value, boolean animated) {
        if (Math.abs(toMaxTextEmojiSaturation - value) < 0.01f) {
            return;
        }
        if (maxTextEmojiSaturationAnimator != null) {
            maxTextEmojiSaturationAnimator.cancel();
            maxTextEmojiSaturationAnimator = null;
        }
        toMaxTextEmojiSaturation = value;
        if (animated) {
            maxTextEmojiSaturationAnimator = ValueAnimator.ofFloat(maxTextEmojiSaturation, value);
            maxTextEmojiSaturationAnimator.addUpdateListener(anm -> {
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(maxTextEmojiSaturation = (float) anm.getAnimatedValue());
                if (Theme.isCurrentThemeDark()) {
                    AndroidUtilities.adjustBrightnessColorMatrix(colorMatrix, -.3f * (1f - maxTextEmojiSaturation));
                }
                maxText.setEmojiColorFilter(new ColorMatrixColorFilter(colorMatrix));
            });
            maxTextEmojiSaturationAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ColorMatrix colorMatrix = new ColorMatrix();
                    colorMatrix.setSaturation(maxTextEmojiSaturation = value);
                    if (Theme.isCurrentThemeDark()) {
                        AndroidUtilities.adjustBrightnessColorMatrix(colorMatrix, -.3f * (1f - maxTextEmojiSaturation));
                    }
                    maxText.setEmojiColorFilter(new ColorMatrixColorFilter(colorMatrix));
                }
            });
            maxTextEmojiSaturationAnimator.setDuration(240);
            maxTextEmojiSaturationAnimator.start();
        } else {
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(maxTextEmojiSaturation = value);
            if (Theme.isCurrentThemeDark()) {
                AndroidUtilities.adjustBrightnessColorMatrix(colorMatrix, -.3f * (1f - maxTextEmojiSaturation));
            }
            maxText.setEmojiColorFilter(new ColorMatrixColorFilter(colorMatrix));
        }
    }

    private CharSequence processText(int resId, int value) {
        String string = getString(resId);
        string = string.replace("%d", "" + value);
        CharSequence cs = AndroidUtilities.replaceTags(string);
        cs = ChannelMonetizationLayout.replaceTON(cs, valueText.getPaint());
        return cs;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(dp(75), MeasureSpec.EXACTLY)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setSystemGestureExclusionRects(Arrays.asList(
                new Rect(0, 0, dp(80), getMeasuredHeight()),
                new Rect(getMeasuredWidth() - dp(80), 0, getMeasuredWidth(), getMeasuredHeight())
            ));
        }
    }

    public static class Options {
        public int style;

        public int min;
        public int max;

        public String resId;

        public int minStringResId;
        public int valueMinStringResId, valueStringResId, valueMaxStringResId;
        public int maxStringResId;

        public static Options make(
            int style,
            int min, int minStringResId,
            int valueMinStringResId, int valueStringResId, int valueMaxStringResId,
            int max, int maxStringResId
        ) {
            Options o = new Options();
            o.style = style;
            o.min = min;
            o.minStringResId = minStringResId;
            o.valueMinStringResId = valueMinStringResId;
            o.valueStringResId = valueStringResId;
            o.valueMaxStringResId = valueMaxStringResId;
            o.max = max;
            o.maxStringResId = maxStringResId;
            return o;
        }

        public static Options make(
            int style,
            String resId, int min, int max
        ) {
            Options o = new Options();
            o.style = style;
            o.min = min;
            o.resId = resId;
            o.max = max;
            return o;
        }
    }
}
