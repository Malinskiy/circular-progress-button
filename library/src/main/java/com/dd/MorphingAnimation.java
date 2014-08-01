package com.dd;

import android.animation.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

class MorphingAnimation {

    public static final int DURATION_INSTANT = 1;

    private OnAnimationEndListener mListener;

    private int mDuration;

    private int mFromWidth;
    private int mToWidth;

    private int mFromColor;
    private int mToColor;

    private int mFromStrokeColor;
    private int mToStrokeColor;
    private int mStrokeWidth;

    private float mFromCornerRadius;
    private float mToCornerRadius;

    private boolean mFromFill;
    private boolean mToFill;

    private float mPadding;

    private TextView         mView;
    private GradientDrawable mDrawable;
    private AnimatorSet      animatorSet;

    public MorphingAnimation(TextView viewGroup, GradientDrawable drawable) {
        mView = viewGroup;
        mDrawable = drawable;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public void setListener(OnAnimationEndListener listener) {
        mListener = listener;
    }

    public void setFromWidth(int fromWidth) {
        mFromWidth = fromWidth;
    }

    public void setToWidth(int toWidth) {
        mToWidth = toWidth;
    }

    public void setFromColor(int fromColor) {
        mFromColor = fromColor;
    }

    public void setToColor(int toColor) {
        mToColor = toColor;
    }

    public void setFromStrokeColor(int fromStrokeColor) {
        mFromStrokeColor = fromStrokeColor;
    }

    public void setToStrokeColor(int toStrokeColor) {
        mToStrokeColor = toStrokeColor;
    }

    public void setStrokeWidth(int strokeWidth) {
        mStrokeWidth = strokeWidth;
    }

    public void setFromCornerRadius(float fromCornerRadius) {
        mFromCornerRadius = fromCornerRadius;
    }

    public void setToCornerRadius(float toCornerRadius) {
        mToCornerRadius = toCornerRadius;
    }

    public void setToFill(boolean mToFill) {
        this.mToFill = mToFill;
    }

    public void setFromFill(boolean mFromFill) {
        this.mFromFill = mFromFill;
    }

    public void setPadding(float padding) {
        mPadding = padding;
    }

    public void start() {
        ValueAnimator widthAnimation = ValueAnimator.ofInt(mFromWidth, mToWidth);
        widthAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                int leftOffset;
                int rightOffset;
                int padding;

                boolean reducingSize = mFromWidth > mToWidth;
                if (reducingSize) {
                    leftOffset = (mFromWidth - value) / 2;
                    rightOffset = mFromWidth - leftOffset;
                    padding = (int) (mPadding * animation.getAnimatedFraction());
                } else {
                    leftOffset = (mToWidth - value) / 2;
                    rightOffset = mToWidth - leftOffset;
                    padding = (int) (mPadding - mPadding * animation.getAnimatedFraction());
                }

                int left = leftOffset + padding;
                int top = padding;
                int right = rightOffset - padding;
                int bottom = mView.getHeight() - padding;
                mDrawable.setBounds(left, top, right, bottom);
            }
        });

        ValueAnimator strokeColorAnimation = ValueAnimator.ofInt(mFromStrokeColor, mToStrokeColor);
        strokeColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mDrawable.setStroke(mStrokeWidth, (Integer) animation.getAnimatedValue());
            }
        });
        strokeColorAnimation.setEvaluator(new ArgbEvaluator());

        ObjectAnimator cornerAnimation =
                ObjectAnimator.ofFloat(mDrawable, "cornerRadius", mFromCornerRadius, mToCornerRadius);

        ObjectAnimator bgColorAnimation;
        if(mFromFill != mToFill) {
            if(mToFill) {
                bgColorAnimation = ObjectAnimator.ofInt(mDrawable, "color", Color.TRANSPARENT, mToColor);
            } else {
                bgColorAnimation = ObjectAnimator.ofInt(mDrawable, "color", mFromColor, Color.TRANSPARENT);
            }
        } else {
            bgColorAnimation = ObjectAnimator.ofInt(mDrawable, "color", mFromColor, mToColor);
        }
        bgColorAnimation.setEvaluator(new ArgbEvaluator());

        animatorSet = new AnimatorSet();
        animatorSet.setDuration(mDuration);
        animatorSet.playTogether(strokeColorAnimation, widthAnimation, bgColorAnimation, cornerAnimation);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mListener != null) {
                    mListener.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        animatorSet.start();
    }

    public void end() {
        if (animatorSet != null) animatorSet.end();
    }
}