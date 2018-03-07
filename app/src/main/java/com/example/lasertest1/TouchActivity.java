package com.example.lasertest1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.bgcamera.HiddenCameraService;

/**
 * Created by Ashwini on 05-02-2018.
 */

public class TouchActivity extends Activity {


    /**
     * Empty activity. Its only purpose is to launch the service.
     */


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
                startService(new Intent(this, MainService.class));

            finish();
        }
}