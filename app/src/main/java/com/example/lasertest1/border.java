package com.example.lasertest1;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * Created by Ashwini on 10-02-2018.
 */

public class border extends Service {

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private RelativeLayout thisview;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void initUI()
    {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        Toast.makeText(this,String.valueOf(height),Toast.LENGTH_LONG).show();
        Toast.makeText(this,String.valueOf(width),Toast.LENGTH_LONG).show();

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        mParams.width = width;
        mParams.height = height;
        mParams.gravity = Gravity.LEFT | Gravity.TOP | Gravity.BOTTOM | Gravity.RIGHT;

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        thisview = (RelativeLayout) inflater.inflate(R.layout.boundary, null);
        mWindowManager.addView(thisview, mParams);
    }

    public void desroyUI(){
        mWindowManager.removeView(thisview);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        initUI();
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        desroyUI();
        initUI();
    }


    public void onDestroy() {
        desroyUI();
        super.onDestroy();
        // The service is no longer used and is being destroyed
    }
}