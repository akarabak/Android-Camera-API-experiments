package com.example.dexel.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        try {
            getCameraInstance();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public CameraDevice getCameraInstance() throws CameraAccessException {
        CameraDevice c = null;
        CameraManager m = (CameraManager) getSystemService(this.getApplicationContext().CAMERA_SERVICE);
        String[] cameras = m.getCameraIdList();
        List<String> a = new ArrayList<String>(Arrays.asList(cameras));
        Log.d("Cameras", a.toString());
        

        return c; // returns null if camera is unavailable
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
