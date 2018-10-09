package com.biglabs.mozo.sdk.ui.view;

/**
 * Created by MyInnos on 31-01-2017.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.biglabs.mozo.sdk.R;

public class FastScrollRecyclerView extends RecyclerView {

    private FastScrollRecyclerSection mScroller = null;
    private GestureDetector mGestureDetector = null;

    private boolean mEnabled = true;

    public int setIndexTextSize = 12;
    public float mIndexBarWidth = 20;
    public float mIndexBarMargin = 5;
    public float mPreviewPadding = 5;
    public int mIndexBarCornerRadius = 5;
    public float mIndexBarTransparentValue = (float) 0.6;
    public @ColorInt
    int mIndexbarBackgroudColor = Color.TRANSPARENT;
    public @ColorInt
    int mIndexbarTextColor = Color.WHITE;
    public @ColorInt
    int mIndexbarHighLateTextColor = Color.BLACK;

    public int mPreviewTextSize = 50;
    public @ColorInt
    int mPreviewBackgroudColor = Color.BLACK;
    public @ColorInt
    int mPreviewTextColor = Color.WHITE;
    public float mPreviewTransparentValue = (float) 0.4;

    public FastScrollRecyclerView(Context context) {
        super(context);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView, 0, 0);

            if (typedArray != null) {
                try {
                    setIndexTextSize = typedArray.getInt(R.styleable.FastScrollRecyclerView_setIndexTextSize, setIndexTextSize);
                    mIndexBarWidth = typedArray.getDimension(R.styleable.FastScrollRecyclerView_setIndexBarWidth, mIndexBarWidth);
                    mIndexBarMargin = typedArray.getDimension(R.styleable.FastScrollRecyclerView_setIndexBarMargin, mIndexBarMargin);
                    mPreviewPadding = typedArray.getDimension(R.styleable.FastScrollRecyclerView_setPreviewPadding, mPreviewPadding);
                    mIndexBarCornerRadius = typedArray.getInt(R.styleable.FastScrollRecyclerView_setIndexBarCornerRadius, mIndexBarCornerRadius);
                    mIndexBarTransparentValue = typedArray.getFloat(R.styleable.FastScrollRecyclerView_setIndexBarTransparentValue, mIndexBarTransparentValue);

                    if (typedArray.hasValue(R.styleable.FastScrollRecyclerView_setIndexBarColor)) {
                        mIndexbarBackgroudColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_setIndexBarColor, mIndexbarBackgroudColor);
                    }

                    if (typedArray.hasValue(R.styleable.FastScrollRecyclerView_setIndexBarTextColor)) {
                        mIndexbarTextColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_setIndexBarTextColor, mIndexbarTextColor);
                    }

                    if (typedArray.hasValue(R.styleable.FastScrollRecyclerView_setIndexBarHighlightTextColor)) {
                        mIndexbarHighLateTextColor = Color.parseColor(typedArray.getString(R.styleable.FastScrollRecyclerView_setIndexBarHighlightTextColor));
                    }

                    if (typedArray.hasValue(R.styleable.FastScrollRecyclerView_setIndexBarHighlightTextColorRes)) {
                        mIndexbarHighLateTextColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_setIndexBarHighlightTextColor, mIndexbarHighLateTextColor);
                    }

                    mPreviewTextSize = typedArray.getInt(R.styleable.FastScrollRecyclerView_setPreviewTextSize, mPreviewTextSize);
                    mPreviewTransparentValue = typedArray.getFloat(R.styleable.FastScrollRecyclerView_setPreviewTransparentValue, mPreviewTransparentValue);

                    if (typedArray.hasValue(R.styleable.FastScrollRecyclerView_setPreviewColor)) {
                        mPreviewBackgroudColor = Color.parseColor(typedArray.getString(R.styleable.FastScrollRecyclerView_setPreviewColor));
                    }

                    if (typedArray.hasValue(R.styleable.FastScrollRecyclerView_setPreviewTextColor)) {
                        mPreviewTextColor = Color.parseColor(typedArray.getString(R.styleable.FastScrollRecyclerView_setPreviewTextColor));
                    }
                    
                } finally {
                    typedArray.recycle();
                }
            }
        }
        mScroller = new FastScrollRecyclerSection(context, this);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Overlay index bar
        if (mScroller != null)
            mScroller.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mEnabled) {
            // Intercept ListView's touch event
            if (mScroller != null && mScroller.onTouchEvent(ev))
                return true;

            if (mGestureDetector == null) {
                mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        return super.onFling(e1, e2, velocityX, velocityY);
                    }

                });
            }
            mGestureDetector.onTouchEvent(ev);
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mEnabled && mScroller != null && mScroller.contains(ev.getX(), ev.getY()))
            return true;

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        if (mScroller != null)
            mScroller.setAdapter(adapter);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mScroller != null)
            mScroller.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * @param value int to set the text size of the index bar
     */
    public void setIndexTextSize(int value) {
        mScroller.setIndexTextSize(value);
    }

    /**
     * @param value float to set the width of the index bar
     */
    public void setIndexBarWidth(float value) {
        mScroller.setIndexBarWidth(value);
    }

    /**
     * @param value float to set the margin of the index bar
     */
    public void setIndexBarMargin(float value) {
        mScroller.setIndexBarMargin(value);
    }

    /**
     * @param value int to set the preview padding
     */
    public void setPreviewPadding(int value) {
        mScroller.setPreviewPadding(value);
    }

    /**
     * @param value int to set the corner radius of the index bar
     */
    public void setIndexBarCornerRadius(int value) {
        mScroller.setIndexBarCornerRadius(value);
    }

    /**
     * @param value float to set the transparency value of the index bar
     */
    public void setIndexBarTransparentValue(float value) {
        mScroller.setIndexBarTransparentValue(value);
    }

    /**
     * @param typeface Typeface to set the typeface of the preview & the index bar
     */
    public void setTypeface(Typeface typeface) {
        mScroller.setTypeface(typeface);
    }

    /**
     * @param shown boolean to show or hide the index bar
     */
    public void setIndexBarVisibility(boolean shown) {
        mScroller.setIndexBarVisibility(shown);
        mEnabled = shown;
    }

    /**
     * @param shown boolean to show or hide the preview
     */
    public void setPreviewVisibility(boolean shown) {
        mScroller.setPreviewVisibility(shown);
    }

    /**
     * @param value int to set the text size of the preview box
     */
    public void setPreviewTextSize(int value) {
        mScroller.setPreviewTextSize(value);
    }

    /**
     * @param color The color for the preview box
     */
    public void setPreviewColor(@ColorRes int color) {
        int colorValue = getContext().getResources().getColor(color);
        mScroller.setPreviewColor(colorValue);
    }

    /**
     * @param color The color for the preview box
     */
    public void setPreviewColor(String color) {
        mScroller.setPreviewColor(Color.parseColor(color));
    }

    /**
     * @param color The text color for the preview box
     */
    public void setPreviewTextColor(@ColorRes int color) {
        int colorValue = getContext().getResources().getColor(color);
        mScroller.setPreviewTextColor(colorValue);
    }

    /**
     * @param value float to set the transparency value of the preview box
     */
    public void setPreviewTransparentValue(float value) {
        mScroller.setPreviewTransparentValue(value);
    }

    /**
     * @param color The text color for the preview box
     */
    public void setPreviewTextColor(String color) {
        mScroller.setPreviewTextColor(Color.parseColor(color));
    }

    /**
     * @param color The color for the index bar
     */
    public void setIndexBarColor(String color) {
        mScroller.setIndexBarColor(Color.parseColor(color));
    }

    /**
     * @param color The color for the index bar
     */
    public void setIndexBarColor(@ColorRes int color) {
        int colorValue = getContext().getResources().getColor(color);
        mScroller.setIndexBarColor(colorValue);
    }


    /**
     * @param color The text color for the index bar
     */
    public void setIndexBarTextColor(String color) {
        mScroller.setIndexBarTextColor(Color.parseColor(color));
    }

    /**
     * @param color The text color for the index bar
     */
    public void setIndexBarTextColor(@ColorRes int color) {
        int colorValue = getContext().getResources().getColor(color);
        mScroller.setIndexBarTextColor(colorValue);
    }

    /**
     * @param color The text color for the index bar
     */
    public void setIndexbarHighLateTextColor(String color) {
        mScroller.setIndexBarHighLateTextColor(Color.parseColor(color));
    }

    /**
     * @param color The text color for the index bar
     */
    public void setIndexbarHighLateTextColor(@ColorRes int color) {
        int colorValue = getContext().getResources().getColor(color);
        mScroller.setIndexBarHighLateTextColor(colorValue);
    }

    /**
     * @param shown boolean to show or hide the index bar
     */
    public void setIndexBarHighLateTextVisibility(boolean shown) {
        mScroller.setIndexBarHighLateTextVisibility(shown);
    }
}