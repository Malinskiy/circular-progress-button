package com.dd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StateSet;
import com.dd.circular.progress.button.R;
import com.devspark.robototextview.widget.RobotoButton;

public class CircularProgressButton extends RobotoButton {

    public static final int     IDLE_STATE_PROGRESS  = 0;
    public static final int     ERROR_STATE_PROGRESS = -1;
    public static final boolean FILL_PROGRESS        = false;

    private CircularAnimatedDrawable mAnimatedDrawable;
    private CircularProgressDrawable mProgressDrawable;

    private ColorStateList mIdleColorState;
    private ColorStateList mCompleteColorState;
    private ColorStateList mErrorColorState;

    private StateListDrawable mIdleStateDrawable;
    private StateListDrawable mCompleteStateDrawable;
    private StateListDrawable mErrorStateDrawable;

    private StateManager mStateManager;
    private State        mState;
    private String       mIdleText;
    private String       mCompleteText;
    private String       mErrorText;

    private int      mColorProgress;
    private int      mColorIndicator;
    private int      mColorIndicatorBackground;
    private Drawable mIconComplete;
    private Drawable mIconError;
    private Drawable mIconIdle;
    private boolean  mFillIdle;
    private boolean  mFillComplete;
    private boolean  mFillError;
    private int      mStrokeWidth;
    private int      mPaddingProgress;
    private float    mCornerRadius;
    private boolean  mIndeterminateProgressMode;
    private boolean  mConfigurationChanged;
    private Drawable pendingCenteredDrawable;

    private int               mediumAnimationTime;
    private GradientDrawable  animationDrawable;
    private MorphingAnimation currentMorphingAnimation;

    private enum State {
        PROGRESS, IDLE, COMPLETE, ERROR
    }

    private int   mMaxProgress;
    private float mProgress;

    private boolean mMorphingInProgress;

    public CircularProgressButton(Context context) {
        super(context);
        init(context, null);
    }

    public CircularProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircularProgressButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void setIconComplete(Drawable mIconComplete) {
        this.mIconComplete = mIconComplete;
        if (mState.equals(State.COMPLETE)) {
            onShow(State.COMPLETE);
        }
    }

    public void setIconIdle(Drawable mIconIdle) {
        this.mIconIdle = mIconIdle;
        if (mState.equals(State.IDLE)) {
            onShow(State.IDLE);
        }
    }

    public void setIconError(Drawable mIconError) {
        this.mIconError = mIconError;
        if (mState.equals(State.ERROR)) {
            onShow(State.ERROR);
        }
    }

