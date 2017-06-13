package com.example.dexel.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.Face;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static java.lang.Math.abs;

/**
 * Created by dexel on 6/6/2017.
 */

public class CustomSurface extends SurfaceView {
    private final static String TAG = CustomSurface.class.getSimpleName();

    private final Paint paint;
    private final SurfaceHolder mHolder;
    private final Context context;

    public CustomSurface(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

        setWillNotDraw(false);
    }

    public CustomSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

        setWillNotDraw(false);
    }

    public CustomSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

        setWillNotDraw(false);
    }

    public CustomSurface(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

        setWillNotDraw(false);
    }



//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//    }


//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        Log.d(TAG, "on touch");
//        super.onTouchEvent(event);
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            invalidate();
//            if (mHolder.getSurface().isValid()) {
//                final Canvas canvas = mHolder.lockCanvas();
//                Log.d("touch", "touchRecieved by camera");
//                if (canvas != null) {
//                    Log.d("touch", "touchRecieved CANVAS STILL Not Null");
//                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//                    canvas.drawColor(Color.TRANSPARENT);
//                    canvas.drawRect(event.getX() - 100, event.getY() + 100,
//                            event.getX() + 100, event.getY() - 100, paint);
//                    mHolder.unlockCanvasAndPost(canvas);
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            Canvas canvas1 = mHolder.lockCanvas();
//                            if(canvas1 !=null){
//                                canvas1.drawColor(0, PorterDuff.Mode.CLEAR);
//                                mHolder.unlockCanvasAndPost(canvas1);
//                            }
//
//                        }
//                    }, 1000);
//
//                }
//            }
//        }
//
//
//        return false;
//    }


    public void onDetectEvent(Face face) {
        invalidate();
        if (mHolder.getSurface().isValid()) {
            final Canvas canvas = mHolder.lockCanvas();
            Log.d("touch", "touchRecieved by camera");
            if (canvas != null) {
                Log.d("touch", "touchRecieved CANVAS STILL Not Null");
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawColor(Color.TRANSPARENT);

                float v_scalar = canvas.getHeight() / 2000f;
                float h_scalar = canvas.getWidth() / 2000f;

                float left = face.getBounds().left * h_scalar;
                float right = face.getBounds().right * h_scalar;
                float top = face.getBounds().top * v_scalar;
                float bottom = face.getBounds().bottom * v_scalar;


                Log.d(TAG, left + " " + right + " " + top + " " + bottom);

                canvas.drawRect(left, top, right, bottom, paint);
                mHolder.unlockCanvasAndPost(canvas);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Canvas canvas1 = mHolder.lockCanvas();
                        if (canvas1 != null) {
                            canvas1.drawColor(0, PorterDuff.Mode.CLEAR);
                            mHolder.unlockCanvasAndPost(canvas1);
                        }

                    }
                }, 1000);

            }
        }
    }
}
