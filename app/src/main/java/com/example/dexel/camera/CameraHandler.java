package com.example.dexel.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class CameraHandler extends Activity{
    private final static String TAG = CameraHandler.class.getSimpleName();
    private CameraDevice c = null;
    private CameraManager m = null;
    private Context mContext = null;

    private final static int MY_PERMISSIONS_CAMERA_PERMISSIONS = 1;

    public CameraHandler(Context context) {
        mContext = context;
        try {
            getCameraInstance();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public CameraDevice getCameraInstance() throws CameraAccessException {
        m = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        String[] cameras = m.getCameraIdList();
        List<String> a = new ArrayList<String>(Arrays.asList(cameras));

        CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
//                CameraCaptureSession captureSessionCallback = new CameraCaptureSession.CaptureCallback(){
//
//                }
                Log.d(TAG, "CameraState callback onOpened is called");
                //Surface cameraSurface = new Surface((SurfaceTexture) findViewById(R.id.camera_view));

//                camera.createCaptureSession(new ArrayList<Surface>(),
//                        captureSessionCallback, );
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

        if (ActivityCompat.checkSelfPermission((Activity) mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) mContext,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_CAMERA_PERMISSIONS);

        }

        m.openCamera(a.get(0), stateCallback, null);

        Log.d("Cameras", a.toString());
        //m.

        return c; // returns null if camera is unavailable
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode){
            case MY_PERMISSIONS_CAMERA_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                return;
            }
        }

    }


}
