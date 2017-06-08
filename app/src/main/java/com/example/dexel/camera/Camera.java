package com.example.dexel.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

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
import java.util.Arrays;

/**
 * Created by dexel on 6/7/2017.
 */

public class Camera {
    private final static String TAG = Camera.class.getSimpleName();

    private Context mContext = null;
    private Activity mActivity = null;
    private Surface mSurface = null;

    // Camera variables
    private int mCameraID = 0;
    private CameraManager mCameraManager = null;
    private CameraDevice mCamera = null;
    private CameraCaptureSession mCameraCaptureSession = null;

    // Recorder variables
    private MediaRecorder mRecorder = null;


    // Variables for storage
    private File outputDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            , "Streamer");
    private int recordingNum = 0;


    private File mCascade = null;


    public Camera(Activity activity, Surface surface){
        mActivity = activity;
        mContext = activity.getBaseContext();
        mSurface = surface;
        initializeDependencies();
    }

    private void initializeDependencies() {
        try {

            InputStream is = mContext.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            mCascade = new File(mContext.getFilesDir(), "cascade_face.xml");
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(mCascade);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switchCamera() {
        mCameraID++;
        String[] cameras = new String[0];
        try {
            cameras = mCameraManager.getCameraIdList();
            mCameraID %= cameras.length;
            if (mCamera != null) {
                mCamera.close();
                mCamera = null;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        openCamera();
    }


    public void openCamera() {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);


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



    private void printCharacteristics(String camera) throws CameraAccessException {
        Log.d(TAG, "printCharacteristics");
        CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(camera);
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        for(int i=0; i < modes.length; i++){
            Log.i(TAG, "Face detect modes: " + modes[i]);
        }

        Log.i(TAG, "FACE COUNT " + cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT).toString());
        Log.d(TAG, cameraCharacteristics.toString());
    }

    private void setFaceDetect(CaptureRequest.Builder captureRequestBuilder) throws CameraAccessException {
        CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCamera.getId());
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);

        captureRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, modes[modes.length-1]);

    }


    //Preview code ##################################################################

    private void createPreviewSession() {
        try {
            mCamera.createCaptureSession(Arrays.asList(mSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        printCharacteristics(mCamera.getId());

                        CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        setFaceDetect(captureRequestBuilder);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

                        captureRequestBuilder.addTarget(mSurface);
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

    //Recording code #################################################

    public void startRecord() {
        //Prepares media recorder
        Log.i(TAG, "Started recording");
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();

            //prepare recorder
            //createPreview();
            //Socket socket = new Socket(HOST_IP, HOST_PORT);
            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));

            //mRecorder.setOutputFile(ParcelFileDescriptor.fromSocket(socket).getFileDescriptor());
            if (!outputDir.exists()) {
                if (!outputDir.mkdir()) {
                    Log.e(TAG, "Directory failed to create");
                }
            }
            mRecorder.setOutputFile(new File(outputDir.getAbsolutePath(),
                    "recordingNum" + Integer.toString(recordingNum) + ".mp4").getAbsolutePath());
            recordingNum++;
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        createCaptureSession();
    }

    private int frameNum = 0;
    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {


        @Override
        public void onCaptureCompleted(final CameraCaptureSession session, final CaptureRequest request,
                                       final TotalCaptureResult result) {

//            if (frameNum == 0) {
//                Log.i(TAG, "onCaptureCompleted");
//                final Bitmap b = textureView.getBitmap();
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        //Log.i(TAG, "Recognition running " + Thread.currentThread().getId());
//                        Mat toRecognize = new Mat();
//                        Utils.bitmapToMat(b, toRecognize);
//                        ImageProcessing(toRecognize);
//
//
//                        int mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
//
//                        Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
//                        for (Face f : faces){
//                            Log.i(TAG, "FACE: " + f.toString());
//                            android.graphics.Rect rect = f.getBounds();
//
//                        }
//                    }
//
//                }
//                ).start();
//                frameNum++;
//            } else {
//                frameNum = (frameNum + 1) % 60;
//            }


        }
    };

    private void createCaptureSession() {
        try {
            mCamera.createCaptureSession(Arrays.asList(mRecorder.getSurface(), mSurface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        captureRequestBuilder.addTarget(mRecorder.getSurface());
                        captureRequestBuilder.addTarget(mSurface);

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

            mRecorder.start();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void stopRecord() {
        Log.i(TAG, "Stopped recordingNum");
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

            createPreviewSession();

        }
    }

    public void closeCamera() {
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mCameraCaptureSession != null) {
            //mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    private void ImageProcessing(Mat rgb) {
        Mat gray = new Mat();
        if (rgb != null && !rgb.empty()) {
            Imgproc.cvtColor(rgb, gray, Imgproc.COLOR_RGB2GRAY);
            try {
                CascadeClassifier classifier = new CascadeClassifier(mCascade.getAbsolutePath());
                MatOfRect detectedFaces = new MatOfRect();
                classifier.detectMultiScale(gray, detectedFaces, 1.1, 10, 0, new Size(20, 20), new Size(gray.width(), gray.height()));


                for (Rect f : detectedFaces.toList()) {
                    Log.i("Face Detected: ", f.toString());
                       mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "FACE DETECTED", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}

