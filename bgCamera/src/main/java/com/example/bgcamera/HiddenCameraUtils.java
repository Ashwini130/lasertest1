package com.example.bgcamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.example.bgcamera.config.CameraImageFormat;
import com.example.bgcamera.config.CameraRotation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class HiddenCameraUtils {

    @SuppressLint("NewApi")
    public static boolean canOverDrawOtherApps(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static void openDrawOverPermissionSetting(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @NonNull
    static File getCacheDir(Context context) {
        return context.getExternalCacheDir() == null ? context.getCacheDir() : context.getExternalCacheDir();
    }

    @WorkerThread
    static Bitmap rotateBitmap(@NonNull Bitmap bitmap, @CameraRotation.SupportedRotation int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    public static boolean saveImageFromFile(@NonNull Bitmap bitmap,
                                     @NonNull File fileToSave,
                                     @CameraImageFormat.SupportedImageFormat int imageFormat) {
        FileOutputStream out = null;
        boolean isSuccess = false;

        //Decide the image format
        Bitmap.CompressFormat compressFormat;
        switch (imageFormat) {
            case CameraImageFormat.FORMAT_JPEG:
                compressFormat = Bitmap.CompressFormat.JPEG;
                break;
            case CameraImageFormat.FORMAT_WEBP:
                compressFormat = Bitmap.CompressFormat.WEBP;
                break;
            case CameraImageFormat.FORMAT_PNG:
            default:
                compressFormat = Bitmap.CompressFormat.PNG;
        }

        try {
            if (!fileToSave.exists()) fileToSave.createNewFile();
            //out = new FileOutputStream(fileToSave);
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/saved_images");
            myDir.mkdirs();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
            String format = simpleDateFormat.format(new Date());
            String fname = "Image-"+format+".jpg";
            File file = new File (myDir, fname);
            if (file.exists ())
                file.delete ();
                out = new FileOutputStream(file);
            bitmap.compress(compressFormat, 100, out); // bmp is your Bitmap instance
            out.flush();
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isSuccess;
    }
}
