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
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
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
    private int mCameraID = 0;
    private CameraManager mCameraManager = null;

    private int recording = 0;


    Mat mGray = null;
    Mat mRgba = null;
    File mCascade = null;

    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            , "Streamer");
    final File outputFile = new File(dir.getAbsolutePath());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        //Sets the view for this activity
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.textureView);


        initializeDependencies();

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //open your camera here and get texture view surface ready

                MainActivity.this.surface = new Surface(surface);

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

    private void initializeDependencies() {
        try{
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            mCascade = new File(getFilesDir(), "cascade_face.xml");
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(mCascade);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1){
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private void switchCamera() throws CameraAccessException {
        mCameraID++;
        String[] cameras = mCameraManager.getCameraIdList();
        mCameraID %= cameras.length;
        if (mCamera != null){
            mCamera.close();
            mCamera = null;
        }
        openCamera();
    }

    private void openCamera() {
        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED);

        try {
            String[] cameras = mCameraManager.getCameraIdList();
            //noinspection MissingPermission


            mCameraManager.openCamera(cameras[mCameraID], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "CameraState callback onOpened is called");
                    mCamera = camera;
                    createPreviewSession();

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
            Log.d("Cameras", Arrays.asList(cameras).toString());

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Preview code ##################################################################

    private void createPreviewSession(){
        try {
            mCamera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(surface);

                        //mCameraCaptureSession = session;
                        //mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                        session.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "configuring preview failed");
                    //mCameraCaptureSession = session;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onStart() {
        super.onStart();

        final ToggleButton record = (ToggleButton) findViewById(R.id.record);
        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //prepareRecord();
                    //createCaptureSession();
                    prepareRecord();
                    createCaptureSession();
                    mRecorder.start();
                }
                else {
                    stopRecord();
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

    //Recording code #################################################

    private void prepareRecord() {
        //Prepares media recorder
        Log.i(TAG, "Started recording");
        if (mRecorder == null){
            mRecorder = new MediaRecorder();

            //prepare recorder
            //createPreview();
            //Socket socket = new Socket(HOST_IP, HOST_PORT);
            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            //mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mRecorder.setVideoEncodingBitRate(1000);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //mRecorder.setOutputFile(ParcelFileDescriptor.fromSocket(socket).getFileDescriptor());
            Log.d(TAG, "File: " + outputFile.getAbsolutePath());
            if (!dir.exists()) {
                if (!dir.mkdir()){
                    Log.e(TAG, "Directory failed to create");
                }
            }
            mRecorder.setOutputFile(new File(dir.getAbsolutePath(),
                    "recording" + Integer.toString(recording) + ".mp4").getAbsolutePath());
            recording++;
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private int frameNum = 0;
    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureCompleted(final CameraCaptureSession session, CaptureRequest request,
                                           TotalCaptureResult result) {
                if (frameNum == 0) {
                    //Log.d(TAG, "new thread");
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Mat toRecognize = new Mat();


                            Utils.bitmapToMat(textureView.getBitmap(), toRecognize);
                            ImageProcessing(toRecognize);
                        }

                    }
                    );
                    t.run();
                    frameNum++;
                }
                else{
                    //Log.d(TAG, "increase counter");
                    frameNum = (frameNum + 1) % 60;
                }


            }
    };

    private void createCaptureSession(){
        try {
            mCamera.createCaptureSession(Arrays.asList(mRecorder.getSurface(), surface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(mRecorder.getSurface());
                        captureRequestBuilder.addTarget(surface);

                        //mCameraCaptureSession = session;
                        //mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                        session.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "configuring preview failed");
                    //mCameraCaptureSession = session;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void stopRecord() {
        Log.i(TAG, "Stopped recording");
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

            createPreviewSession();

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
            //mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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


    private Mat ImageProcessing(Mat rgb){
        mGray = new Mat();
        if (rgb != null && !rgb.empty()){
            Imgproc.cvtColor(rgb, mGray, Imgproc.COLOR_RGB2GRAY);
            mRgba = rgb;

            try {
                CascadeClassifier classifier = new CascadeClassifier(mCascade.getAbsolutePath());
                MatOfRect detectedFaces = new MatOfRect();
                classifier.detectMultiScale(mGray, detectedFaces, 1.1, 10, 0, new Size(20,20), new Size(mGray.width(), mGray.height()));


                for(Rect f : detectedFaces.toList()){
                    Log.i("Face Detected: ", f.toString());
                    Toast.makeText(this, "FACE DETECTED", Toast.LENGTH_SHORT).show();
                }
                return mRgba;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return null;
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
        }
        else{
            Log.i(TAG, "OPenCV initialization failed");
        }
    }

}
