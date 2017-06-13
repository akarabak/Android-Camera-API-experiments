package com.example.dexel.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


//So far -> recreate mediaRecorder and captureSession after each toggle

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private final static String TAG = MainActivity.class.getSimpleName();
    Surface surface = null;
    AutoFitTextureView textureView = null;

    private final static String HOST_IP = "198.168.0.25";
    private final static int HOST_PORT = 7000;
    private Thread mBackgroundThread = null;



    private final static int MY_CAMERA_PERMISSIONS = 0;
    private final static int MY_INTERNET_PERMISSIONS = 1;
    private final static int MY_STORAGE_PERMISSIONS = 2;
    private final static int MY_MIC_PERMISSIONS = 3;

    Camera mCamera = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();


        while (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display mDisplay = windowManager.getDefaultDisplay();
        mDisplay.getRotation();

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);



        //Sets the view for this activity
        setContentView(R.layout.activity_main);




        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        textureView = (AutoFitTextureView) findViewById(R.id.textureView);
//        textureView.setAspectRatio(textureView.getWidth(), textureView.getHeight(), false);
//        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                //open your camera here and get texture view surface ready
//
//                MainActivity.this.surface = new Surface(surface);
//
//                openCamera();
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//                // Transform you image captured size according to the surface width and height
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                return true;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//            }
//        });

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

    }




    private void switchCamera() throws CameraAccessException {
        mCamera.switchCamera();
    }



    @Override
    protected void onStart() {
        super.onStart();

        final ToggleButton record = (ToggleButton) findViewById(R.id.record);
        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //prepareRecord();
                    //createCaptureSession();
                    mCamera.startRecord();
                } else {
                    mCamera.stopRecord();
                }
            }
        });
        final Button switchButton = (Button) findViewById(R.id.switch_button);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    switchCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

    }



    @Override
    protected void onStop() {
        super.onStop();
        mCamera.stopRecord();
        mCamera.closeCamera();
    }



    private void checkPermissions() {
        Log.d(TAG, "Checking permissions");
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting camera permissions");
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_PERMISSIONS);
        }
        if (checkSelfPermission(Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting network permissions");
            requestPermissions(new String[]{Manifest.permission.INTERNET},
                    MY_INTERNET_PERMISSIONS);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting write permissions");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_STORAGE_PERMISSIONS);
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting read permissions");
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_STORAGE_PERMISSIONS);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting record audio permissions");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_MIC_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_CAMERA_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, permissions[0] + " required", Toast.LENGTH_SHORT).show();
                    finish();
                }

            }
            case MY_INTERNET_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, permissions[0] + " required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            case MY_STORAGE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, permissions[0] + " required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            case MY_MIC_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, permissions[0] + " required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }




    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV successfully initialized");
            //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "OPenCV initialization failed");
        }
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surface = holder.getSurface();
        mCamera = new Camera(MainActivity.this, surface);
        mCamera.openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
