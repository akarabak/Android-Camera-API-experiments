package com.example.dexel.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by dexel on 6/6/2017.
 */

public class AutoFitTextureView extends TextureView {

    private int mCameraWidth = 0;
    private int mCameraHeight = 0;
    private boolean mSquarePreview = false;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(int width, int height, boolean squarePreview) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mCameraWidth = width;
        mCameraHeight = height;
        mSquarePreview = squarePreview;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mCameraWidth || 0 == mCameraHeight) {
            setMeasuredDimension(width, height);
        } else {
            /**
             * Vertical orientation
             */
            if (width < height) {
                if (mSquarePreview) {
                    setTransform(squareTransform(width, height));
                    setMeasuredDimension(width, width);
                } else {
                    setMeasuredDimension(width, width * mCameraHeight / mCameraWidth);
                }
            }
            /**
             * Horizontal orientation
             */
            else {
                if (mSquarePreview) {
                    setTransform(squareTransform(width, height));
                    setMeasuredDimension(height, height);
                } else {
                    setMeasuredDimension(height * mCameraWidth / mCameraHeight, height);
                }
            }
        }
    }

    private Matrix setupTransform(int sw, int sh, int dw, int dh) {
        Matrix matrix = new Matrix();
        RectF src = new RectF(0, 0, sw, sh);
        RectF dst = new RectF(0, 0, dw, dh);
        RectF screen = new RectF(0, 0, dw, dh);

        matrix.postRotate(-90, screen.centerX(), screen.centerY());
        matrix.mapRect(dst);

        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
        matrix.mapRect(src);

        matrix.setRectToRect(screen, src, Matrix.ScaleToFit.FILL);
        matrix.postRotate(-90, screen.centerX(), screen.centerY());

        return matrix;
    }

    private Matrix squareTransform(int viewWidth, int viewHeight) {
        Matrix matrix = new Matrix();

        if (viewWidth < viewHeight) {
            matrix.setScale(1, (float) mCameraHeight / (float) mCameraWidth, viewWidth / 2, viewHeight / 2);
        } else {
            matrix.setScale((float) mCameraHeight / (float) mCameraWidth, 1, viewWidth / 2, viewHeight / 2);
        }

        return matrix;
    }
}