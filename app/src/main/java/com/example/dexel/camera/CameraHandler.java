package com.example.dexel.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class CameraHandler extends Activity{
    private final static String TAG = CameraHandler.class.getSimpleName();
    private CameraDevice c = null;
    private CameraManager m = null;
    private Context mContext = null;
    private CameraDevice cameraDevice = null;
    Surface surface = null;

    private final static int MY_PERMISSIONS_CAMERA_PERMISSIONS = 1;

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraState callback onOpened is called");
            cameraDevice = camera;
            createPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraState callback onOpened is called");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, "CameraState callback onOpened is called");
        }
    };

    public CameraHandler(Context context) {
        mContext = context;
    }

    public void openCamera() throws CameraAccessException {
        m = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        String[] cameras = m.getCameraIdList();

        List<String> a = new ArrayList<String>(Arrays.asList(cameras));

        if (ActivityCompat.checkSelfPermission((Activity) mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) mContext,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_CAMERA_PERMISSIONS);
            return;
        }

        m.openCamera(a.get(0), stateCallback, null);
        Log.d("Cameras", a.toString());
    }

    private void displayPreview(CameraCaptureSession session) throws CameraAccessException {
        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        captureRequestBuilder.addTarget(surface);
        try {
            session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreview(){

        SurfaceTexture surfaceTexture = ((TextureView) findViewById(R.id.textureView)).getSurfaceTexture();
        surface = new Surface(surfaceTexture);
        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    //display preview
                    try {
                        displayPreview(session);
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
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(mContext, "Permission required", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }

    }


}