    private void init(Context context, AttributeSet attributeSet) {
        mStrokeWidth = (int) getContext().getResources().getDimension(R.dimen.stroke_width);

        initAttributes(context, attributeSet);

        mMaxProgress = 100;
        mState = State.IDLE;
        mStateManager = new StateManager(this);

        if (mIconIdle != null) {
            setLeftIcon(mIconIdle);
        }

        setText(mIdleText);

        mIdleStateDrawable = createStateListDrawable(mIdleColorState, mFillIdle);
        setBackgroundCompat(mIdleStateDrawable);

        mediumAnimationTime = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);
    }

    private StateListDrawable createStateListDrawable(ColorStateList colorStateList, boolean fill) {
        int colorNormal = getNormalColor(colorStateList);
        int colorPressed = getPressedColor(colorStateList);
        int colorFocused = getFocusedColor(colorStateList);
        int colorDisabled = getDisabledColor(colorStateList);

        GradientDrawable drawableNormal = createDrawable(colorNormal, fill);
        GradientDrawable drawableDisabled = createDrawable(colorDisabled, fill);
        GradientDrawable drawableFocused = createDrawable(colorFocused, fill);
        GradientDrawable drawablePressed = createDrawable(colorPressed, fill);
        StateListDrawable result = new StateListDrawable();

        result.addState(new int[]{android.R.attr.state_pressed}, drawablePressed);
        result.addState(new int[]{android.R.attr.state_focused}, drawableFocused);
        result.addState(new int[]{-android.R.attr.state_enabled}, drawableDisabled);
        result.addState(StateSet.WILD_CARD, drawableNormal);

        return result;
    }

    private int getNormalColor(ColorStateList colorStateList) {
        return colorStateList.getColorForState(new int[]{android.R.attr.state_enabled}, 0);
    }

    private int getPressedColor(ColorStateList colorStateList) {
        return colorStateList.getColorForState(new int[]{android.R.attr.state_pressed}, 0);
    }

    private int getFocusedColor(ColorStateList colorStateList) {
        return colorStateList.getColorForState(new int[]{android.R.attr.state_focused}, 0);
    }

    private int getDisabledColor(ColorStateList colorStateList) {
        return colorStateList.getColorForState(new int[]{-android.R.attr.state_enabled}, 0);
    }

    private GradientDrawable createDrawable(int color, boolean fill) {
        GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.background).mutate();
        if (fill) {
            drawable.setColor(color);
        } else {
            drawable.setColor(Color.TRANSPARENT);
        }

        drawable.setCornerRadius(mCornerRadius);
        drawable.setStroke(mStrokeWidth, color);

        return drawable;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        switch (mState) {
            case PROGRESS:
                setBackgroundCompat(null);
                break;
            case IDLE:
                if (mIdleStateDrawable == null)
                    mIdleStateDrawable = createStateListDrawable(mIdleColorState, mFillIdle);
                setBackgroundCompat(mIdleStateDrawable);
                break;
            case COMPLETE:
                if (mCompleteStateDrawable == null)
                    mCompleteStateDrawable = createStateListDrawable(mCompleteColorState, mFillComplete);
                setBackgroundCompat(mCompleteStateDrawable);
                break;
            case ERROR:
                if (mErrorStateDrawable == null)
                    mErrorStateDrawable = createStateListDrawable(mErrorColorState, mFillError);
                setBackgroundCompat(mErrorStateDrawable);
                break;
        }
    }

    private void initAttributes(Context context, AttributeSet attributeSet) {
        TypedArray attr = getTypedArray(context, attributeSet, R.styleable.CircularProgressButton);
        if (attr == null) {
            return;
        }

        try {

            mIdleText = attr.getString(R.styleable.CircularProgressButton_cpb_textIdle);
            mCompleteText = attr.getString(R.styleable.CircularProgressButton_cpb_textComplete);
            mErrorText = attr.getString(R.styleable.CircularProgressButton_cpb_textError);

            mFillIdle = attr.getBoolean(R.styleable.CircularProgressButton_cpb_fillIdle, true);
            mFillComplete = attr.getBoolean(R.styleable.CircularProgressButton_cpb_fillComplete, true);
            mFillError = attr.getBoolean(R.styleable.CircularProgressButton_cpb_fillError, true);

            int completeDrawableId = attr.getResourceId(R.styleable.CircularProgressButton_cpb_iconComplete, 0);
            if (completeDrawableId != 0) mIconComplete = getResources().getDrawable(completeDrawableId);

            int idleDrawableId = attr.getResourceId(R.styleable.CircularProgressButton_cpb_iconIdle, 0);
            if (idleDrawableId != 0) mIconIdle = getResources().getDrawable(idleDrawableId);

            int errorDrawableId = attr.getResourceId(R.styleable.CircularProgressButton_cpb_iconError, 0);
            if (errorDrawableId != 0) mIconError = getResources().getDrawable(errorDrawableId);

            mCornerRadius = attr.getDimension(R.styleable.CircularProgressButton_cpb_cornerRadius, 0);
            mPaddingProgress = attr.getDimensionPixelSize(R.styleable.CircularProgressButton_cpb_paddingProgress, 0);

            int blue = getColor(R.color.blue);
            int white = getColor(R.color.white);
            int grey = getColor(R.color.grey);

            int idleStateSelector = attr.getResourceId(R.styleable.CircularProgressButton_cpb_selectorIdle,
                                                       R.color.idle_state_selector);
            mIdleColorState = getResources().getColorStateList(idleStateSelector);

            int completeStateSelector = attr.getResourceId(R.styleable.CircularProgressButton_cpb_selectorComplete,
                                                           R.color.complete_state_selector);
            mCompleteColorState = getResources().getColorStateList(completeStateSelector);

            int errorStateSelector = attr.getResourceId(R.styleable.CircularProgressButton_cpb_selectorError,
                                                        R.color.error_state_selector);
            mErrorColorState = getResources().getColorStateList(errorStateSelector);

            mColorProgress = attr.getColor(R.styleable.CircularProgressButton_cpb_colorProgress, white);
            mColorIndicator = attr.getColor(R.styleable.CircularProgressButton_cpb_colorIndicator, blue);
            mColorIndicatorBackground =
                    attr.getColor(R.styleable.CircularProgressButton_cpb_colorIndicatorBackground, grey);
        } finally {
            attr.recycle();
        }
    }

    protected int getColor(int id) {
        return getResources().getColor(id);
    }

    protected TypedArray getTypedArray(Context context, AttributeSet attributeSet, int[] attr) {
        return context.obtainStyledAttributes(attributeSet, attr, 0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mProgress > 0 && mState == State.PROGRESS && !mMorphingInProgress) {
            if (mIndeterminateProgressMode) {
                drawIndeterminateProgress(canvas);
            } else {
                drawProgress(canvas);
            }
        }
    }

    private void drawIndeterminateProgress(Canvas canvas) {
        if (mAnimatedDrawable == null) {
            int offset = (getWidth() - getHeight()) / 2;
            mAnimatedDrawable = new CircularAnimatedDrawable(mColorIndicator, mStrokeWidth);
            int left = offset + mPaddingProgress;
            int right = getWidth() - offset - mPaddingProgress;
            int bottom = getHeight() - mPaddingProgress;
            int top = mPaddingProgress;
            mAnimatedDrawable.setBounds(left, top, right, bottom);
            mAnimatedDrawable.setCallback(this);
            mAnimatedDrawable.start();
        } else {
            mAnimatedDrawable.draw(canvas);
        }
    }

    private void drawProgress(Canvas canvas) {
        if (mProgressDrawable == null) {
            int offset = (getWidth() - getHeight()) / 2;
            int size = getHeight() - mPaddingProgress * 2;
            mProgressDrawable = new CircularProgressDrawable(size, mStrokeWidth, mColorIndicator);
            int left = offset + mPaddingProgress;
            mProgressDrawable.setBounds(left, mPaddingProgress, left, mPaddingProgress);
        }
        float sweepAngle = (360f / mMaxProgress) * mProgress;
        mProgressDrawable.setSweepAngle(sweepAngle);
        mProgressDrawable.draw(canvas);
    }

    public boolean isIndeterminateProgressMode() {
        return mIndeterminateProgressMode;
    }

    public void setIndeterminateProgressMode(boolean indeterminateProgressMode) {
        this.mIndeterminateProgressMode = indeterminateProgressMode;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mAnimatedDrawable || super.verifyDrawable(who);
    }

    private OnAnimationEndListener mProgressStateListener = new OnAnimationEndListener() {
        @Override
        public void onAnimationEnd() {
            setText(null);
            mMorphingInProgress = false;
            mState = State.PROGRESS;
            onShow(mState);

            mStateManager.checkState(CircularProgressButton.this);
        }
    };

    private OnAnimationEndListener mErrorStateListener = new OnAnimationEndListener() {
        @Override
        public void onAnimationEnd() {
            setText(mErrorText);
            mMorphingInProgress = false;
            mState = State.ERROR;
            onShow(mState);

            mStateManager.checkState(CircularProgressButton.this);
        }
    };

    private OnAnimationEndListener mIdleStateListener = new OnAnimationEndListener() {
        @Override
        public void onAnimationEnd() {
            setText(mIdleText);
            mMorphingInProgress = false;
            mState = State.IDLE;
            onShow(mState);

            mStateManager.checkState(CircularProgressButton.this);
        }
    };


    private OnAnimationEndListener mCompleteStateListener = new OnAnimationEndListener() {
        @Override
        public void onAnimationEnd() {
            setText(mCompleteText);
            mMorphingInProgress = false;
            mState = State.COMPLETE;
            onShow(mState);

            mStateManager.checkState(CircularProgressButton.this);
        }
    };

    private MorphingAnimation createMorphing() {
        mMorphingInProgress = true;

        resetAnimationDrawable();
        MorphingAnimation animation = new MorphingAnimation(this, animationDrawable);
        animation.setFromCornerRadius(mCornerRadius);
        animation.setToCornerRadius(mCornerRadius);

        animation.setFromWidth(getWidth());
        animation.setToWidth(getWidth());

        animation.setStrokeWidth(mStrokeWidth);

        if (mConfigurationChanged) {
            animation.setDuration(MorphingAnimation.DURATION_INSTANT);
        } else {
            animation.setDuration(mediumAnimationTime);
        }

        mConfigurationChanged = false;

        return animation;
    }

    private MorphingAnimation createProgressMorphing(float fromCorner, float toCorner, int fromWidth, int toWidth) {
        mMorphingInProgress = true;

        resetAnimationDrawable();
        MorphingAnimation animation = new MorphingAnimation(this, animationDrawable);
        animation.setFromCornerRadius(fromCorner);
        animation.setToCornerRadius(toCorner);

        animation.setPadding(mPaddingProgress);

        animation.setFromWidth(fromWidth);
        animation.setToWidth(toWidth);

        animation.setStrokeWidth(mStrokeWidth);

        if (mConfigurationChanged) {
            animation.setDuration(MorphingAnimation.DURATION_INSTANT);
        } else {
            animation.setDuration(mediumAnimationTime);
        }

        mConfigurationChanged = false;

        return animation;
    }

    private void morph(State from, State to) {
        currentMorphingAnimation = null;

        if (to.equals(State.PROGRESS)) {
            currentMorphingAnimation = createProgressMorphing(mCornerRadius, getHeight(), getWidth(), getHeight());
            currentMorphingAnimation.setToColor(mColorIndicatorBackground);
            currentMorphingAnimation.setToStrokeColor(mColorProgress);
            currentMorphingAnimation.setToFill(FILL_PROGRESS);
            currentMorphingAnimation.setListener(mProgressStateListener);
        } else if (from.equals(State.PROGRESS)) {
            currentMorphingAnimation = createProgressMorphing(getHeight(), mCornerRadius, getHeight(), getWidth());
            currentMorphingAnimation.setFromColor(mColorIndicatorBackground);
            currentMorphingAnimation.setFromStrokeColor(mColorProgress);
            currentMorphingAnimation.setFromFill(FILL_PROGRESS);
        } else {
            currentMorphingAnimation = createMorphing();
        }

        switch (to) {
            case IDLE:
                currentMorphingAnimation.setToColor(getNormalColor(mIdleColorState));
                currentMorphingAnimation.setToStrokeColor(getNormalColor(mIdleColorState));
                currentMorphingAnimation.setToFill(mFillIdle);
                currentMorphingAnimation.setListener(mIdleStateListener);
                break;
            case COMPLETE:
                currentMorphingAnimation.setToColor(getNormalColor(mCompleteColorState));
                currentMorphingAnimation.setToStrokeColor(getNormalColor(mCompleteColorState));
                currentMorphingAnimation.setToFill(mFillComplete);
                currentMorphingAnimation.setListener(mCompleteStateListener);
                break;
            case ERROR:
                currentMorphingAnimation.setToColor(getNormalColor(mErrorColorState));
                currentMorphingAnimation.setToStrokeColor(getNormalColor(mErrorColorState));
                currentMorphingAnimation.setToFill(mFillError);
                currentMorphingAnimation.setListener(mErrorStateListener);
                break;
        }

        onHide(from);
        switch (from) {
            case IDLE:
                currentMorphingAnimation.setFromColor(getNormalColor(mIdleColorState));
                currentMorphingAnimation.setFromStrokeColor(getNormalColor(mIdleColorState));
                currentMorphingAnimation.setFromFill(mFillIdle);
                break;
            case COMPLETE:
                currentMorphingAnimation.setFromColor(getNormalColor(mCompleteColorState));
                currentMorphingAnimation.setFromStrokeColor(getNormalColor(mCompleteColorState));
                currentMorphingAnimation.setFromFill(mFillComplete);
                break;
            case ERROR:
                currentMorphingAnimation.setFromColor(getNormalColor(mErrorColorState));
                currentMorphingAnimation.setFromStrokeColor(getNormalColor(mErrorColorState));
                currentMorphingAnimation.setFromFill(mFillError);
                break;
        }

        setBackgroundCompat(animationDrawable);
        currentMorphingAnimation.start();
    }

    private void onHide(State state) {
        switch (state) {
            case PROGRESS:
                break;
            case IDLE:
                if (mIconIdle != null) {
                    removeIcon();
                }
                setText(null);
                break;
            case COMPLETE:
                if (mIconComplete != null) {
                    removeIcon();
                }
                setText(null);
                break;
            case ERROR:
                if (mIconError != null) {
                    removeIcon();
                }
                setText(null);
                break;
        }
    }

    private void onShow(State state) {
        switch (state) {
            case PROGRESS:
                break;
            case IDLE:
                if (mIconIdle != null) {
                    if (!TextUtils.isEmpty(mIdleText)) {
                        setLeftIcon(mIconIdle);
                    } else {
                        setIcon(mIconIdle);
                    }
                }
                break;
            case COMPLETE:
                if (mIconComplete != null) {
                    if (!TextUtils.isEmpty(mCompleteText)) {
                        setLeftIcon(mIconComplete);
                    } else {
                        setIcon(mIconComplete);
                    }
                }
                break;
            case ERROR:
                if (mIconError != null) {
                    if (!TextUtils.isEmpty(mErrorText)) {
                        setLeftIcon(mIconError);
                    } else {
                        setIcon(mIconError);
                    }
                }
                break;
        }
        drawableStateChanged();
    }

    private void setIcon(Drawable drawable) {
        if (drawable != null) {
            pendingCenteredDrawable = drawable;
            int leftPadding = (getWidth() / 2) - (drawable.getIntrinsicWidth() / 2);
            setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            setPadding(leftPadding, 0, 0, 0);
        }
    }

    private void setLeftIcon(Drawable drawable) {
        pendingCenteredDrawable = null;
        if (drawable != null) {
            int rightPadding = (int) mCornerRadius / 2;
            int leftPadding = rightPadding;
            setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            setPadding(leftPadding, 0, rightPadding, 0);
        }
    }

    protected void removeIcon() {
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        setPadding(0, 0, 0, 0);
    }

    /**
     * Set the View's background. Masks the API changes made in Jelly Bean.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public void setBackgroundCompat(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(null);
            setBackground(drawable);
        } else {
            setBackgroundDrawable(null);
            setBackgroundDrawable(drawable);
        }
    }

    public void setProgress(float progress) {
        State newState = null;

        if (progress == mProgress || getWidth() == 0) {
            return;
        }

        mProgress = progress;
        mStateManager.saveProgress(this);

        if (mProgress >= mMaxProgress) {
            newState = State.COMPLETE;
        } else if (mProgress > IDLE_STATE_PROGRESS) {
            if (mState == State.PROGRESS) {
                invalidate();
                return;
            }
            newState = State.PROGRESS;
        } else if (mProgress == ERROR_STATE_PROGRESS) {
            newState = State.ERROR;
        } else if (mProgress == IDLE_STATE_PROGRESS) {
            newState = State.IDLE;
        }

        if (!mState.equals(newState)) {
            if(mMorphingInProgress) {
                if(currentMorphingAnimation == null) {
                    Log.wtf(CircularProgressButton.class.getSimpleName(), "Animation in progress but no reference to it...");
                } else {
                    currentMorphingAnimation.end();
                    currentMorphingAnimation = null;
                    mMorphingInProgress = false;
                }
            }

            morph(mState, newState);
        }
    }

    public float getProgress() {
        return mProgress;
    }

    public String getIdleText() {
        return mIdleText;
    }

    public String getCompleteText() {
        return mCompleteText;
    }

    public String getErrorText() {
        return mErrorText;
    }

    public void setIdleText(String text) {
        mIdleText = text;
    }

    public void setCompleteText(String text) {
        mCompleteText = text;
    }

    public void setErrorText(String text) {
        mErrorText = text;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            setProgress(mProgress);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (pendingCenteredDrawable != null) {
            int leftPadding = (w / 2) - (pendingCenteredDrawable.getIntrinsicWidth() / 2);
            int rightPadding = 0;
            pendingCenteredDrawable = null;

            setPadding(leftPadding, 0, rightPadding, 0);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mProgress = mProgress;
        savedState.mIndeterminateProgressMode = mIndeterminateProgressMode;
        savedState.mConfigurationChanged = true;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mProgress = savedState.mProgress;
            mIndeterminateProgressMode = savedState.mIndeterminateProgressMode;
            mConfigurationChanged = savedState.mConfigurationChanged;
            super.onRestoreInstanceState(savedState.getSuperState());
            setProgress(mProgress);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private void resetAnimationDrawable() {
        if (animationDrawable == null) {
            animationDrawable = createDrawable(Color.TRANSPARENT, true);
        } else {
            animationDrawable.setColor(Color.TRANSPARENT);
            animationDrawable.setStroke(mStrokeWidth, Color.TRANSPARENT);
        }
    }

    static class SavedState extends BaseSavedState {

        private boolean mIndeterminateProgressMode;
        private boolean mConfigurationChanged;
        private float   mProgress;

        public SavedState(Parcelable parcel) {
            super(parcel);
        }

        private SavedState(Parcel in) {
            super(in);
            mProgress = in.readInt();
            mIndeterminateProgressMode = in.readInt() == 1;
            mConfigurationChanged = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(mProgress);
            out.writeInt(mIndeterminateProgressMode ? 1 : 0);
            out.writeInt(mConfigurationChanged ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
