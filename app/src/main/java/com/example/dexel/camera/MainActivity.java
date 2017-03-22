package com.example.dexel.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private CameraDevice mCamera = null;
    Surface surface = null;
    TextureView textureView = null;
    MediaRecorder mRecorder = null;
    CameraCaptureSession mCameraCaptureSession = null;

    private final static String HOST_IP = "198.168.0.25";
    private final static int HOST_PORT = 7000;
    private Thread mBackgroundThread = null;

    private final static int MY_PERMISSIONS_CAMERA_PERMISSIONS = 1;
    private final static int MY_INTERNET_PERMISSIONS = 2;
    private final static int MY_STORAGE_PERMISSIONS = 3;

    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            , "Streamer");
    final File outputFile = new File(dir.getAbsolutePath(), "recording.mp4");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //open your camera here
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Transform you image captured size according to the surface width and height
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

    }

    private void checkPermissions() {
        Log.d(TAG, "Checking permissions");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting camera permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_CAMERA_PERMISSIONS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting network permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET},
                    MY_INTERNET_PERMISSIONS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting write permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_STORAGE_PERMISSIONS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Granting read permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_STORAGE_PERMISSIONS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBackgroundThread = new Thread(new Runnable() {
            public void run() {
                startRecord();
            }
        });
        mBackgroundThread.start();
    }

    private void startRecord() {
        Log.i(TAG, "Started recording");
        mRecorder = new MediaRecorder();
        try {
            //Socket socket = new Socket(HOST_IP, HOST_PORT);
            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            //mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mRecorder.setVideoEncodingBitRate(1000);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //mRecorder.setOutputFile(ParcelFileDescriptor.fromSocket(socket).getFileDescriptor());
            Log.d(TAG, "File: " + outputFile.getAbsolutePath());
            if (!dir.exists()) {
                dir.mkdir();
            }
            mRecorder.setOutputFile(outputFile.getAbsolutePath());
            //mRecorder.setPreviewDisplay(new Surface(textureView.getSurfaceTexture()));

            mRecorder.prepare();
            mRecorder.getSurface();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecord() {
        Log.i(TAG, "Stopped recording");
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            try {
                mBackgroundThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRecord();
        closeCamera();
    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    private void openCamera() {
        CameraManager cm = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        checkPermissions();

        try {
            String[] cameras = cm.getCameraIdList();
            //noinspection MissingPermission
            cm.openCamera(cameras[0], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "CameraState callback onOpened is called");
                    mCamera = camera;
                    try {
                        createPreview();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "CameraState callback onDisconnected is called");
                    camera.close();
                    mCamera = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "CameraState callback onError is called");
                }
            }, null);
            Log.d("Cameras", cameras.toString());

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void createPreview() throws CameraAccessException {

        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(500,500);
        surface = new Surface(surfaceTexture);
        try {
            mCamera.createCaptureSession(Arrays.asList(mRecorder.getSurface(), surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(surface);
                        captureRequestBuilder.addTarget(mRecorder.getSurface());

                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "configuring preview failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode){
            case MY_PERMISSIONS_CAMERA_PERMISSIONS: {
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
        }

    }

    /**
         * A native method that is implemented by the 'native-lib' native library,
         * which is packaged with this application.
         */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
