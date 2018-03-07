package com.example.lasertest1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity {

    Mat imageMat;
    //WindowManager mWindowManager;
int f=0;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    imageMat=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Intent intent = new Intent(this, test.class);
        //startActivity(intent);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter("intentkey"));
       // mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

    }


    public void start(View v)
    {
        startService(new Intent(MainActivity.this, CamService.class));
        /*if(f==1)
            startActivity(new Intent(MainActivity.this,TouchActivity.class));*/
    }



    public void startTouchActivity(View v)
    {
        Intent myIntent = new Intent(MainActivity.this,
                TouchActivity.class);
        startActivity(myIntent);
    }

    public void bluetooth(View v)
    {
        startActivity(new Intent(MainActivity.this,BluetoothConnectionActivity.class));
    }

    BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("intentkey")) {
                String msg = intent.getStringExtra("key");
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                Log.e(msg, "coordinates in activity");
                //stopService(new Intent(MainActivity.this,CamService.class));
                f=1;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
            {
                Intent intent = new Intent(this, PreferenceSettingActivity.class);
                startActivity(intent);

                return true;
            }
            default:
            {
                return false;
            }
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

}
